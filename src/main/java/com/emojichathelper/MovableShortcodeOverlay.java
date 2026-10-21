package com.emojichathelper;

import java.awt.*;
import javax.inject.*;
import net.runelite.client.ui.overlay.*;

/**
 * Draws the {@link ShortcodeList} where the player puts it: Alt and drag it, as any RuneLite
 * overlay, to anywhere or one of RuneLite's spots. It starts above the right of the chatbox. It
 * shows only while there are matches, so type a colon to see it to move it.
 */
class MovableShortcodeOverlay extends Overlay
{
	private final ShortcodeList list;

	@Inject
	MovableShortcodeOverlay(ShortcodeList list)
	{
		this.list = list;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(true);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (list.isEmpty())
		{
			return null;
		}
		list.paint(g, 0, 0);
		return list.size(g);
	}
}
