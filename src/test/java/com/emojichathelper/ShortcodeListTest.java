package com.emojichathelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ShortcodeListTest
{
	private static final EmojiChatHelperConfig DEFAULTS = new EmojiChatHelperConfig()
	{
	};
	private static final int GAP = 5;

	private EmojiIndex index;
	private ShortcodeList list;
	private Graphics2D g;

	@Before
	public void setUp()
	{
		index = new EmojiIndex();
		index.load();
		list = new ShortcodeList(new EmojiIcons());
		list.restyle(new ShortcodeList.Style(DEFAULTS));
		g = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
	}

	private List<EmojiIndex.Match> show(String word)
	{
		List<EmojiIndex.Match> matches = index.search(word, 6, false);
		list.show(word, false, matches);
		return matches;
	}

	@Test
	public void nothingIsListedWithoutMatches()
	{
		assertTrue(list.isEmpty());
		list.show(":zzzzqq", false, Collections.emptyList());
		assertTrue(list.isEmpty());
	}

	@Test
	public void sizeFitsTheWidestRowAndAllRows()
	{
		// ":s" lists names; ":" lists emoticons, which show their name beside them.
		for (String word : new String[]{":s", ":"})
		{
			List<EmojiIndex.Match> matches = show(word);
			Dimension size = list.size(g);
			FontMetrics metrics = g.getFontMetrics();

			int widest = 0;
			for (EmojiIndex.Match match : matches)
			{
				int row = metrics.stringWidth(match.typedForm());
				if (!match.emoticon && match.emoji.emoticon != null)
				{
					row += GAP + metrics.stringWidth(match.emoji.emoticon);
				}
				else if (match.emoticon)
				{
					row += GAP + metrics.stringWidth(match.emoji.name);
				}
				widest = Math.max(widest, row);
			}
			int padding = DEFAULTS.padding();
			int emoji = EmojiIcons.SIZE;
			assertEquals(word, padding + emoji + GAP + widest + padding, size.width);

			int rowHeight = Math.max(metrics.getAscent() + metrics.getDescent(), emoji) + DEFAULTS.rowSpacing();
			assertEquals(word, padding + matches.size() * rowHeight - DEFAULTS.rowSpacing() + padding, size.height);
		}
	}

	@Test
	public void anEmoticonTypedInFullStillDraws()
	{
		// Nothing is left to type, so the remaining part is empty.
		show("Xd");
		list.paint(g, 0, 0);
		assertTrue(list.size(g).width > 0);
	}

	@Test
	public void itIsMeasuredOnceUntilSomethingChanges()
	{
		show(":swe");
		Dimension size = list.size(g);
		assertSame(size, list.size(g));

		list.restyle(new ShortcodeList.Style(new EmojiChatHelperConfig()
		{
			@Override
			public int emojiScale()
			{
				return 3;
			}
		}));
		Dimension bigger = list.size(g);
		assertNotSame(size, bigger);
		assertTrue(bigger.height > size.height);
	}

	@Test
	public void everyStyleDraws()
	{
		show(":");
		int withNames = list.size(g).width;
		list.restyle(new ShortcodeList.Style(new EmojiChatHelperConfig()
		{
			@Override
			public boolean showOtherName()
			{
				return false;
			}

			@Override
			public boolean textShadow()
			{
				return false;
			}

			@Override
			public int padding()
			{
				return 0;
			}
		}));
		list.paint(g, 10, 10);
		assertTrue(list.size(g).width < withNames);
	}
}
