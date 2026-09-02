# Zeah Blood Rune Tracker

A RuneLite overlay based on the built-in Fishing session overlay. It tracks
only blood runes created by consuming dark essence fragments. It uses the raw
inventory quantities, so stacks above 100,000 are counted correctly even when
Old School RuneScape displays them as shortened white stacks.

The configurable overlay can display:

- whether the player is currently runecrafting;
- blood runes crafted this session;
- blood runes crafted per hour after the second craft;
- the current GE price of one blood rune;
- total money made; and
- money made per hour.

Each statistic can be switched on or off independently in the plugin settings.
Bank withdrawals and ground-item pickups are ignored because an increase is
only recorded on a game tick where the player also gains Runecrafting XP.
The bloods-per-hour and money-per-hour display refreshes every 10 seconds so
the estimates remain readable instead of changing every rendered frame.

Blood runes must enter the main inventory to be counted. If a rune pouch has
space for blood runes, bank it or remove blood runes from it before starting.

Right-click the overlay and choose **Reset** to clear the session. The automatic
session timeout can be changed in the plugin configuration.

## Development client

Run `./gradlew run` on macOS/Linux or `gradlew.bat run` on Windows.
