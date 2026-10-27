package com.fishinvcounter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FishInvCounterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FishInvCounterPlugin.class);
		RuneLite.main(args);
	}
}
