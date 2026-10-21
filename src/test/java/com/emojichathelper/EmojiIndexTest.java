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
import static org.junit.Assert.assertTrue;

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
		return index.search(word, limit, false);
	}

	private List<String> typed(String word, int limit)
	{
		return search(word, limit).stream().map(EmojiIndex.Match::typedForm).collect(Collectors.toList());
	}

	@Test
	public void sheetHasAPictureForEveryEmoji()
	{
		BufferedImage sheet = ImageUtil.loadImageResource(EmojiIcons.class, EmojiIcons.SHEET);
		int rows = (index.all().size() + EmojiIcons.COLUMNS - 1) / EmojiIcons.COLUMNS;
		assertEquals(1366, index.all().size());
		assertEquals(EmojiIcons.COLUMNS * EmojiIcons.SIZE, sheet.getWidth());
		assertEquals(rows * EmojiIcons.SIZE, sheet.getHeight());
	}

	@Test
	public void findsRuneLiteNamesInExactCase()
	{
		EmojiIndex.Emoji sweat = index.find("grinning_face_with_sweat");
		assertNotNull(sweat);
		assertEquals("1f605", sweat.codepoint);
		assertNull(index.find("Grinning_Face_With_Sweat"));
		assertNull(index.find("sweat_smile"));
	}

	@Test
	public void slackNamesFindTheEmojiButListRuneLitesName()
	{
		EmojiIndex.Match match = search(":sweat_smile", 1).get(0);
		assertEquals(":grinning_face_with_sweat:", match.typedForm());
		assertEquals(0, match.rank);
		// Typed as a Slack name, so the RuneLite name has to be typed from the start.
		assertNull(match.left(":sweat_smile"));
		assertEquals(":face_with_tears_of_joy:", typed(":joy", 1).get(0));
		assertTrue(typed(":thumbsu", 3).contains(":thumbs_up:"));
	}

	@Test
	public void slackNamesNeverShowAsEmojiInChat()
	{
		assertNull(Shortcodes.replace("lol :sweat_smile:", n -> index.find(n) == null ? -1 : 1));
		assertEquals("lol <img=1>", Shortcodes.replace("lol :grinning_face_with_sweat:", n -> index.find(n) == null ? -1 : 1));
	}

	@Test
	public void runeLitesNameWinsOverASlackNameForAnotherEmoji()
	{
		// Slack's :sunglasses: is 😎, but RuneLite's sunglasses is 🕶, which is what others see.
		assertEquals("1f576", search(":sunglasses", 1).get(0).emoji.codepoint);
	}

	@Test
	public void emoticonsAreLoaded()
	{
		assertEquals("Xd", index.find("grinning_squinting_face").emoticon);
		assertEquals("<3", index.find("red_heart").emoticon);
		assertEquals(":)", index.find("slightly_smiling_face").emoticon);
		assertNull(index.find("grinning_face_with_sweat").emoticon);
		assertEquals(63, index.all().stream().filter(e -> e.emoticon != null).count());
	}

	@Test
	public void exactNameRanksFirst()
	{
		assertEquals(":fire:", typed(":fire", 1).get(0));
		assertEquals(0, search(":fire", 1).get(0).rank);
	}

	@Test
	public void prefixesRankAboveLaterWords()
	{
		List<EmojiIndex.Match> matches = search(":grinning_face_w", 10);
		assertTrue(matches.stream().allMatch(m -> m.rank == 1));
		assertTrue(typed(":sweat", 10).contains(":grinning_face_with_sweat:"));
		assertEquals(2, EmojiIndex.rank("grinning_face_with_sweat", "sweat"));
	}

	@Test
	public void oneLetterAfterAColonSearches()
	{
		List<EmojiIndex.Match> matches = search(":s", 6);
		assertEquals(6, matches.size());
		// By name, or by a Slack name such as smiley.
		assertTrue(matches.stream().allMatch(m -> m.emoticon || m.emoji.name.startsWith("s")
			|| m.emoji.keywords.stream().anyMatch(k -> k.startsWith("s"))));
	}

	@Test
	public void loneColonListsColonEmoticons()
	{
		List<EmojiIndex.Match> matches = search(":", 20);
		assertFalse(matches.isEmpty());
		assertTrue(matches.stream().allMatch(m -> m.emoticon && m.emoji.emoticon.startsWith(":")));
		assertTrue(typed(":", 20).contains(":)"));
		assertTrue(typed(":", 20).contains(":3"));
	}

	@Test
	public void emoticonsMatchWhileTyped()
	{
		EmojiIndex.Match xd = search("Xd", 1).get(0);
		assertTrue(xd.emoticon);
		assertEquals(0, xd.rank);
		assertEquals("1f606", xd.emoji.codepoint);
		assertEquals("2764", search("<3", 1).get(0).emoji.codepoint);
		assertEquals(":)", typed(":)", 1).get(0));
		// A single letter would trigger on every word.
		assertTrue(search("x", 6).isEmpty());
		assertTrue(search("hello", 6).isEmpty());
	}

	@Test
	public void emoticonInWrongCaseIsNotComplete()
	{
		EmojiIndex.Match xd = search("xd", 1).get(0);
		assertEquals("Xd", xd.typedForm());
		assertEquals(1, xd.rank);
		assertNull(xd.left("xd"));
	}

	@Test
	public void finishedNamesAndCommandsListNothing()
	{
		assertTrue(search(":fire:", 6).isEmpty());
		assertTrue(search("::bank", 6).isEmpty());
		assertTrue(search("12:30", 6).isEmpty());
	}

	@Test
	public void typosStillMatch()
	{
		assertEquals(4, EmojiIndex.rank("fire", "frie"));
		assertEquals(4, EmojiIndex.rank("thumbs_up", "thmubs"));
		assertTrue(typed(":frie", 6).contains(":fire:"));
		assertEquals(-1, EmojiIndex.rank("fire", "fxyz"));
	}

	@Test
	public void oneEditCoversChangeInsertDeleteAndSwap()
	{
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smile"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smxle"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "simle"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smiel"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "smil"));
		assertTrue(EmojiIndex.oneEdit("smil", 0, 4, "smile"));
		assertTrue(EmojiIndex.oneEdit("smile", 0, 5, "mile"));
		assertTrue(EmojiIndex.oneEdit("cat_smile", 4, 5, "smiel"));
		assertFalse(EmojiIndex.oneEdit("smile", 0, 5, "simel"));
		assertFalse(EmojiIndex.oneEdit("smile", 0, 5, "sm"));
	}

	@Test
	public void lettersInOrderMatchWhenCloseTogether()
	{
		assertEquals(5, EmojiIndex.rank("thumbs_up", "thmbsu"));
		assertEquals(-1, EmojiIndex.rank("slightly_smiling_face", "simle"));
	}

	@Test
	public void leftIsTheRestOfTheTypedForm()
	{
		assertEquals("_with_sweat:", search(":grinning_face_w", 10).stream()
			.filter(m -> m.emoji.codepoint.equals("1f605")).findFirst().get().left(":grinning_face"));
		assertEquals(":", search(":fire", 1).get(0).left(":fire"));
		assertEquals("d", search("Xd", 1).get(0).left("X"));
		EmojiIndex.Match laterWord = search(":sweat", 10).stream()
			.filter(m -> m.emoji.name.equals("grinning_face_with_sweat")).findFirst().get();
		assertNull(laterWord.left(":sweat"));
	}

	@Test
	public void eachEmojiIsListedOnce()
	{
		List<EmojiIndex.Match> matches = search(":smil", 20);
		assertEquals(matches.size(), matches.stream().map(m -> m.emoji).distinct().count());
	}

	@Test
	public void emoticonsOnlyListsEmoticons()
	{
		List<EmojiIndex.Match> matches = index.search(":smil", 10, true);
		assertFalse(matches.isEmpty());
		assertTrue(matches.stream().allMatch(m -> m.emoticon && m.emoji.emoticon != null));
		assertTrue(matches.stream().anyMatch(m -> m.typedForm().equals(":)")));
	}

	@Test
	public void everyNameIsReplacedInChat()
	{
		for (EmojiIndex.Emoji emoji : index.all())
		{
			String shown = Shortcodes.replace("a :" + emoji.name + ": b", n -> index.find(n) == emoji ? 7 : -1);
			assertEquals(emoji.name, "a <img=7> b", shown);
		}
	}
}
