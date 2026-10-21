package com.emojichathelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts a developer client with the plugin loaded. In the test sources so it isn't shipped. */
public class EmojiChatHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EmojiChatHelperPlugin.class);
		RuneLite.main(args);
	}
}
