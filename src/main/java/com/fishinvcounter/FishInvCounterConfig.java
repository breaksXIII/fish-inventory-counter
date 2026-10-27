package com.fishinvcounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FishInvCounterConfig.GROUP)
public interface FishInvCounterConfig extends Config
{
	String GROUP = "fishinvcounter";

	@ConfigItem(
		keyName = "goal",
		name = "Goal",
		description = "How many full inventories of fish you want to reach",
		position = 1
	)
	default int goal()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "count",
		name = "Current count",
		description = "Full fish inventories so far. Set to 0 to reset.",
		position = 2
	)
	default int count()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show the counter box on screen",
		position = 3
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyOnGoal",
		name = "Notify on goal",
		description = "Send a notification when you hit your goal",
		position = 4
	)
	default boolean notifyOnGoal()
	{
		return true;
	}
}
