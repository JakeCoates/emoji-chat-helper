package com.emojichathelper;

import java.awt.image.*;
import java.util.*;
import javax.inject.*;
import net.runelite.client.game.*;
import net.runelite.client.util.*;

/** Emoji pictures, cut from one sheet, and their chat icons. Client thread. */
@Singleton
class EmojiIcons
{
	static final String SHEET = "emoji.png";
	/** Picture size in pixels, the same as the game's chat icons. */
	static final int SIZE = 13;
	/** Pictures per row of the sheet. */
	static final int COLUMNS = 32;

	@Inject
	private ChatIconManager chatIconManager;

	private BufferedImage sheet;
	private final Map<EmojiIndex.Emoji, BufferedImage> pictures = new HashMap<>();
	/** Kept across plugin restarts, as chat icons can't be unregistered. */
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

	/** The index for an {@code <img=>} tag showing the emoji, or -1 if icons aren't loaded yet. */
	int chatIcon(EmojiIndex.Emoji emoji)
	{
		int id = chatIcons.computeIfAbsent(emoji, e -> chatIconManager.registerChatIcon(picture(e)));
		return chatIconManager.chatIconIndex(id);
	}
}
