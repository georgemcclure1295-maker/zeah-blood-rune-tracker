package com.zeahbloodrunetracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.api.Player;
import net.runelite.api.gameval.AnimationID;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class ZeahBloodRuneTrackerOverlay extends OverlayPanel
{
	private static final String OVERLAY_NAME = "Zeah blood runes";
	private static final Duration RATE_REFRESH_INTERVAL = Duration.ofSeconds(10);

	private final Client client;
	private final ZeahBloodRuneTrackerPlugin plugin;
	private final ZeahBloodRuneTrackerConfig config;
	private int displayedBloodRunesPerHour;
	private int bloodRunesAtLastRateRefresh = -1;
	private Instant lastRateRefresh;

	@Inject
	ZeahBloodRuneTrackerOverlay(Client client, ZeahBloodRuneTrackerPlugin plugin,
		ZeahBloodRuneTrackerConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, OVERLAY_NAME);
		addMenuEntry(RUNELITE_OVERLAY, "Reset", OVERLAY_NAME, event -> plugin.reset());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		ZeahBloodRuneSession session = plugin.getSession();
		if (!session.isActive())
		{
			resetRateDisplay();
			return null;
		}

		Player player = client.getLocalPlayer();
		boolean runecrafting = player != null
			&& (player.getAnimation() == AnimationID.RUNECRAFTING
				|| player.getAnimation() == AnimationID.HUMAN_RUNECRAFT);

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(runecrafting ? "Runecrafting" : "NOT runecrafting")
			.color(runecrafting ? Color.GREEN : Color.RED)
			.build());

		if (config.showBloodRunes())
		{
			addLine("Blood runes:", session.getBloodRunes());
		}

		Instant now = plugin.now();
		refreshRateDisplay(session, now);
		int bloodRunesPerHour = displayedBloodRunesPerHour;
		if (config.showBloodRunesPerHour() && session.hasRate())
		{
			addLine("Bloods/hr:", bloodRunesPerHour);
		}

		int bloodRunePrice = plugin.getBloodRunePrice();
		if (config.showRuneValue())
		{
			addLine("GE price:", bloodRunePrice);
		}

		if (config.showMoneyMade())
		{
			addLine("Money made:", (long) session.getBloodRunes() * bloodRunePrice);
		}

		if (config.showMoneyPerHour() && session.hasRate())
		{
			addLine("Money/hr:", (long) bloodRunesPerHour * bloodRunePrice);
		}

		return super.render(graphics);
	}

	private void refreshRateDisplay(ZeahBloodRuneSession session, Instant now)
	{
		boolean newCraft = session.getBloodRunes() != bloodRunesAtLastRateRefresh;
		boolean refreshDue = lastRateRefresh == null
			|| Duration.between(lastRateRefresh, now).compareTo(RATE_REFRESH_INTERVAL) >= 0;

		if (newCraft || refreshDue)
		{
			displayedBloodRunesPerHour = session.getBloodRunesPerHour(now);
			bloodRunesAtLastRateRefresh = session.getBloodRunes();
			lastRateRefresh = now;
		}
	}

	private void resetRateDisplay()
	{
		displayedBloodRunesPerHour = 0;
		bloodRunesAtLastRateRefresh = -1;
		lastRateRefresh = null;
	}

	private void addLine(String label, long value)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.right(NumberFormat.getIntegerInstance(Locale.US).format(value))
			.build());
	}

}
