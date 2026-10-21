package com.emojichathelper;

import java.awt.*;
import net.runelite.client.config.*;

@ConfigGroup(EmojiChatHelperConfig.GROUP)
public interface EmojiChatHelperConfig extends Config
{
	String GROUP = "emojichathelper";

	@ConfigSection(
		name = "Style",
		description = "How the list of matches looks",
		position = 100,
		closedByDefault = true
	)
	String styleSection = "style";

	@ConfigItem(
		keyName = "suggest",
		name = "List matches while typing",
		description = "Lists emoji that match what you type: a name after a colon, like :swe, or the start of an emoticon, like :) or Xd. A lone colon lists the emoticons that start with one. It only shows them: you type the rest yourself.",
		position = 0
	)
	default boolean suggest()
	{
		return true;
	}

	/** Where the list of matches is drawn. */
	enum ListPosition
	{
		ABOVE_CHATBOX("Above chatbox"),
		ABOVE_INPUT("Above chat input"),
		MOVABLE("Anywhere (Alt+drag)");

		private final String label;

		ListPosition(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "listPosition",
		name = "List position",
		description = "Where the list of matches shows: above the chatbox, so it covers none of the chat; just above the line you type in; or anywhere you drag it. To move it, type a colon so it shows, then hold Alt and drag it.",
		position = 1
	)
	default ListPosition listPosition()
	{
		return ListPosition.ABOVE_CHATBOX;
	}

	@ConfigItem(
		keyName = "maxSuggestions",
		name = "Matches listed",
		description = "How many matches the list shows at most.",
		position = 2
	)
	@Range(min = 1, max = 10)
	default int maxSuggestions()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "showOtherName",
		name = "Show other name",
		description = "Shows each match's other name beside it, in grey: the Slack name that found it, or RuneLite's name for a Slack name.",
		position = 3
	)
	default boolean showOtherName()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInChat",
		name = "Emoji in chat",
		description = "Shows :names: in chat and over players' heads as emoji.",
		position = 4
	)
	default boolean showInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "runeLiteNamesOnly",
		name = "Only names others see",
		description = "Shows emoji only for RuneLite's names, which players with RuneLite's Emojis plugin see as emoji too, and lists those names to type. A Slack name still finds its emoji in the list. Turned off, Slack names show as emoji as well, but only to players with this plugin.",
		position = 5
	)
	default boolean runeLiteNamesOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "emoticonsOnly",
		name = "Only emoticons",
		description = "Lists only emoji that RuneLite has a short emoticon for, like Xd or <3, with the emoticon to type. A name still finds them.",
		position = 6
	)
	default boolean emoticonsOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "The list's font and its size.",
		section = styleSection,
		position = 100
	)
	default FontType font()
	{
		return FontType.SMALL;
	}

	@ConfigItem(
		keyName = "emojiScale",
		name = "Emoji size",
		description = "How many times their own 13 pixels the emoji are drawn. Whole times keep their pixels sharp.",
		section = styleSection,
		position = 101
	)
	@Range(min = 1, max = 3)
	default int emojiScale()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "padding",
		name = "Padding",
		description = "Space between the list's edge and what is in it.",
		section = styleSection,
		position = 102
	)
	@Units(Units.PIXELS)
	@Range(max = 16)
	default int padding()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "rowSpacing",
		name = "Row spacing",
		description = "Extra space between matches.",
		section = styleSection,
		position = 103
	)
	@Units(Units.PIXELS)
	@Range(max = 16)
	default int rowSpacing()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "textShadow",
		name = "Text shadow",
		description = "Draws text with the game's black drop shadow.",
		section = styleSection,
		position = 104
	)
	default boolean textShadow()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "backgroundColour",
		name = "Background",
		description = "The list's background. Its alpha sets how see-through it is.",
		section = styleSection,
		position = 105
	)
	default Color backgroundColour()
	{
		return new Color(14, 12, 10, 235);
	}

	@Alpha
	@ConfigItem(
		keyName = "borderColour",
		name = "Border",
		description = "The line around the list. Make it fully see-through for none.",
		section = styleSection,
		position = 106
	)
	default Color borderColour()
	{
		return new Color(56, 48, 35);
	}

	@Alpha
	@ConfigItem(
		keyName = "leftColour",
		name = "Left to type",
		description = "What is left to type of a match, closing colon included.",
		section = styleSection,
		position = 107
	)
	default Color leftColour()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "typedColour",
		name = "Already typed",
		description = "What is already typed of a match.",
		section = styleSection,
		position = 108
	)
	default Color typedColour()
	{
		return new Color(120, 120, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "nameColour",
		name = "Name to retype",
		description = "A match to type from its start, as one found by a later word or with a typo is.",
		section = styleSection,
		position = 109
	)
	default Color nameColour()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "emoticonColour",
		name = "Emoticon",
		description = "RuneLite's emoticon beside a name, like Xd.",
		section = styleSection,
		position = 110
	)
	default Color emoticonColour()
	{
		return new Color(90, 220, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "otherNameColour",
		name = "Other name",
		description = "The match's other name, beside it.",
		section = styleSection,
		position = 111
	)
	default Color otherNameColour()
	{
		return new Color(170, 170, 170);
	}
}
