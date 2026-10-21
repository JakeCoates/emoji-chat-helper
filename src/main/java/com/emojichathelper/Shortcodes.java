package com.emojichathelper;

import java.util.function.*;

/** Finding :names: in text: the one being typed, and the finished ones in a chat message. */
final class Shortcodes
{
	private Shortcodes()
	{
	}

	/**
	 * The word being typed at the end of the chat input, as typed: ":swe" in "hi :swe". Null when
	 * the input is empty or ends in a space. {@link EmojiIndex#search} decides whether it is a name
	 * or an emoticon being typed.
	 */
	static String lastWord(String typed)
	{
		if (typed == null || typed.isEmpty() || isSpace(typed.charAt(typed.length() - 1)))
		{
			return null;
		}
		int start = typed.length();
		while (start > 0 && !isSpace(typed.charAt(start - 1)))
		{
			start--;
		}
		return typed.substring(start);
	}

	/**
	 * The message with each :name: that {@code icons} knows replaced by its chat image; null if
	 * none was. Read as RuneLite's Emojis plugin reads it, so a message shows the same emoji to
	 * both: a name runs from one colon to the next, and a space ends it.
	 *
	 * @param icons the chat image index for a name, or -1 for one that is not an emoji
	 */
	static String replace(String message, ToIntFunction<String> icons)
	{
		StringBuilder out = null;
		int copied = 0;
		int open = -1;
		for (int i = 0; i < message.length(); i++)
		{
			char c = message.charAt(i);
			if (isSpace(c))
			{
				open = -1;
				continue;
			}
			if (c != ':')
			{
				continue;
			}
			if (open >= 0 && i > open + 1)
			{
				int icon = icons.applyAsInt(message.substring(open + 1, i));
				if (icon >= 0)
				{
					if (out == null)
					{
						out = new StringBuilder(message.length());
					}
					out.append(message, copied, open).append("<img=").append(icon).append('>');
					copied = i + 1;
					open = -1;
					continue;
				}
			}
			// Not a name: this colon may open the next one.
			open = i;
		}
		return out == null ? null : out.append(message, copied, message.length()).toString();
	}

	/** Whether a character ends a word: whitespace, or the game's non-breaking space. */
	static boolean isSpace(char c)
	{
		return Character.isWhitespace(c) || c == '\u00a0';
	}
}
