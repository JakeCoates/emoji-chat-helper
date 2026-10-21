package com.emojichathelper;

import java.awt.*;
import javax.inject.*;
import lombok.*;
import net.runelite.api.*;
import net.runelite.api.gameval.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

/** Draws the {@link ShortcodeList} just above the chatbox or the line being typed in, following it. */
class ShortcodeOverlay extends Overlay
{
	/**
	 * Above the chatbox, the first shown: the chat lines, so it covers none of the chat; then the
	 * chat input or the private message prompt, while the chat lines are closed.
	 */
	private static final int[] ABOVE_CHATBOX = {InterfaceID.Chatbox.CHATAREA, InterfaceID.Chatbox.INPUT, InterfaceID.Chatbox.MES_LAYER};
	private static final int[] ABOVE_PROMPT = {InterfaceID.Chatbox.MES_TEXT2, InterfaceID.Chatbox.MES_LAYER};
	private static final int[] ABOVE_INPUT = {InterfaceID.Chatbox.INPUT};

	private final Client client;
	private final ShortcodeList list;

	/** Above the line being typed in rather than above the chatbox. */
	@Setter
	private boolean aboveInput;

	@Inject
	ShortcodeOverlay(Client client, ShortcodeList list)
	{
		this.client = client;
		this.list = list;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (list.isEmpty())
		{
			return null;
		}
		Rectangle at = anchor(!aboveInput ? ABOVE_CHATBOX : list.isPrompt() ? ABOVE_PROMPT : ABOVE_INPUT);
		if (at == null)
		{
			return null;
		}
		Dimension size = list.size(g);
		// Kept on the game's screen, as a long name could take it past the right edge.
		int x = Math.max(0, Math.min(at.x, client.getCanvasWidth() - size.width));
		list.paint(g, x, Math.max(0, at.y - size.height - 2));
		return null;
	}

	/** The first of the widgets shown; null when none is. */
	private Rectangle anchor(int[] widgets)
	{
		for (int id : widgets)
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				return widget.getBounds();
			}
		}
		return null;
	}
}
