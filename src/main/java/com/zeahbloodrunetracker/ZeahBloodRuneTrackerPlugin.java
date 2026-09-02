package com.zeahbloodrunetracker;

import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Zeah Blood Rune Tracker",
	description = "Tracks blood runes crafted with dark essence fragments on Zeah",
	tags = {"arceuus", "blood", "runecrafting", "skilling", "zeah"}
)
@Singleton
public class ZeahBloodRuneTrackerPlugin extends Plugin
{
	private static final Duration PRICE_REFRESH_INTERVAL = Duration.ofMinutes(5);
	private static final int CRAFT_EVENT_GRACE_TICKS = 3;
	private static final int ALTAR_CLICK_GRACE_TICKS = 8;

	private final ZeahBloodRuneSession session = new ZeahBloodRuneSession();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ZeahBloodRuneTrackerOverlay overlay;

	@Inject
	private ZeahBloodRuneTrackerConfig config;

	@Inject
	private ItemManager itemManager;

	private int previousBloodRunes;
	private int previousRunecraftingXp = -1;
	private int pendingBloodRuneIncrease;
	private int pendingBloodRuneTicks;
	private int pendingRunecraftingXpTicks;
	private int pendingAltarClickTicks;
	private boolean inventorySnapshotReady;
	private int bloodRunePrice;
	private Instant lastPriceRefresh;

	@Provides
	ZeahBloodRuneTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZeahBloodRuneTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		clientThread.invoke(() ->
		{
			snapshotInventory();
			refreshBloodRunePrice(now());
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		session.reset();
		inventorySnapshotReady = false;
		previousRunecraftingXp = -1;
		clearPendingCraft();
		bloodRunePrice = 0;
		lastPriceRefresh = null;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.RUNECRAFT)
		{
			return;
		}

		int runecraftingXp = event.getXp();
		if (previousRunecraftingXp >= 0 && runecraftingXp > previousRunecraftingXp)
		{
			if (pendingBloodRuneIncrease > 0 && pendingBloodRuneTicks > 0)
			{
				session.recordCraft(pendingBloodRuneIncrease, now());
				pendingBloodRuneIncrease = 0;
				pendingBloodRuneTicks = 0;
			}
			else
			{
				pendingRunecraftingXpTicks = CRAFT_EVENT_GRACE_TICKS;
			}
		}
		previousRunecraftingXp = runecraftingXp;
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption() == null ? "" : event.getMenuOption();
		String target = event.getMenuTarget() == null ? "" : event.getMenuTarget();

		String lowerOption = option.toLowerCase();
		String lowerTarget = target.toLowerCase();
		if (lowerOption.contains("craft-rune") || lowerOption.contains("craft rune")
			|| lowerTarget.contains("altar"))
		{
			pendingAltarClickTicks = ALTAR_CLICK_GRACE_TICKS;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Instant now = now();
		processInventoryChanges(now);
		advancePendingCraftWindow();

		if (bloodRunePrice <= 0 || lastPriceRefresh == null
			|| Duration.between(lastPriceRefresh, now).compareTo(PRICE_REFRESH_INTERVAL) >= 0)
		{
			refreshBloodRunePrice(now);
		}

		if (session.hasTimedOut(now, Duration.ofMinutes(config.statTimeout())))
		{
			reset();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING
			|| gameState == GameState.CONNECTION_LOST)
		{
			inventorySnapshotReady = false;
			previousRunecraftingXp = -1;
			clearPendingCraft();
		}
		else if (gameState == GameState.LOGGED_IN)
		{
			snapshotInventory();
		}
	}

	void reset()
	{
		session.reset();
		snapshotInventory();
	}

	ZeahBloodRuneSession getSession()
	{
		return session;
	}

	int getBloodRunePrice()
	{
		return bloodRunePrice;
	}

	Instant now()
	{
		return Instant.now();
	}

	static int getCraftedBloodRuneIncrease(int previousBloodRunes, int bloodRunes,
		boolean recentRunecraftingXp)
	{
		int bloodRuneIncrease = bloodRunes - previousBloodRunes;
		return bloodRuneIncrease > 0 && recentRunecraftingXp
			? bloodRuneIncrease : 0;
	}

	private void snapshotInventory()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			inventorySnapshotReady = false;
			return;
		}

		previousBloodRunes = inventory.count(ItemID.BLOODRUNE);
		previousRunecraftingXp = client.getSkillExperience(Skill.RUNECRAFT);
		inventorySnapshotReady = true;
		clearPendingCraft();
	}

	private void processInventoryChanges(Instant now)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			inventorySnapshotReady = false;
			clearPendingCraft();
			return;
		}

		// count() returns the true integer quantity. The white 100K-style text is
		// only how the game draws the stack and is never read by this plugin.
		int bloodRunes = inventory.count(ItemID.BLOODRUNE);

		if (inventorySnapshotReady && bloodRunes > previousBloodRunes)
		{
			int bloodRuneIncrease = bloodRunes - previousBloodRunes;
			if (pendingRunecraftingXpTicks > 0 || pendingAltarClickTicks > 0)
			{
				session.recordCraft(bloodRuneIncrease, now);
				pendingRunecraftingXpTicks = 0;
				pendingAltarClickTicks = 0;
			}
			else
			{
				// StatChanged and the final inventory state can be delivered on either
				// side of GameTick. Hold the increase briefly so the XP event can pair
				// with it regardless of delivery order.
				pendingBloodRuneIncrease = bloodRuneIncrease;
				pendingBloodRuneTicks = CRAFT_EVENT_GRACE_TICKS;
			}
		}

		previousBloodRunes = bloodRunes;
		inventorySnapshotReady = true;
	}

	private void advancePendingCraftWindow()
	{
		if (pendingBloodRuneTicks > 0 && --pendingBloodRuneTicks == 0)
		{
			pendingBloodRuneIncrease = 0;
		}

		if (pendingRunecraftingXpTicks > 0)
		{
			pendingRunecraftingXpTicks--;
		}

		if (pendingAltarClickTicks > 0)
		{
			pendingAltarClickTicks--;
		}
	}

	private void clearPendingCraft()
	{
		pendingBloodRuneIncrease = 0;
		pendingBloodRuneTicks = 0;
		pendingRunecraftingXpTicks = 0;
		pendingAltarClickTicks = 0;
	}

	private void refreshBloodRunePrice(Instant now)
	{
		bloodRunePrice = itemManager.getItemPrice(ItemID.BLOODRUNE);
		lastPriceRefresh = now;
	}

}
