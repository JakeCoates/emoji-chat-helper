package com.emojichathelper;

import com.google.inject.*;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.*;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.*;
import net.runelite.client.config.*;
import net.runelite.client.eventbus.*;
import net.runelite.client.events.*;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.*;

/**
 * Lists matching emoji while a :name: or emoticon is typed, and shows :names: in chat as emoji.
 * It uses RuneLite's own emoji names, so what it shows matches RuneLite's Emojis plugin. It never
 * types into the chatbox or changes what is sent.
 */
@PluginDescriptor(
	name = "Emoji Chat Helper",
	description = "Search RuneLite's emoji and emoticons as you type a :name: in chat, the way Slack and Discord do",
	tags = {"emoji", "emojis", "chat", "shortcode", "search", "smiley", "emoticon", "helper"}
)
public class EmojiChatHelperPlugin extends Plugin
{
	/** Shown in the chat input by the Key Remapping plugin while typing is off. */
	private static final String PRESS_ENTER = "Press Enter to Chat";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EmojiChatHelperConfig config;

	@Inject
	private EmojiIndex index;

	@Inject
	private EmojiIcons icons;

	@Inject
	private ShortcodeList list;

	@Inject
	private ShortcodeOverlay overlay;

	@Inject
	private MovableShortcodeOverlay movableOverlay;

	// Copies of settings read on the client thread every tick and every message.
	private boolean suggest;
	private int maxSuggestions;
	private boolean emoticonsOnly;
	private boolean showInChat;

	@Provides
	EmojiChatHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EmojiChatHelperConfig.class);
	}

	@Override
	protected void startUp()
	{
		index.load();
		clientThread.invoke(this::applyConfig);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(movableOverlay);
		clientThread.invoke(() -> list.show(null, false, Collections.emptyList()));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Config changes arrive on the Swing thread; the list lives on the client thread.
		if (EmojiChatHelperConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::applyConfig);
		}
	}

	private void applyConfig()
	{
		suggest = config.suggest();
		maxSuggestions = config.maxSuggestions();
		emoticonsOnly = config.emoticonsOnly();
		showInChat = config.showInChat();
		list.restyle(new ShortcodeList.Style(config));

		EmojiChatHelperConfig.ListPosition position = config.listPosition();
		overlay.setAboveInput(position == EmojiChatHelperConfig.ListPosition.ABOVE_INPUT);
		overlayManager.remove(overlay);
		overlayManager.remove(movableOverlay);
		overlayManager.add(position == EmojiChatHelperConfig.ListPosition.MOVABLE ? movableOverlay : overlay);

		// Search again on the next tick with the new settings.
		list.show(null, false, Collections.emptyList());
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		String word = null;
		boolean prompt = false;
		if (suggest && client.getGameState() == GameState.LOGGED_IN)
		{
			int mode = client.getVarcIntValue(VarClientID.MESLAYERMODE);
			prompt = mode == InputType.PRIVATE_MESSAGE.getType();
			if (prompt)
			{
				word = Shortcodes.lastWord(client.getVarcStrValue(VarClientID.MESLAYERINPUT));
			}
			else if (mode == InputType.NONE.getType() && chatInputTakesText())
			{
				word = Shortcodes.lastWord(client.getVarcStrValue(VarClientID.CHATINPUT));
			}
		}
		if (!Objects.equals(word, list.getWord()) || prompt != list.isPrompt())
		{
			list.show(word, prompt, word == null ? Collections.emptyList() : index.search(word, maxSuggestions, emoticonsOnly));
		}
	}

	private boolean chatInputTakesText()
	{
		Widget input = client.getWidget(InterfaceID.Chatbox.INPUT);
		return input != null && !input.isHidden() && !String.valueOf(input.getText()).contains(PRESS_ENTER);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!showInChat)
		{
			return;
		}
		// The same message types RuneLite's Emojis plugin handles.
		switch (event.getType())
		{
			case PUBLICCHAT:
			case MODCHAT:
			case FRIENDSCHAT:
			case CLAN_CHAT:
			case CLAN_GUEST_CHAT:
			case CLAN_GIM_CHAT:
			case PRIVATECHAT:
			case PRIVATECHATOUT:
			case MODPRIVATECHAT:
				break;
			default:
				return;
		}
		MessageNode node = event.getMessageNode();
		String shown = Shortcodes.replace(node.getValue(), this::chatIcon);
		if (shown != null)
		{
			node.setValue(shown);
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!showInChat || !(event.getActor() instanceof Player))
		{
			return;
		}
		String shown = Shortcodes.replace(event.getOverheadText(), this::chatIcon);
		if (shown != null)
		{
			event.getActor().setOverheadText(shown);
		}
	}

	/** The chat icon index for an emoji name, or -1 if it isn't one. */
	private int chatIcon(String name)
	{
		EmojiIndex.Emoji emoji = index.find(name);
		return emoji == null ? -1 : icons.chatIcon(emoji);
	}
}
