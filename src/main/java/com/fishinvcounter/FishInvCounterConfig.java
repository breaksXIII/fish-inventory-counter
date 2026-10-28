package com.fishinvcounter;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(FishInvCounterConfig.GROUP)
public interface FishInvCounterConfig extends Config
{
	String GROUP = "fishinvcounter";

	@ConfigSection(name = "Goals", description = "Goal and counting settings", position = 0)
	String goalsSection = "goals";

	@ConfigSection(name = "Overlay", description = "How the overlay looks", position = 1)
	String overlaySection = "overlay";

	@ConfigSection(name = "Colors", description = "Overlay colors", position = 3, closedByDefault = true)
	String colorsSection = "colors";

	enum GoalType
	{
		INVENTORY_COUNT,
		FISHING_LEVEL;

		@Override
		public String toString()
		{
			return this == INVENTORY_COUNT ? "Inventory count" : "Fishing level";
		}
	}

	// ---- Goals ----

	@ConfigItem(keyName = "goalType", name = "Goal type", position = 0, section = goalsSection,
		description = "Aim for a number of inventories, or a Fishing level")
	default GoalType goalType()
	{
		return GoalType.INVENTORY_COUNT;
	}

	@ConfigItem(keyName = "goal", name = "Inventory goal", position = 1, section = goalsSection,
		description = "How many full inventories of fish you want to reach")
	default int goal()
	{
		return 10;
	}

	@ConfigItem(keyName = "targetLevel", name = "Level goal", position = 2, section = goalsSection,
		description = "Fishing level to aim for when Goal type is Fishing level")
	default int targetLevel()
	{
		return 80;
	}

	@ConfigItem(keyName = "count", name = "Current count", position = 3, section = goalsSection,
		description = "Full fish inventories so far. Set to 0 to reset.")
	default int count()
	{
		return 0;
	}

	@ConfigItem(keyName = "dailyGoal", name = "Daily goal", position = 4, section = goalsSection,
		description = "Inventories per day for your streak. 0 = off.")
	default int dailyGoal()
	{
		return 0;
	}

	@ConfigItem(keyName = "fishList", name = "Only count these fish", position = 5, section = goalsSection,
		description = "Comma separated fish names, e.g. Raw lobster, Raw swordfish. Empty = all fish.")
	default String fishList()
	{
		return "";
	}

	@ConfigItem(keyName = "chatMessages", name = "Chat messages", position = 6, section = goalsSection,
		description = "Post the count in chat each time it goes up")
	default boolean chatMessages()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyOnGoal", name = "Notify on goal", position = 7, section = goalsSection,
		description = "Send a notification when you hit a goal")
	default boolean notifyOnGoal()
	{
		return true;
	}

	@ConfigItem(keyName = "soundOnGoal", name = "Sound on goal", position = 8, section = goalsSection,
		description = "Play a sound when you hit a goal")
	default boolean soundOnGoal()
	{
		return true;
	}

	// ---- Overlay ----

	@ConfigItem(keyName = "showOverlay", name = "Show overlay", position = 1, section = overlaySection,
		description = "Show the counter box on screen")
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "onlyWhileFishing", name = "Only while fishing", position = 2, section = overlaySection,
		description = "Hide the overlay if you haven't caught a fish in 5 minutes")
	default boolean onlyWhileFishing()
	{
		return false;
	}

	@ConfigItem(keyName = "compactMode", name = "Compact mode", position = 3, section = overlaySection,
		description = "Show one line with the count only")
	default boolean compactMode()
	{
		return false;
	}

	@ConfigItem(keyName = "hideTitle", name = "Hide title", position = 4, section = overlaySection,
		description = "Hide the 'Fish Inventories' title")
	default boolean hideTitle()
	{
		return false;
	}

	@ConfigItem(keyName = "showCount", name = "Show count", position = 5, section = overlaySection,
		description = "Show the inventory count line")
	default boolean showCount()
	{
		return true;
	}

	@ConfigItem(keyName = "showEta", name = "Show time left", position = 6, section = overlaySection,
		description = "Show time left to your goal")
	default boolean showEta()
	{
		return true;
	}

	@ConfigItem(keyName = "showInvPerHour", name = "Show inventories/hr", position = 7, section = overlaySection,
		description = "Show inventories per hour")
	default boolean showInvPerHour()
	{
		return false;
	}

	@ConfigItem(keyName = "showXpGained", name = "Show XP gained", position = 8, section = overlaySection,
		description = "Show Fishing XP gained this session")
	default boolean showXpGained()
	{
		return false;
	}

	@ConfigItem(keyName = "showXpPerInv", name = "Show XP/inventory", position = 9, section = overlaySection,
		description = "Show average XP per full inventory")
	default boolean showXpPerInv()
	{
		return false;
	}

	@ConfigItem(keyName = "showXpPerHour", name = "Show XP/hr", position = 10, section = overlaySection,
		description = "Show Fishing XP per hour")
	default boolean showXpPerHour()
	{
		return false;
	}

	// ---- Colors ----

	@ConfigItem(keyName = "titleColor", name = "Title", position = 1, section = colorsSection,
		description = "Title text color")
	default Color titleColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(keyName = "textColor", name = "Text", position = 2, section = colorsSection,
		description = "Count and info text color")
	default Color textColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(keyName = "barColor", name = "Bar", position = 3, section = colorsSection,
		description = "Progress bar color")
	default Color barColor()
	{
		return new Color(0, 110, 200);
	}

	@Alpha
	@ConfigItem(keyName = "barDoneColor", name = "Bar (goal reached)", position = 4, section = colorsSection,
		description = "Progress bar color once the goal is reached")
	default Color barDoneColor()
	{
		return new Color(0, 160, 0);
	}

	@Alpha
	@ConfigItem(keyName = "barBackgroundColor", name = "Bar background", position = 5, section = colorsSection,
		description = "Empty part of the progress bar")
	default Color barBackgroundColor()
	{
		return new Color(60, 60, 60, 200);
	}
}
