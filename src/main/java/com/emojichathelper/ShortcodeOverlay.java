package com.emojichathelper;

import java.awt.*;
import javax.inject.*;
import lombok.*;
import net.runelite.api.*;
import net.runelite.api.gameval.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

/** Draws the {@link ShortcodeList} above the chatbox or the chat input. */
class ShortcodeOverlay extends Overlay
{
	/** Anchors in order of preference; the later ones are for when the chat is collapsed. */
	private static final int[] ABOVE_CHATBOX = {InterfaceID.Chatbox.CHATAREA, InterfaceID.Chatbox.INPUT, InterfaceID.Chatbox.MES_LAYER};
	private static final int[] ABOVE_PROMPT = {InterfaceID.Chatbox.MES_TEXT2, InterfaceID.Chatbox.MES_LAYER};
	private static final int[] ABOVE_INPUT = {InterfaceID.Chatbox.INPUT};

	private final Client client;
	private final ShortcodeList list;

	/** Anchor to the input line rather than the whole chatbox. */
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
		// Keep it on screen.
		int x = Math.max(0, Math.min(at.x, client.getCanvasWidth() - size.width));
		list.paint(g, x, Math.max(0, at.y - size.height - 2));
		return null;
	}

	/** The bounds of the first visible widget, or null. */
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
