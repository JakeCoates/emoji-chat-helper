package com.emojichathelper;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ShortcodesTest
{
	private static final Map<String, Integer> ICONS = new HashMap<>();

	static
	{
		ICONS.put("joy", 1);
		ICONS.put("fire", 2);
		ICONS.put("+1", 3);
	}

	private static String replace(String message)
	{
		return Shortcodes.replace(message, name -> ICONS.getOrDefault(name, -1));
	}

	@Test
	public void theWordBeingTypedIsTheLastOne()
	{
		assertEquals(":swe", Shortcodes.lastWord("hello :swe"));
		assertEquals(":", Shortcodes.lastWord("hello :"));
		assertEquals("Xd", Shortcodes.lastWord("Xd"));
		assertEquals(":sweat_sm", Shortcodes.lastWord("hi :joy: :sweat_sm"));
	}

	@Test
	public void noWordIsBeingTypedAfterASpace()
	{
		assertNull(Shortcodes.lastWord(null));
		assertNull(Shortcodes.lastWord(""));
		assertNull(Shortcodes.lastWord("hi :swe "));
		// The game's non-breaking space counts as a space.
		assertNull(Shortcodes.lastWord("hi :swe" + (char) 0xa0));
		assertEquals(":swe", Shortcodes.lastWord("hi" + (char) 0xa0 + ":swe"));
	}

	@Test
	public void namesBecomeChatImages()
	{
		assertEquals("<img=1>", replace(":joy:"));
		assertEquals("lol <img=1> gz <img=3>", replace("lol :joy: gz :+1:"));
		assertEquals("<img=1><img=2>", replace(":joy::fire:"));
	}

	@Test
	public void aColonThatIsNotANameCanOpenTheNext()
	{
		assertEquals("at 12:30<img=1>", replace("at 12:30:joy:"));
		assertEquals(":nope<img=1>", replace(":nope:joy:"));
	}

	@Test
	public void aSpaceEndsAName()
	{
		assertNull(replace(":joy :"));
		assertNull(replace(":joy" + (char) 0xa0 + ":"));
		assertEquals("a: b <img=1>", replace("a: b :joy:"));
	}

	@Test
	public void aMessageWithoutNamesIsLeftAlone()
	{
		assertNull(replace("hello there"));
		assertNull(replace(":unknown: :("));
	}
}
