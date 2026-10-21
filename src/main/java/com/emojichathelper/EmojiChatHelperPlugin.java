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
 * Slack and Discord style :names: for emoji. Typing one lists the emoji whose names match, and
 * one in chat or over a player's head shows as the emoji.
 *
 * <p>It takes RuneLite's names as well as Slack's, and reads a message as RuneLite's Emojis plugin
 * does, so the two show players the same emoji. Nothing here types into the chatbox or changes
 * what is sent: the list only shows names, and only this player sees the emoji it draws.
 */
@PluginDescriptor(
	name = "Emoji Chat Helper",
	description = "Type emoji in chat the Slack and Discord way: a list of matching emoji, names and emoticons while you type a :name:",
	tags = {"emoji", "emojis", "chat", "shortcode", "slack", "discord", "smiley", "emoticon", "helper"}
)
public class EmojiChatHelperPlugin extends Plugin
{
	/** What the Key Remapping plugin shows in the chat input while typing moves the camera. */
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

	// The settings read on the client thread's hot paths, copied by applyConfig. Client thread.
	private boolean suggest;
	private int maxSuggestions;
	private boolean runeLiteNamesOnly;
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
		// Settings change on the Swing thread; the list is drawn and listed on the client thread.
		if (EmojiChatHelperConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::applyConfig);
		}
	}

	/** Restyles the list, puts it where the settings say, and lists again with what they now say. Client thread. */
	private void applyConfig()
	{
		suggest = config.suggest();
		maxSuggestions = config.maxSuggestions();
		runeLiteNamesOnly = config.runeLiteNamesOnly();
		emoticonsOnly = config.emoticonsOnly();
		showInChat = config.showInChat();
		list.restyle(new ShortcodeList.Style(config));
		EmojiChatHelperConfig.ListPosition position = config.listPosition();
		overlay.setAboveInput(position == EmojiChatHelperConfig.ListPosition.ABOVE_INPUT);
		overlayManager.remove(overlay);
		overlayManager.remove(movableOverlay);
		overlayManager.add(position == EmojiChatHelperConfig.ListPosition.MOVABLE ? movableOverlay : overlay);
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
			list.show(word, prompt, word == null ? Collections.emptyList()
				: index.search(word, maxSuggestions, runeLiteNamesOnly, emoticonsOnly));
		}
	}

	/**
	 * Whether the chat input is shown and taking text. Other prompts, such as a bank search, are
	 * left out by the caller.
	 */
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
		// The kinds RuneLite's Emojis plugin reads: what players say.
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

	/** The chat image index for an emoji's name; -1 if it is not one, or is a Slack name that only this plugin would show. */
	private int chatIcon(String name)
	{
		EmojiIndex.Emoji emoji = runeLiteNamesOnly ? index.findRuneLiteName(name) : index.find(name);
		return emoji == null ? -1 : icons.chatIcon(emoji);
	}
}
