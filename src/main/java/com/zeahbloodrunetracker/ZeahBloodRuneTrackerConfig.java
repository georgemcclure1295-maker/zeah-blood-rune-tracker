package com.zeahbloodrunetracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Units;

@ConfigGroup("zeahbloodrunetracker")
public interface ZeahBloodRuneTrackerConfig extends Config
{
	@ConfigSection(
		name = "Display",
		description = "Choose which statistics are shown on the overlay",
		position = 0
	)
	String displaySection = "display";

	@ConfigItem(
		position = 0,
		keyName = "showBloodRunes",
		name = "Blood runes",
		description = "Show the number of blood runes crafted",
		section = displaySection
	)
	default boolean showBloodRunes()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "showBloodRunesPerHour",
		name = "Bloods/hr",
		description = "Show the estimated blood runes crafted per hour",
		section = displaySection
	)
	default boolean showBloodRunesPerHour()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "showRuneValue",
		name = "Rune value",
		description = "Show the current GE price of one blood rune",
		section = displaySection
	)
	default boolean showRuneValue()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "showMoneyMade",
		name = "Money made",
		description = "Show the GE value of the blood runes crafted",
		section = displaySection
	)
	default boolean showMoneyMade()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "showMoneyPerHour",
		name = "Money/hr",
		description = "Show the estimated GE value of blood runes crafted per hour",
		section = displaySection
	)
	default boolean showMoneyPerHour()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "statTimeout",
		name = "Reset stats",
		description = "The time until the blood runecrafting session is reset"
	)
	@Units(Units.MINUTES)
	default int statTimeout()
	{
		return 5;
	}
}
