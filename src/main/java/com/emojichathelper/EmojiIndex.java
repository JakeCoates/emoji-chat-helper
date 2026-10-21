package com.emojichathelper;

import com.google.common.io.CharStreams;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import javax.inject.*;
import lombok.*;

/**
 * Every emoji the plugin knows, by name: RuneLite's own names, which its Emojis plugin shows, and
 * Slack's, which Discord mostly shares. Built from RuneLite's and Slack's lists by
 * dev-tools/BuildEmojiData.java; see THIRD_PARTY_NOTICES.md.
 */
@Singleton
class EmojiIndex
{
	static final String FILE = "emoji.txt";
	/** RuneLite's emoticons, by dev-tools/DumpEmoticons.java. */
	static final String EMOTICONS = "emoticons.txt";

	@RequiredArgsConstructor
	static final class Emoji
	{
		/** Where its picture is in the sheet, {@link EmojiIcons}. */
		final int tile;
		final String codepoint;
		/** RuneLite's name for it. */
		final String name;
		/**
		 * RuneLite's short way to type it, as "Xd" or ":)", which its Emojis plugin shows as the emoji
		 * too; null for none. Case matters: "XD" is not one.
		 */
		final String emoticon;
		/** Slack's names for it that RuneLite does not use. */
		final List<String> aliases;
	}

	@RequiredArgsConstructor
	static final class Match
	{
		final Emoji emoji;
		/**
		 * What to type for it: a name without its colons (the one that matched, or RuneLite's when
		 * only those are listed), or its emoticon when {@link #emoticon}.
		 */
		final String name;
		/** The name or emoticon that matched. */
		final String matched;
		/**
		 * How well: 0 the whole of it, 1 its start, 2 the start of a later word, 3 anywhere in it, 4
		 * with one letter wrong, missing, extra or swapped, 5 its letters close together in order.
		 */
		final int rank;
		/** Whether {@link #name} is the emoji's emoticon, typed as it is, rather than a :name:. */
		final boolean emoticon;

		/** The same match, listed under RuneLite's name. */
		Match asRuneLiteName()
		{
			return new Match(emoji, emoji.name, matched, rank, false);
		}

		/** The same match, listed under its emoticon. */
		Match asEmoticon()
		{
			return new Match(emoji, emoji.emoticon, matched, rank, true);
		}

		/** As it is typed: ":sweat_smile:", or "Xd". */
		String typedForm()
		{
			return emoticon ? name : ":" + name + ":";
		}

		/**
		 * What is left to type after the word typed so far, closing colon included: "at_smile:" after
		 * ":swe" for sweat_smile. Null when it does not start with that word, case included, as RuneLite
		 * reads names and emoticons only in their own case: a match on a later word, a typo, or "xd" for
		 * "Xd" takes typing it again from its start.
		 */
		String left(String word)
		{
			String form = typedForm();
			return form.startsWith(word) ? form.substring(word.length()) : null;
		}

		/**
		 * The name to show beside it: the first that is not what to type of the name that found it,
		 * RuneLite's, and its first Slack name; null if none.
		 */
		String otherName()
		{
			for (String other : new String[]{matched, emoji.name, emoji.aliases.isEmpty() ? null : emoji.aliases.get(0)})
			{
				if (other != null && !other.equals(name))
				{
					return other;
				}
			}
			return null;
		}

		/** Whether a match of this rank on this many letters would be a better one. */
		boolean beatenBy(int rank, int length)
		{
			return rank < this.rank || rank == this.rank && length < matched.length();
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
			for (String alias : e.aliases)
			{
				byName.put(alias, e);
			}
		}
	}

	/** A bundled file's lines, without blank ones and # comments. */
	private static List<String> lines(String file)
	{
		try (InputStream in = EmojiIndex.class.getResourceAsStream(file))
		{
			if (in == null)
			{
				throw new IOException(file + " is missing");
			}
			List<String> lines = CharStreams.readLines(new InputStreamReader(in, StandardCharsets.UTF_8));
			lines.removeIf(line -> line.isEmpty() || line.startsWith("#"));
			return lines;
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	/** In Slack's picker order, which is also the sheet's. */
	List<Emoji> all()
	{
		return Collections.unmodifiableList(emoji);
	}

	/** The emoji a name means, whatever its case; null if none. */
	Emoji find(String name)
	{
		return byName.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * The emoji RuneLite gives a name, in the name's own case as RuneLite reads it; null if none, as
	 * for a Slack name or ":Joy:".
	 */
	Emoji findRuneLiteName(String name)
	{
		Emoji e = byName.get(name);
		return e != null && e.name.equals(name) ? e : null;
	}

	/**
	 * The emoji that best match the word being typed, each once, best first: by how well (see
	 * {@link Match#rank}), then Slack's picker order, which puts the common faces first. A word starting with a colon
	 * is a name being typed; any word may be the start of an emoticon, so a lone colon lists those.
	 *
	 * @param runeLiteNames list each under RuneLite's name, which other players see, even when a Slack name found it
	 * @param emoticonsOnly list only emoji with an emoticon, under that
	 */
	List<Match> search(String word, int limit, boolean runeLiteNames, boolean emoticonsOnly)
	{
		String query = nameQuery(word);
		List<Match> found = new ArrayList<>();
		for (Emoji e : emoji)
		{
			if (emoticonsOnly && e.emoticon == null)
			{
				continue;
			}
			Match best = null;
			if (query != null && !query.isEmpty())
			{
				best = match(e, e.name, query, null);
				for (String alias : e.aliases)
				{
					best = match(e, alias, query, best);
				}
				if (best != null && runeLiteNames)
				{
					best = best.asRuneLiteName();
				}
			}
			int rank = emoticonRank(e.emoticon, word);
			if (rank >= 0 && (best == null || best.beatenBy(rank, e.emoticon.length())))
			{
				best = new Match(e, e.emoticon, e.emoticon, rank, true);
			}
			if (best != null)
			{
				found.add(emoticonsOnly && !best.emoticon ? best.asEmoticon() : best);
			}
		}
		found.sort(Comparator.<Match>comparingInt(m -> m.rank)
			.thenComparingInt(m -> m.emoji.tile));
		return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
	}

	/**
	 * The name being typed in a word that starts with a colon, lower case: "swe" for ":swe", and ""
	 * for a lone colon. Null when the word is no name, as "::bank", ":-)" and ":joy:" are not.
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
	 * How well a word matches an emoticon: 0 all of it in its own case, which is how RuneLite reads
	 * it, 1 its start or all of it in another case, -1 not at all. One that starts with a letter or
	 * digit, as "Xd" does, needs two typed, or every word starting with an x would list it.
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

	private static Match match(Emoji e, String name, String query, Match best)
	{
		int rank = rank(name, query);
		if (rank < 0 || best != null && !best.beatenBy(rank, name.length()))
		{
			return best;
		}
		return new Match(e, name, name, rank, false);
	}

	/**
	 * See {@link Match#rank}; -1 for no match. Past the starts of words, a match needs three letters,
	 * and four for a typo or letters in order, or it lists noise: ":swe" would find
	 * slightly_frowning_face.
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

	/** Letters {@link #inOrder} may skip. */
	static final int IN_ORDER_SKIPS = 3;

	private static boolean wordStart(String name, int i)
	{
		return i == 0 || name.charAt(i - 1) == '_' || name.charAt(i - 1) == '-';
	}

	/**
	 * Whether the query's letters are all in the name in order and close together, the first
	 * starting it or a word in it, with at most {@value #IN_ORDER_SKIPS} letters skipped:
	 * "thmbsp" in thumbsup, but not "simle" spread across slightly_smiling_face.
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

	/** Whether the name, or a word in it, starts with the query but for one letter wrong, missing, extra or swapped: "simle" for smile. */
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
	 * Whether the {@code length} letters of {@code a} from {@code from} are {@code b} with at most one
	 * letter changed, added, removed or swapped with its neighbour. Allocates nothing: it runs for
	 * every name each time the word typed changes.
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
				// One changed.
				|| a.regionMatches(from + i + 1, b, i + 1, n - i - 1)
				// Two swapped.
				|| i + 1 < n && a.charAt(from + i) == b.charAt(i + 1) && a.charAt(from + i + 1) == b.charAt(i)
				&& a.regionMatches(from + i + 2, b, i + 2, n - i - 2);
		}
		// One more in a, or one more in b.
		return length > n ? a.regionMatches(from + i + 1, b, i, n - i) : a.regionMatches(from + i, b, i + 1, length - i);
	}
}
