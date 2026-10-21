package com.emojichathelper;

import java.awt.image.*;
import java.util.*;
import javax.inject.*;
import net.runelite.client.game.*;
import net.runelite.client.util.*;

/** Each emoji's 13x13 picture, cut from one sheet, and its chat image. Client thread. */
@Singleton
class EmojiIcons
{
	static final String SHEET = "emoji.png";
	/** Pictures are this many pixels square, as the game's chat images are. */
	static final int SIZE = 13;
	/** Pictures in a row of the sheet. */
	static final int COLUMNS = 32;

	@Inject
	private ChatIconManager chatIconManager;

	private BufferedImage sheet;
	private final Map<EmojiIndex.Emoji, BufferedImage> pictures = new HashMap<>();
	/**
	 * Chat image ids, kept while the plugin is turned off and on again: the client has no way to
	 * give one back, so registering again would only use up more.
	 */
	private final Map<EmojiIndex.Emoji, Integer> chatIcons = new HashMap<>();

	BufferedImage picture(EmojiIndex.Emoji emoji)
	{
		return pictures.computeIfAbsent(emoji, e ->
		{
			if (sheet == null)
			{
				sheet = ImageUtil.loadImageResource(EmojiIcons.class, SHEET);
			}
			BufferedImage picture = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
			picture.getGraphics().drawImage(sheet.getSubimage(e.tile % COLUMNS * SIZE, e.tile / COLUMNS * SIZE, SIZE, SIZE), 0, 0, null);
			return picture;
		});
	}

	/** The index an {@code <img=>} tag takes to show the emoji; -1 while the client has none to give. */
	int chatIcon(EmojiIndex.Emoji emoji)
	{
		int id = chatIcons.computeIfAbsent(emoji, e -> chatIconManager.registerChatIcon(picture(e)));
		return chatIconManager.chatIconIndex(id);
	}
}
