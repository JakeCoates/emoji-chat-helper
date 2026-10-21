package com.emojichathelper;

import java.util.function.*;

/** Text parsing for :names:. */
final class Shortcodes
{
	private Shortcodes()
	{
	}

	/** The last word of the input, such as ":swe" in "hi :swe", or null if it ends in a space. */
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
	 * Replaces each known :name: with its chat icon, parsing as RuneLite's Emojis plugin does: a
	 * name runs from one colon to the next and can't contain a space. Returns null if nothing changed.
	 *
	 * @param icons the icon index for a name, or -1 if it isn't one
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
			// Not a name, but this colon may start the next one.
			open = i;
		}
		return out == null ? null : out.append(message, copied, message.length()).toString();
	}

	/** Whitespace, including the game's non-breaking space. */
	static boolean isSpace(char c)
	{
		return Character.isWhitespace(c) || c == '\u00a0';
	}
}
