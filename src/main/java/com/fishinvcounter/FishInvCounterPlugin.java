package com.fishinvcounter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Fish Inventory Counter",
	description = "Counts how many inventories of fish you've filled, with a custom goal",
	tags = {"fishing", "fish", "counter", "goal"}
)
public class FishInvCounterPlugin extends Plugin
{
	private static final int INVENTORY_ID = 93;
	private static final int INVENTORY_SIZE = 28;

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
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != INVENTORY_ID)
		{
			return;
		}

		ItemContainer inv = event.getItemContainer();
		int used = 0;
		int fish = 0;
		for (Item item : inv.getItems())
		{
			if (item.getId() == -1 || item.getQuantity() <= 0)
			{
				continue;
			}
			used++;
			if (isFish(item.getId()))
			{
				fish++;
			}
		}
		boolean full = used >= INVENTORY_SIZE;

		// Count when the inventory just became full because you caught a fish
		if (lastFishCount != null && !lastFull && full && fish > lastFishCount)
		{
			addOne();
		}

		lastFishCount = fish;
		lastFull = full;
	}

	private boolean isFish(int itemId)
	{
		String name = itemManager.getItemComposition(itemId).getName();
		return name != null && FISH.contains(name.toLowerCase());
	}

	private void addOne()
	{
		int count = config.count() + 1;
		int goal = config.goal();
		configManager.setConfiguration(FishInvCounterConfig.GROUP, "count", count);

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"Fish inventories: " + count + " / " + goal, null);

		if (count == goal)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"<col=00ff00>Fishing goal reached! " + goal + " inventories!</col>", null);
			if (config.notifyOnGoal())
			{
				notifier.notify("Fishing goal reached: " + goal + " inventories!");
			}
		}
	}
}
