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
		description = "Lists emoji matching what you're typing: a name after a colon, like :smil, or an emoticon, like :) or Xd. Slack names such as :sweat_smile find emoji too. You type the rest yourself.",
		position = 0
	)
	default boolean suggest()
	{
		return true;
	}

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
		description = "Where the list shows. To move it in Anywhere mode, type a colon so it appears, then Alt+drag it.",
		position = 1
	)
	default ListPosition listPosition()
	{
		return ListPosition.ABOVE_CHATBOX;
	}

	@ConfigItem(
		keyName = "maxSuggestions",
		name = "Matches listed",
		description = "Most matches to list.",
		position = 2
	)
	@Range(min = 1, max = 10)
	default int maxSuggestions()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "showOtherName",
		name = "Show emoticon names",
		description = "Shows each emoticon's emoji name beside it.",
		position = 3
	)
	default boolean showOtherName()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInChat",
		name = "Emoji in chat",
		description = "Shows :names: in chat and overhead text as emoji, as RuneLite's Emojis plugin does.",
		position = 4
	)
	default boolean showInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "emoticonsOnly",
		name = "Only emoticons",
		description = "Only lists emoji that have an emoticon, like Xd or <3, and shows the emoticon to type.",
		position = 5
	)
	default boolean emoticonsOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font and size of the list's text.",
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
		description = "Scale of the 13 pixel emoji pictures.",
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
		description = "Space inside the list's border.",
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
		description = "Extra space between rows.",
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
		description = "Draws text with a drop shadow, like the game's.",
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
		description = "Background colour. Lower its alpha to make the list see-through.",
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
		description = "Border colour. Set its alpha to 0 to hide the border.",
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
		description = "The part of a match still to type.",
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
		description = "The part of a match already typed.",
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
		name = "Other matches",
		description = "Matches that don't start with what you've typed, such as typo matches.",
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
		description = "An emoticon shown beside a name, like Xd.",
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
		name = "Emoticon name",
		description = "The emoji name shown beside an emoticon.",
		section = styleSection,
		position = 111
	)
	default Color otherNameColour()
	{
		return new Color(170, 170, 170);
	}
}
