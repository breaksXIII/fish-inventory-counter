package com.fishinvcounter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Experience;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FishInvCounterOverlay extends OverlayPanel
{
	private final FishInvCounterPlugin plugin;
	private final FishInvCounterConfig config;

	@Inject
	FishInvCounterOverlay(FishInvCounterPlugin plugin, FishInvCounterConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Undo last", "Fish Inventories", e -> plugin.undoLast());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || (config.onlyWhileFishing() && !plugin.isRecentlyFishing()))
		{
			return null;
		}

		boolean levelMode = config.goalType() == FishInvCounterConfig.GoalType.FISHING_LEVEL;
		return levelMode ? renderLevelGoal(graphics) : renderCountGoal(graphics);
	}

	private Dimension renderCountGoal(Graphics2D graphics)
	{
		int count = config.count();
		int goal = Math.max(1, config.goal());
		boolean done = count >= goal;

		if (config.compactMode())
		{
			panelComponent.getChildren().add(line("Fish:", count + " / " + goal));
			return super.render(graphics);
		}

		addTitle();
		if (config.showCount())
		{
			panelComponent.getChildren().add(line("Count:", count + " / " + goal));
		}

		double perHour = plugin.shownInventoriesPerHour();
		if (config.showEta() && !done && perHour > 0)
		{
			panelComponent.getChildren().add(line("Time left:", formatHours((goal - count) / perHour)));
		}

		addExtraLines();
		addBar(0, goal, Math.min(count, goal), done);
		return super.render(graphics);
	}

	private Dimension renderLevelGoal(Graphics2D graphics)
	{
		int level = Math.max(2, Math.min(config.targetLevel(), Experience.MAX_REAL_LEVEL));
		int xp = plugin.currentXp();
		String label = "Lvl " + level + ":";

		if (xp < 0)
		{
			// XP isn't known until the first XP update after logging in / enabling the plugin
			panelComponent.getChildren().add(line(label, "catch a fish"));
			return super.render(graphics);
		}

		int targetXp = Experience.getXpForLevel(level);
		int xpLeft = targetXp - xp;
		boolean done = xpLeft <= 0;
		int xpPerInv = plugin.xpPerInventory();
		int invsLeft = xpPerInv > 0 ? (int) Math.ceil(xpLeft / (double) xpPerInv) : -1;

		String remaining;
		if (done)
		{
			remaining = "Done!";
		}
		else if (invsLeft > 0)
		{
			remaining = invsLeft + (invsLeft == 1 ? " inv" : " invs");
		}
		else
		{
			remaining = String.format("%,d xp", xpLeft);
		}

		if (config.compactMode())
		{
			panelComponent.getChildren().add(line(label, remaining));
			return super.render(graphics);
		}

		addTitle();
		panelComponent.getChildren().add(line(label, remaining));
		if (config.showCount())
		{
			panelComponent.getChildren().add(line("Count:", String.valueOf(config.count())));
		}

		double perHour = plugin.shownInventoriesPerHour();
		if (config.showEta() && !done && invsLeft > 0 && perHour > 0)
		{
			panelComponent.getChildren().add(line("Time left:", formatHours(invsLeft / perHour)));
		}

		addExtraLines();
		int startXp = Math.min(plugin.levelGoalStartXp(level), targetXp - 1);
		addBar(startXp, targetXp, Math.min(xp, targetXp), done);
		return super.render(graphics);
	}

	private void addTitle()
	{
		if (!config.hideTitle())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Fish Inventories")
				.color(config.titleColor())
				.build());
		}
	}

	/** Optional lines shared by both goal types. */
	private void addExtraLines()
	{
		double perHour = plugin.shownInventoriesPerHour();
		if (config.showInvPerHour() && perHour > 0)
		{
			panelComponent.getChildren().add(line("Invs/hr:", String.format("%.1f", perHour)));
		}

		if (plugin.currentXp() >= 0)
		{
			if (config.showXpGained())
			{
				panelComponent.getChildren().add(line("XP gained:", String.format("%,d", plugin.sessionXp())));
			}
			if (config.showXpPerInv() && plugin.xpPerInventory() > 0)
			{
				panelComponent.getChildren().add(line("XP/inv:", String.format("%,d", plugin.xpPerInventory())));
			}
			double xpHr = plugin.shownXpPerHour();
			if (config.showXpPerHour() && xpHr > 0)
			{
				panelComponent.getChildren().add(line("XP/hr:", String.format("%,d", Math.round(xpHr))));
			}
		}

		int dailyGoal = config.dailyGoal();
		if (dailyGoal > 0)
		{
			panelComponent.getChildren().add(line("Today:", plugin.dailyCount() + " / " + dailyGoal));
			int streak = plugin.streak();
			panelComponent.getChildren().add(line("Streak:", streak + (streak == 1 ? " day" : " days")));
		}
	}

	private void addBar(int min, int max, int value, boolean done)
	{
		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMinimum(min);
		bar.setMaximum(max);
		bar.setValue(value);
		bar.setForegroundColor(done ? config.barDoneColor() : config.barColor());
		bar.setBackgroundColor(config.barBackgroundColor());
		panelComponent.getChildren().add(bar);
	}

	private LineComponent line(String left, String right)
	{
		return LineComponent.builder()
			.left(left)
			.leftColor(config.textColor())
			.right(right)
			.rightColor(config.textColor())
			.build();
	}

	static String formatHours(double hours)
	{
		long minutes = Math.round(hours * 60);
		return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
	}
}
