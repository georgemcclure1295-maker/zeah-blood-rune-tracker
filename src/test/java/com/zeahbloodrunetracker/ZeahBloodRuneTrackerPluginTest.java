package com.zeahbloodrunetracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ZeahBloodRuneTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ZeahBloodRuneTrackerPlugin.class);
		RuneLite.main(args);
	}
}
