package com.fishinvcounter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Fish Inventory Counter",
	description = "Counts how many inventories of fish you've filled, with a custom goal",
	tags = {"fishing", "fish", "counter", "goal"}
)
public class FishInvCounterPlugin extends Plugin
{
	private static final int INVENTORY_ID = 93;
	private static final int INVENTORY_SIZE = 28;
	private static final int GOAL_SOUND_ID = 3925; // GE offer "ding"
	private static final Duration FISHING_TIMEOUT = Duration.ofMinutes(5);

	private static final Set<String> FISH = ImmutableSet.of(
		"raw shrimps", "raw anchovies", "raw sardine", "raw herring", "raw mackerel",
		"raw trout", "raw cod", "raw pike", "raw salmon", "raw tuna", "raw rainbow fish",
		"raw lobster", "raw bass", "raw swordfish", "raw monkfish", "raw shark",
		"raw sea turtle", "raw manta ray", "raw anglerfish", "raw dark crab",
		"raw karambwan", "raw cave eel", "raw slimy eel", "raw lava eel",
		"leaping trout", "leaping salmon", "leaping sturgeon",
		"infernal eel", "sacred eel", "raw guppy", "raw cavefish", "raw tetra", "raw catfish"
	);

	@Inject private Client client;
	@Inject private FishInvCounterConfig config;
	@Inject private ConfigManager configManager;
	@Inject private ItemManager itemManager;
	@Inject private Notifier notifier;
	@Inject private OverlayManager overlayManager;
	@Inject private FishInvCounterOverlay overlay;

	// null = no baseline yet (just logged in / hopped), so we don't count the starting inventory
	private Integer lastFishCount;
	private boolean lastFull;

	private Instant lastCatch;
	private Instant sessionStart;
	private int sessionInventories;

	// Fishing XP: -1 = not known yet (StatChanged fires for every skill on login)
	private int currentXp = -1;
	private int sessionStartXp = -1;
	private int sessionXpAtLastCount;

	// Rates refreshed every few seconds so the overlay numbers don't flicker
	private volatile double invPerHourShown = -1;
	private volatile double xpPerHourShown = -1;

	@Provides
	FishInvCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FishInvCounterConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		lastFishCount = null;
		lastCatch = null;
		resetSession();
	}

	private void resetSession()
	{
		sessionStart = null;
		sessionInventories = 0;
		currentXp = -1;
		sessionStartXp = -1;
		sessionXpAtLastCount = 0;
		invPerHourShown = -1;
		xpPerHourShown = -1;
	}

	@Schedule(period = 3, unit = ChronoUnit.SECONDS)
	public void refreshRates()
	{
		invPerHourShown = inventoriesPerHour();
		xpPerHourShown = xpPerHour();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			lastFishCount = null;
		}
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			resetSession();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.FISHING)
		{
			return;
		}
		int previousXp = currentXp;
		currentXp = event.getXp();
		if (sessionStartXp < 0)
		{
			sessionStartXp = currentXp;
		}

		if (config.goalType() == FishInvCounterConfig.GoalType.FISHING_LEVEL && previousXp >= 0)
		{
			int level = Math.min(config.targetLevel(), Experience.MAX_REAL_LEVEL);
			int targetXp = Experience.getXpForLevel(Math.max(level, 2));
			if (previousXp < targetXp && currentXp >= targetXp)
			{
				goalReached("Fishing level goal reached! Level " + level + "!");
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != INVENTORY_ID)
		{
			return;
		}

		ItemContainer inv = event.getItemContainer();
		Set<String> fishNames = fishNames();
		int used = 0;
		int fish = 0;
		for (Item item : inv.getItems())
		{
			if (item.getId() == -1 || item.getQuantity() <= 0)
			{
				continue;
			}
			used++;
			if (isFish(item.getId(), fishNames))
			{
				fish++;
			}
		}
		boolean full = used >= INVENTORY_SIZE;

		if (lastFishCount != null && fish > lastFishCount)
		{
			lastCatch = Instant.now();
			if (sessionStart == null)
			{
				sessionStart = lastCatch;
			}
		}

		// Count when the inventory just became full because you caught a fish
		if (lastFishCount != null && !lastFull && full && fish > lastFishCount)
		{
			addOne();
		}

		lastFishCount = fish;
		lastFull = full;
	}

	private Set<String> fishNames()
	{
		String list = config.fishList();
		if (list == null || list.trim().isEmpty())
		{
			return FISH;
		}
		ImmutableSet.Builder<String> names = ImmutableSet.builder();
		for (String name : Text.fromCSV(list))
		{
			names.add(name.toLowerCase());
		}
		return names.build();
	}

	private boolean isFish(int itemId, Set<String> fishNames)
	{
		String name = itemManager.getItemComposition(itemId).getName();
		return name != null && fishNames.contains(name.toLowerCase());
	}

	private void addOne()
	{
		int count = config.count() + 1;
		int goal = config.goal();
		setInt("count", count);
		sessionInventories++;

		if (sessionStartXp >= 0)
		{
			sessionXpAtLastCount = currentXp - sessionStartXp;
			// Saved so the "inventories to level" estimate works right away next session
			setInt("xpPerInventory", sessionXpAtLastCount / sessionInventories);
		}

		String today = LocalDate.now().toString();
		int daily = dailyCount() + 1;
		setInt("dailyCount", daily);
		configManager.setConfiguration(FishInvCounterConfig.GROUP, "dailyDate", today);

		if (config.chatMessages())
		{
			boolean countGoal = config.goalType() == FishInvCounterConfig.GoalType.INVENTORY_COUNT;
			chat("Fish inventories: " + count + (countGoal ? " / " + goal : ""));
		}

		if (config.goalType() == FishInvCounterConfig.GoalType.INVENTORY_COUNT && count == goal)
		{
			goalReached("Fishing goal reached! " + goal + " inventories!");
		}

		int dailyGoal = config.dailyGoal();
		if (dailyGoal > 0 && daily == dailyGoal)
		{
			String yesterday = LocalDate.now().minusDays(1).toString();
			int streak = yesterday.equals(getString("streakDate")) ? getInt("streak") + 1 : 1;
			setInt("streak", streak);
			configManager.setConfiguration(FishInvCounterConfig.GROUP, "streakDate", today);
			goalReached("Daily fishing goal done! Streak: " + streak + (streak == 1 ? " day" : " days"));
		}
	}

	void undoLast()
	{
		int count = config.count();
		if (count <= 0)
		{
			return;
		}
		setInt("count", count - 1);
		if (dailyCount() > 0)
		{
			setInt("dailyCount", dailyCount() - 1);
		}
		if (sessionInventories > 0)
		{
			sessionInventories--;
		}
		chat("Undid last fish inventory. Now: " + (count - 1) + " / " + config.goal());
	}

	private void goalReached(String message)
	{
		chat("<col=00ff00>" + message + "</col>");
		if (config.notifyOnGoal())
		{
			notifier.notify(message);
		}
		if (config.soundOnGoal())
		{
			client.playSoundEffect(GOAL_SOUND_ID);
		}
	}

	private void chat(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	// ---- Values the overlay reads ----

	int dailyCount()
	{
		// Resets automatically when the saved date isn't today
		return LocalDate.now().toString().equals(getString("dailyDate")) ? getInt("dailyCount") : 0;
	}

	int streak()
	{
		String date = getString("streakDate");
		String today = LocalDate.now().toString();
		String yesterday = LocalDate.now().minusDays(1).toString();
		// Streak is only alive if the daily goal was hit today or yesterday
		return today.equals(date) || yesterday.equals(date) ? getInt("streak") : 0;
	}

	boolean isRecentlyFishing()
	{
		return lastCatch != null && Duration.between(lastCatch, Instant.now()).compareTo(FISHING_TIMEOUT) < 0;
	}

	/** XP when this level goal was first seen, so the bar starts empty instead of nearly full. */
	int levelGoalStartXp(int level)
	{
		String key = "levelStartXp" + level;
		int saved = getInt(key);
		if (saved == 0 && currentXp > 0)
		{
			setInt(key, currentXp);
			return currentXp;
		}
		return saved;
	}

	double shownInventoriesPerHour()
	{
		return invPerHourShown;
	}

	double shownXpPerHour()
	{
		return xpPerHourShown;
	}

	/** Inventories per hour this session, or -1 if not enough data yet. */
	private double inventoriesPerHour()
	{
		if (sessionStart == null || sessionInventories == 0)
		{
			return -1;
		}
		// shortcut: counts AFK/bank time since first catch, so the rate drops if you stop fishing; fine for a rough ETA
		double hours = Duration.between(sessionStart, Instant.now()).toMillis() / 3_600_000.0;
		return hours <= 0 ? -1 : sessionInventories / hours;
	}

	int currentXp()
	{
		return currentXp;
	}

	int sessionXp()
	{
		return sessionStartXp < 0 ? 0 : currentXp - sessionStartXp;
	}

	/** Average XP per full inventory: this session if available, otherwise the last saved value. 0 = unknown. */
	int xpPerInventory()
	{
		// shortcut: if you log in with a half-full inventory, the first one undercounts; evens out over a session
		if (sessionInventories > 0 && sessionXpAtLastCount > 0)
		{
			return sessionXpAtLastCount / sessionInventories;
		}
		return getInt("xpPerInventory");
	}

	/** Fishing XP per hour this session, or -1 if not enough data yet. */
	private double xpPerHour()
	{
		if (sessionStart == null || sessionXp() <= 0)
		{
			return -1;
		}
		double hours = Duration.between(sessionStart, Instant.now()).toMillis() / 3_600_000.0;
		return hours <= 0 ? -1 : sessionXp() / hours;
	}

	// ---- Hidden saved values ----

	private int getInt(String key)
	{
		Integer value = configManager.getConfiguration(FishInvCounterConfig.GROUP, key, Integer.class);
		return value == null ? 0 : value;
	}

	private String getString(String key)
	{
		return configManager.getConfiguration(FishInvCounterConfig.GROUP, key);
	}

	private void setInt(String key, int value)
	{
		configManager.setConfiguration(FishInvCounterConfig.GROUP, key, value);
	}
}
