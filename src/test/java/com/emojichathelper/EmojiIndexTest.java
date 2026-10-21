package com.emojichathelper;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.*;
import net.runelite.client.util.ImageUtil;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The bundled emoji, found by RuneLite's names and Slack's. */
public class EmojiIndexTest
{
	private EmojiIndex index;

	@Before
	public void setUp()
	{
		index = new EmojiIndex();
		index.load();
	}

	private List<EmojiIndex.Match> search(String word, int limit)
	{
		return index.search(word, limit, false, false);
	}

	/** What the list says to type for a word. */
	private List<String> typed(String word, int limit)
	{
		return search(word, limit).stream().map(EmojiIndex.Match::typedForm).collect(Collectors.toList());
	}

	@Test
	public void everyPictureIsInTheSheet()
	{
		BufferedImage sheet = ImageUtil.loadImageResource(EmojiIcons.class, EmojiIcons.SHEET);
		int rows = (index.all().size() + EmojiIcons.COLUMNS - 1) / EmojiIcons.COLUMNS;
		assertEquals(1366, index.all().size());
		assertEquals(EmojiIcons.COLUMNS * EmojiIcons.SIZE, sheet.getWidth());
		assertEquals(rows * EmojiIcons.SIZE, sheet.getHeight());
	}

	@Test
	public void slackAndRuneLiteNamesMeanTheSameEmoji()
	{
		EmojiIndex.Emoji sweatSmile = index.find("sweat_smile");
		assertNotNull(sweatSmile);
		assertEquals("1f605", sweatSmile.codepoint);
		assertSame(sweatSmile, index.find("grinning_face_with_sweat"));
		assertSame(sweatSmile, index.find("SWEAT_SMILE"));
		assertEquals("1f602", index.find("joy").codepoint);
		assertEquals("1f44d", index.find("+1").codepoint);
		assertSame(index.find("+1"), index.find("thumbsup"));
		assertNull(index.find("not_an_emoji"));
	}

	@Test
	public void runeLiteKeepsTheNamesItGivesAnotherEmoji()
	{
		// Slack's :sunglasses: is a face; RuneLite's is the glasses, and players see RuneLite's.
		assertEquals("1f576", index.find("sunglasses").codepoint);
	}

	@Test
	public void wholeNamesComeFirst()
	{
		assertEquals(":joy:", typed(":joy", 1).get(0));
		assertEquals(":+1:", typed(":+1", 1).get(0));
	}

	@Test
	public void startsOfNamesComeBeforeWordsInThem()
	{
		// Then Slack's picker order, which puts the common faces first.
		assertEquals(Arrays.asList(":sweat_smile:", ":sweat:"), typed(":swe", 6).subList(0, 2));
		assertEquals(":sweat_smile:", typed(":sweat_s", 1).get(0));
		assertTrue(typed(":crying", 10).contains(":loudly_crying_face:"));
	}

	@Test
	public void aNameIsFoundFromItsFirstLetter()
	{
		assertEquals(6, search(":s", 6).size());
		assertTrue(search(":s", 6).stream().allMatch(m -> m.typedForm().startsWith(":s") || m.emoticon));
	}

	@Test
	public void aLoneColonListsTheEmoticonsThatStartWithOne()
	{
		List<EmojiIndex.Match> matches = search(":", 20);
		assertFalse(matches.isEmpty());
		assertTrue(matches.stream().allMatch(m -> m.emoticon && m.name.startsWith(":")));
		assertTrue(typed(":", 20).contains(":)"));
		assertTrue(typed(":", 20).contains(":3"));
	}

	@Test
	public void emoticonsAreListedWhileTyped()
	{
		EmojiIndex.Match xd = search("Xd", 1).get(0);
		assertTrue(xd.emoticon);
		assertEquals("1f606", xd.emoji.codepoint);
		assertEquals("grinning_squinting_face", xd.otherName());
		assertEquals("2764", search("<3", 1).get(0).emoji.codepoint);
		assertEquals(":)", typed(":)", 1).get(0));
		// A letter alone lists nothing, or every word starting with an x would.
		assertTrue(search("x", 6).isEmpty());
		assertTrue(search("hello", 6).isEmpty());
	}

	@Test
	public void anEmoticonInAnotherCaseIsNotFinished()
	{
		EmojiIndex.Match xd = search("xd", 1).get(0);
		assertEquals("Xd", xd.name);
		assertEquals(1, xd.rank);
		assertNull(xd.left("xd"));
		assertEquals(0, search("Xd", 1).get(0).rank);
	}

	@Test
	public void nothingIsListedForFinishedNamesOrCommands()
	{
		assertTrue(search(":joy:", 6).isEmpty());
		assertTrue(search("::bank", 6).isEmpty());
		assertTrue(search("12:30", 6).isEmpty());
	}

	@Test
	public void oneTypoStillFindsAName()
	{
		assertEquals(4, EmojiIndex.rank("smile", "simle"));
		assertEquals(4, EmojiIndex.rank("smile", "smle"));
		assertEquals(4, EmojiIndex.rank("thumbsup", "thmubs"));
		assertTrue(typed(":simle", 2).contains(":smile:"));
		assertEquals(-1, EmojiIndex.rank("smile", "sxyle"));
	}

	@Test
	public void oneEditIsOneLetterChangedAddedRemovedOrSwapped()
	{
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smile"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smxle"));
		// Swapped, including the last two letters.
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "simle"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smiel"));
		// A letter more in the name, or in what is typed, including at the end.
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smil"));
		assertTrue(EmojiIndex.oneEdit("smil", 0, 4, "smile"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "mile"));
		// Within a longer name, from a word's start.
		assertTrue(EmojiIndex.oneEdit("cat_smile", 4, 5, "smiel"));
		assertFalse(EmojiIndex.oneEdit("smile", 0, 5, "simel"));
		assertFalse(EmojiIndex.oneEdit("smile", 0, 5, "sm"));
	}

	@Test
	public void lettersCloseTogetherInOrderFindAName()
	{
		assertEquals(5, EmojiIndex.rank("thumbsup", "thmbsp"));
		// Spread across a long name is no match.
		assertEquals(-1, EmojiIndex.rank("slightly_smiling_face", "simle"));
	}

	@Test
	public void eachMatchSaysWhatIsLeftToType()
	{
		assertEquals("at_smile:", search(":sweat_s", 1).get(0).left(":swe"));
		assertEquals(":", search(":joy", 1).get(0).left(":joy"));
		assertEquals("d", search("Xd", 1).get(0).left("X"));
		// RuneLite reads them only in their own case, so another case is typed again from the start.
		assertNull(search(":Swe", 1).get(0).left(":Swe"));
		EmojiIndex.Match laterWord = search(":crying", 10).stream()
			.filter(m -> m.name.equals("loudly_crying_face")).findFirst().get();
		assertNull(laterWord.left(":crying"));
	}

	@Test
	public void eachEmojiIsListedOnce()
	{
		List<EmojiIndex.Match> matches = search(":thumbs", 10);
		assertEquals(matches.size(), matches.stream().map(m -> m.emoji).distinct().count());
	}

	@Test
	public void theOtherNameIsRuneLitesForASlackName()
	{
		assertEquals("grinning_face_with_sweat", search(":sweat_smile", 1).get(0).otherName());
		assertEquals("sweat_smile", search(":grinning_face_with_sweat", 1).get(0).otherName());
	}

	@Test
	public void onlyRuneLiteNamesAreTheOnesOthersSee()
	{
		assertNotNull(index.findRuneLiteName("grinning_face_with_sweat"));
		// RuneLite reads names only in their own case.
		assertNull(index.findRuneLiteName("Grinning_Face_With_Sweat"));
		assertNull(index.findRuneLiteName("sweat_smile"));
	}

	@Test
	public void aSlackNameFindsTheRuneLiteNameToType()
	{
		EmojiIndex.Match match = index.search(":sweat_s", 1, true, false).get(0);
		assertEquals("grinning_face_with_sweat", match.name);
		assertEquals("sweat_smile", match.otherName());
		// Typed as a Slack name, the RuneLite name is typed from its start.
		assertNull(match.left(":sweat_s"));
		assertEquals("face_with_sweat:", index.search(":grinning_f", 10, true, false).stream()
			.filter(m -> m.emoji.codepoint.equals("1f605")).findFirst().get().left(":grinning_"));
	}

	@Test
	public void onlyEmoticonsListsThoseUnderTheirEmoticon()
	{
		List<EmojiIndex.Match> matches = index.search(":smil", 10, true, true);
		assertFalse(matches.isEmpty());
		assertTrue(matches.stream().allMatch(m -> m.emoticon && m.emoji.emoticon != null));
		assertTrue(matches.stream().anyMatch(m -> m.name.equals(":)")));
	}

	@Test
	public void runeLitesEmoticonsComeWithTheirEmoji()
	{
		assertEquals("Xd", index.find("grinning_squinting_face").emoticon);
		assertEquals("<3", index.find("heart").emoticon);
		assertEquals(":)", index.find("slightly_smiling_face").emoticon);
		assertNull(index.find("grinning_face_with_sweat").emoticon);
		assertEquals(63, index.all().stream().filter(e -> e.emoticon != null).count());
	}

	@Test
	public void everyNameShowsInChat()
	{
		int names = 0;
		for (EmojiIndex.Emoji emoji : index.all())
		{
			List<String> all = new ArrayList<>(emoji.aliases);
			all.add(emoji.name);
			for (String name : all)
			{
				String shown = Shortcodes.replace("a :" + name + ": b", n -> index.find(n) == emoji ? 7 : -1);
				assertEquals(name, "a <img=7> b", shown);
				names++;
			}
		}
		assertEquals(1366 + 578, names);
	}
}
