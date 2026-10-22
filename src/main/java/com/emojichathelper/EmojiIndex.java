package com.emojichathelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.*;
import lombok.*;

/**
 * RuneLite's emoji: their names, which its Emojis plugin shows as emoji, their emoticons, and
 * Slack's names for them as search keywords. See THIRD_PARTY_NOTICES.md for the data's sources.
 */
@Singleton
class EmojiIndex
{
	static final String FILE = "emoji.txt";
	static final String EMOTICONS = "emoticons.txt";

	@RequiredArgsConstructor
	static final class Emoji
	{
		/** Position in the picture sheet. */
		final int tile;
		final String codepoint;
		final String name;
		/** Short form such as "Xd" or ":)", or null. RuneLite only matches it in this exact case. */
		final String emoticon;
		/**
		 * Slack's names, such as "sweat_smile". Only used to find the emoji: RuneLite doesn't know
		 * them, so they never show as emoji in chat and the list always gives {@link #name}.
		 */
		final List<String> keywords;
	}

	@RequiredArgsConstructor
	static final class Match
	{
		final Emoji emoji;
		/**
		 * 0 exact, 1 prefix, 2 prefix of a later word, 3 substring, 4 one typo, 5 letters in order.
		 */
		final int rank;
		/** Listed by its emoticon rather than its name. */
		final boolean emoticon;

		/** What to type: ":grinning_face:" or "Xd". */
		String typedForm()
		{
			return emoticon ? emoji.emoticon : ":" + emoji.name + ":";
		}

		/**
		 * The rest of the typed form after {@code word}, or null if it doesn't start with it. Case
		 * counts, as RuneLite only matches names and emoticons in their own case.
		 */
		String left(String word)
		{
			String form = typedForm();
			return form.startsWith(word) ? form.substring(word.length()) : null;
		}
	}

	private final List<Emoji> emoji = new ArrayList<>();
	private final Map<String, Emoji> byName = new HashMap<>();

	void load()
	{
		if (!emoji.isEmpty())
		{
			return;
		}
		Map<String, String> emoticons = new HashMap<>();
		for (String line : lines(EMOTICONS))
		{
			String[] parts = line.split(" ", 2);
			emoticons.put(parts[0], parts[1]);
		}
		for (String line : lines(FILE))
		{
			String[] parts = line.split(" ");
			Emoji e = new Emoji(emoji.size(), parts[0], parts[1], emoticons.get(parts[0]),
				Collections.unmodifiableList(Arrays.asList(parts).subList(2, parts.length)));
			emoji.add(e);
			byName.put(e.name, e);
		}
	}

	/** A resource's lines, skipping blank lines and # comments. */
	private static List<String> lines(String file)
	{
		try (InputStream in = EmojiIndex.class.getResourceAsStream(file))
		{
			if (in == null)
			{
				throw new IOException(file + " is missing");
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.collect(Collectors.toList());
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/** All emoji, in sheet order. */
	List<Emoji> all()
	{
		return Collections.unmodifiableList(emoji);
	}

	/** The emoji with this exact RuneLite name, or null. Keywords aren't names. */
	Emoji find(String name)
	{
		return byName.get(name);
	}

	/**
	 * The best matches for the word being typed, at most one per emoji, sorted by rank and then
	 * sheet order. A word starting with a colon is matched against names and keywords; any word is
	 * also matched against emoticons, so a lone colon lists the emoticons that start with one.
	 *
	 * @param emoticonsOnly only emoji that have an emoticon, listed by it
	 */
	List<Match> search(String word, int limit, boolean emoticonsOnly)
	{
		String query = nameQuery(word);
		List<Match> found = new ArrayList<>();
		for (Emoji e : emoji)
		{
			if (emoticonsOnly && e.emoticon == null)
			{
				continue;
			}
			int nameRank = query == null || query.isEmpty() ? -1 : nameRank(e, query);
			int emoticonRank = emoticonRank(e.emoticon, word);
			if (emoticonRank >= 0 && (nameRank < 0 || emoticonRank <= nameRank))
			{
				found.add(new Match(e, emoticonRank, true));
			}
			else if (nameRank >= 0)
			{
				found.add(new Match(e, nameRank, emoticonsOnly));
			}
		}
		found.sort(Comparator.<Match>comparingInt(m -> m.rank).thenComparingInt(m -> m.emoji.tile));
		return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
	}

	/** The best rank of the emoji's name and keywords, or -1. */
	private static int nameRank(Emoji e, String query)
	{
		int best = rank(e.name, query);
		for (String keyword : e.keywords)
		{
			int rank = rank(keyword, query);
			if (rank >= 0 && (best < 0 || rank < best))
			{
				best = rank;
			}
		}
		return best;
	}

	/**
	 * The lower-cased name after a word's leading colon: "swe" for ":swe", "" for ":". Null if the
	 * word has no leading colon or holds characters no name has, as in "::bank" or ":-)".
	 */
	static String nameQuery(String word)
	{
		if (!word.startsWith(":"))
		{
			return null;
		}
		String name = word.substring(1);
		for (int i = 0; i < name.length(); i++)
		{
			char c = name.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '+' && c != '-')
			{
				return null;
			}
		}
		return name.toLowerCase(Locale.ROOT);
	}

	/**
	 * 0 if the word is the emoticon, 1 if it starts it (ignoring case), else -1. Emoticons starting
	 * with a letter or digit need two characters typed, so ordinary words don't trigger them.
	 */
	static int emoticonRank(String emoticon, String word)
	{
		if (emoticon == null || word.isEmpty() || word.length() < 2 && Character.isLetterOrDigit(word.charAt(0)))
		{
			return -1;
		}
		if (emoticon.equals(word))
		{
			return 0;
		}
		return emoticon.toLowerCase(Locale.ROOT).startsWith(word.toLowerCase(Locale.ROOT)) ? 1 : -1;
	}

	/**
	 * How well a name matches the query (see {@link Match#rank}), or -1. Substring matches need 3
	 * characters and fuzzy matches 4, to keep short queries from matching everything.
	 */
	static int rank(String name, String query)
	{
		if (name.equals(query))
		{
			return 0;
		}
		if (name.startsWith(query))
		{
			return 1;
		}
		for (int i = 1; i < name.length(); i++)
		{
			if (wordStart(name, i) && name.startsWith(query, i))
			{
				return 2;
			}
		}
		if (query.length() < 3)
		{
			return -1;
		}
		if (name.contains(query))
		{
			return 3;
		}
		if (query.length() < 4)
		{
			return -1;
		}
		if (oneTypo(name, query))
		{
			return 4;
		}
		return inOrder(name, query) ? 5 : -1;
	}

	/** Most letters {@link #inOrder} may skip. */
	static final int IN_ORDER_SKIPS = 3;

	private static boolean wordStart(String name, int i)
	{
		return i == 0 || name.charAt(i - 1) == '_' || name.charAt(i - 1) == '-';
	}

	/**
	 * Whether the query's letters appear in order from the start of a word, skipping at most
	 * {@value #IN_ORDER_SKIPS} letters: "thmbsp" in thumbsup.
	 */
	static boolean inOrder(String name, String query)
	{
		for (int start = 0; start < name.length(); start++)
		{
			if (!wordStart(name, start) || name.charAt(start) != query.charAt(0))
			{
				continue;
			}
			int q = 1;
			int i = start + 1;
			for (; i < name.length() && q < query.length(); i++)
			{
				if (name.charAt(i) == query.charAt(q))
				{
					q++;
				}
			}
			if (q == query.length() && i - start - query.length() <= IN_ORDER_SKIPS)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether a word in the name starts with the query give or take one typo: "simle" for smile. */
	static boolean oneTypo(String name, String query)
	{
		for (int start = 0; start < name.length(); start++)
		{
			if (!wordStart(name, start))
			{
				continue;
			}
			for (int length = query.length() - 1; length <= query.length() + 1; length++)
			{
				if (start + length <= name.length() && oneEdit(name, start, length, query))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether {@code a.substring(from, from + length)} equals {@code b} after at most one change,
	 * insertion, deletion or swap of neighbouring letters. Doesn't allocate, as it runs for every
	 * name on each keystroke.
	 */
	static boolean oneEdit(String a, int from, int length, String b)
	{
		int n = b.length();
		if (Math.abs(length - n) > 1)
		{
			return false;
		}
		int i = 0;
		while (i < length && i < n && a.charAt(from + i) == b.charAt(i))
		{
			i++;
		}
		if (length == n)
		{
			return i == n
				|| a.regionMatches(from + i + 1, b, i + 1, n - i - 1)
				|| i + 1 < n && a.charAt(from + i) == b.charAt(i + 1) && a.charAt(from + i + 1) == b.charAt(i)
				&& a.regionMatches(from + i + 2, b, i + 2, n - i - 2);
		}
		return length > n ? a.regionMatches(from + i + 1, b, i, n - i) : a.regionMatches(from + i, b, i + 1, length - i);
	}
}
