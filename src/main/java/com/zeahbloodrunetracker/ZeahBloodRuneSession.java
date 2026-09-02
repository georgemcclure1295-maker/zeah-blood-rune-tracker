package com.zeahbloodrunetracker;

import java.time.Duration;
import java.time.Instant;

class ZeahBloodRuneSession
{
	private int bloodRunes;
	private int crafts;
	private Instant startTime;
	private Instant lastCraftTime;

	void recordCraft(int amount, Instant now)
	{
		if (amount <= 0)
		{
			return;
		}

		if (startTime == null)
		{
			startTime = now;
		}

		bloodRunes += amount;
		crafts++;
		lastCraftTime = now;
	}

	void reset()
	{
		bloodRunes = 0;
		crafts = 0;
		startTime = null;
		lastCraftTime = null;
	}

	boolean isActive()
	{
		return lastCraftTime != null;
	}

	boolean hasRate()
	{
		return crafts >= 2;
	}

	boolean hasTimedOut(Instant now, Duration timeout)
	{
		return lastCraftTime != null && Duration.between(lastCraftTime, now).compareTo(timeout) >= 0;
	}

	int getBloodRunes()
	{
		return bloodRunes;
	}

	int getBloodRunesPerHour(Instant now)
	{
		if (!hasRate() || startTime == null)
		{
			return 0;
		}

		long elapsedMillis = Math.max(1L, Duration.between(startTime, now).toMillis());
		return (int) Math.round(bloodRunes * 3_600_000.0 / elapsedMillis);
	}
}
