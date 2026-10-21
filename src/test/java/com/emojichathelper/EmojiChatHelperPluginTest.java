package com.emojichathelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a developer-mode client with this plugin loaded, for testing in game.
 *
 * <p>Not a test despite the name and the location: {@code dev-tools/run-client.ps1} runs its
 * {@code main}. It lives in the test source set so it is compiled but never shipped in the plugin jar.
 */
public class EmojiChatHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EmojiChatHelperPlugin.class);
		RuneLite.main(args);
	}
}
