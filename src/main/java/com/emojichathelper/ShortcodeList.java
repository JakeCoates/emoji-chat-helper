package com.emojichathelper;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.inject.*;
import lombok.*;

/**
 * The list of matches for the word being typed. Rows are laid out when the matches or style
 * change and measured on the next draw, so drawing each frame does no extra work. Client thread.
 */
@Singleton
class ShortcodeList
{
	/** Space after the picture and between the parts of a row. */
	private static final int GAP = 5;

	/** A snapshot of the style settings, taken when they change. */
	static final class Style
	{
		final Font font;
		final int emoji;
		final int padding;
		final int rowSpacing;
		final boolean shadow;
		final boolean emoticonNames;
		final Color background;
		final Color border;
		final Color left;
		final Color typed;
		final Color name;
		final Color emoticon;
		final Color emoticonName;

		Style(EmojiChatHelperConfig config)
		{
			font = config.font().getFont();
			emoji = EmojiIcons.SIZE * config.emojiScale();
			padding = config.padding();
			rowSpacing = config.rowSpacing();
			shadow = config.textShadow();
			emoticonNames = config.showOtherName();
			background = config.backgroundColour();
			border = config.borderColour();
			left = config.leftColour();
			typed = config.typedColour();
			name = config.nameColour();
			emoticon = config.emoticonColour();
			emoticonName = config.otherNameColour();
		}
	}

	/** One piece of text in a row, and the space after it. */
	@RequiredArgsConstructor
	private static final class Part
	{
		final String text;
		final Color colour;
		final Color shadow;
		final int gap;
		int width;
	}

	@RequiredArgsConstructor
	private static final class Row
	{
		final EmojiIndex.Emoji emoji;
		final List<Part> parts;
	}

	private final EmojiIcons icons;

	/** The word the matches are for, such as ":swe"; null for none. */
	@Getter
	private String word;
	/** Whether the word is in the private message prompt. */
	@Getter
	private boolean prompt;
	private List<EmojiIndex.Match> matches = Collections.emptyList();
	private Style style;
	private List<Row> rows = Collections.emptyList();
	/** Null until measured. */
	private Dimension size;
	private int rowHeight;
	private int textY;

	@Inject
	ShortcodeList(EmojiIcons icons)
	{
		this.icons = icons;
	}

	void show(String word, boolean prompt, List<EmojiIndex.Match> matches)
	{
		this.word = word;
		this.prompt = prompt;
		this.matches = matches;
		layOut();
	}

	void restyle(Style style)
	{
		this.style = style;
		layOut();
	}

	boolean isEmpty()
	{
		return rows.isEmpty();
	}

	/**
	 * Builds each row: what to type, split into the typed and remaining parts, then the emoticon
	 * for a name or the name for an emoticon.
	 */
	private void layOut()
	{
		size = null;
		if (matches.isEmpty() || style == null)
		{
			rows = Collections.emptyList();
			return;
		}
		List<Row> laidOut = new ArrayList<>(matches.size());
		for (EmojiIndex.Match match : matches)
		{
			List<Part> parts = new ArrayList<>(3);
			String form = match.typedForm();
			String left = match.left(word);
			if (left == null)
			{
				parts.add(part(form, style.name, GAP));
			}
			else
			{
				parts.add(part(form.substring(0, form.length() - left.length()), style.typed, 0));
				parts.add(part(left, style.left, GAP));
			}
			if (!match.emoticon && match.emoji.emoticon != null)
			{
				parts.add(part(match.emoji.emoticon, style.emoticon, GAP));
			}
			else if (match.emoticon && style.emoticonNames)
			{
				parts.add(part(match.emoji.name, style.emoticonName, GAP));
			}
			laidOut.add(new Row(match.emoji, parts));
		}
		rows = laidOut;
	}

	private Part part(String text, Color colour, int gap)
	{
		return new Part(text, colour, style.shadow ? new Color(0, 0, 0, colour.getAlpha()) : null, gap);
	}

	/** The list's size, measured on the first call after a change. */
	Dimension size(Graphics2D g)
	{
		FontMetrics metrics = font(g);
		if (size == null)
		{
			int width = 0;
			for (Row row : rows)
			{
				int rowWidth = 0;
				for (Part part : row.parts)
				{
					part.width = metrics.stringWidth(part.text);
					rowWidth += part.width + part.gap;
				}
				width = Math.max(width, rowWidth - row.parts.get(row.parts.size() - 1).gap);
			}
			int height = Math.max(metrics.getAscent() + metrics.getDescent(), style.emoji);
			rowHeight = height + style.rowSpacing;
			textY = (height + metrics.getAscent() - metrics.getDescent()) / 2;
			size = new Dimension(style.padding + style.emoji + GAP + width + style.padding,
				style.padding + rows.size() * rowHeight - style.rowSpacing + style.padding);
		}
		return size;
	}

	/** Draws the list with its top left corner at x, y. */
	void paint(Graphics2D g, int x, int y)
	{
		Dimension size = size(g);
		g.setColor(style.background);
		g.fillRect(x, y, size.width, size.height);
		g.setColor(style.border);
		g.drawRect(x, y, size.width - 1, size.height - 1);

		// Keep pixel art sharp when scaled up.
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		int top = y + style.padding;
		int pictureY = (rowHeight - style.rowSpacing - style.emoji) / 2;
		for (Row row : rows)
		{
			g.drawImage(icons.picture(row.emoji), x + style.padding, top + pictureY, style.emoji, style.emoji, null);
			int textX = x + style.padding + style.emoji + GAP;
			for (Part part : row.parts)
			{
				if (part.shadow != null)
				{
					g.setColor(part.shadow);
					g.drawString(part.text, textX + 1, top + textY + 1);
				}
				g.setColor(part.colour);
				g.drawString(part.text, textX, top + textY);
				textX += part.width + part.gap;
			}
			top += rowHeight;
		}
	}

	/** Sets the font, antialiased unless it's one of the game's pixel fonts. */
	private FontMetrics font(Graphics2D g)
	{
		g.setFont(style.font);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, style.font.getFamily().startsWith("RuneScape")
			? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g.getFontMetrics();
	}
}
