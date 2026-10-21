package com.emojichathelper;

import java.awt.*;
import javax.inject.*;
import net.runelite.client.ui.overlay.*;

/**
 * Draws the {@link ShortcodeList} as a normal overlay the player can Alt+drag. It is only visible
 * while there are matches.
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
