package com.emojichathelper;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.inject.*;
import lombok.*;

/**
 * The emoji matching the word being typed, laid out once each time the word or the style changes,
 * and drawn by whichever overlay {@link EmojiChatHelperConfig#listPosition} picks. It only shows
 * them: nothing here types into the chatbox, which RuneLite does not allow a plugin to do. Client
 * thread.
 */
@Singleton
class ShortcodeList
{
	/** Between a picture and its name, and a name and the other name. */
	private static final int GAP = 5;

	/** How the list looks: the settings it is drawn with, read once each time they change. */
	static final class Style
	{
		final Font font;
		final int emoji;
		final int padding;
		final int rowSpacing;
		final boolean shadow;
		final boolean otherName;
		final Color background;
		final Color border;
		final Color left;
		final Color typed;
		final Color name;
		final Color emoticon;
		final Color other;

		Style(EmojiChatHelperConfig config)
		{
			font = config.font().getFont();
			emoji = EmojiIcons.SIZE * config.emojiScale();
			padding = config.padding();
			rowSpacing = config.rowSpacing();
			shadow = config.textShadow();
			otherName = config.showOtherName();
			background = config.backgroundColour();
			border = config.borderColour();
			left = config.leftColour();
			typed = config.typedColour();
			name = config.nameColour();
			emoticon = config.emoticonColour();
			other = config.otherNameColour();
		}
	}

	/** A piece of a row's text, in its colour, and the space after it; its width once measured. */
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

	/** The word typed so far, as ":swe"; null for none. */
	@Getter
	private String word;
	/** Whether it is typed in the private message prompt rather than the chat input. */
	@Getter
	private boolean prompt;
	private List<EmojiIndex.Match> matches = Collections.emptyList();
	private Style style;
	private List<Row> rows = Collections.emptyList();
	/** Null until measured, in the style's font. */
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

	/** Each match's row: what to type, split into what is typed and what is left, then its emoticon and other name. */
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
			List<Part> parts = new ArrayList<>(4);
			String form = match.typedForm();
			String left = match.left(word);
			if (left == null)
			{
				parts.add(part(form, style.name, GAP));
			}
			else
			{
				// No space between what is typed and what is left: they read as one.
				parts.add(part(form.substring(0, form.length() - left.length()), style.typed, 0));
				parts.add(part(left, style.left, GAP));
			}
			// An emoticon match shows its emoticon as what to type; a name match shows it here.
			if (!match.emoticon && match.emoji.emoticon != null)
			{
				parts.add(part(match.emoji.emoticon, style.emoticon, GAP));
			}
			String other = style.otherName ? match.otherName() : null;
			if (other != null)
			{
				parts.add(part(other, style.other, GAP));
			}
			laidOut.add(new Row(match.emoji, parts));
		}
		rows = laidOut;
	}

	private Part part(String text, Color colour, int gap)
	{
		return new Part(text, colour, style.shadow ? new Color(0, 0, 0, colour.getAlpha()) : null, gap);
	}

	/** How big the list is, measured the first time it is drawn after a change. */
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

	/** Draws the list with its top left at x, y. */
	void paint(Graphics2D g, int x, int y)
	{
		Dimension size = size(g);
		g.setColor(style.background);
		g.fillRect(x, y, size.width, size.height);
		g.setColor(style.border);
		g.drawRect(x, y, size.width - 1, size.height - 1);

		// Scaled up, the emoji keep their pixels sharp.
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

	/** Sets the style's font on {@code g}, smoothed unless it is one of the game's pixel fonts. */
	private FontMetrics font(Graphics2D g)
	{
		g.setFont(style.font);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, style.font.getFamily().startsWith("RuneScape")
			? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g.getFontMetrics();
	}
}
