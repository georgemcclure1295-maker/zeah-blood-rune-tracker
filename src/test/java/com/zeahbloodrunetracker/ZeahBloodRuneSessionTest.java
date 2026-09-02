package com.zeahbloodrunetracker;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZeahBloodRuneSessionTest
{
	@Test
	public void tracksBloodRunesAndHourlyRate()
	{
		ZeahBloodRuneSession session = new ZeahBloodRuneSession();
		Instant start = Instant.parse("2026-01-01T00:00:00Z");
		session.recordCraft(50, start);
		session.recordCraft(50, start.plusSeconds(60));

		assertEquals(100, session.getBloodRunes());
		assertEquals(6000, session.getBloodRunesPerHour(start.plusSeconds(60)));
		assertTrue(session.hasRate());
	}

	@Test
	public void timesOutFromLastCraft()
	{
		ZeahBloodRuneSession session = new ZeahBloodRuneSession();
		Instant start = Instant.parse("2026-01-01T00:00:00Z");
		session.recordCraft(50, start);

		assertFalse(session.hasTimedOut(start.plusSeconds(299), Duration.ofMinutes(5)));
		assertTrue(session.hasTimedOut(start.plusSeconds(300), Duration.ofMinutes(5)));
	}

	@Test
	public void resetClearsTheSession()
	{
		ZeahBloodRuneSession session = new ZeahBloodRuneSession();
		session.recordCraft(50, Instant.now());
		session.reset();

		assertFalse(session.isActive());
		assertEquals(0, session.getBloodRunes());
	}

	@Test
	public void onlyCountsBloodsWhenRunecraftingXpIsGained()
	{
		assertEquals(52, ZeahBloodRuneTrackerPlugin.getCraftedBloodRuneIncrease(100, 152, true));
		assertEquals(220, ZeahBloodRuneTrackerPlugin.getCraftedBloodRuneIncrease(105_631, 105_851, true));
		assertEquals(0, ZeahBloodRuneTrackerPlugin.getCraftedBloodRuneIncrease(105_631, 105_851, false));
		assertEquals(0, ZeahBloodRuneTrackerPlugin.getCraftedBloodRuneIncrease(100, 100, true));
	}

	@Test
	public void doesNotCountOrdinaryInventoryIncreases()
	{
		assertEquals(0, ZeahBloodRuneTrackerPlugin.getCraftedBloodRuneIncrease(105_000, 110_000, false));
	}
}
