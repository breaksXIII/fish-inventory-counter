# Fish Inventory Counter

Counts every time your inventory fills up with fish. Set a goal in the plugin settings.

## Run it
1. Open this folder in IntelliJ IDEA (it'll load Gradle automatically).
2. Run `src/test/java/com/fishinvcounter/FishInvCounterPluginTest.java` (`main`).
   Add VM option `-ea` in the run config.
3. RuneLite opens with the plugin loaded. Log in with your Jagex account like normal
   (for Jagex accounts, see RuneLite wiki "Using Jagex Accounts" for dev mode login).

## Settings
- Goal: target number of full fish inventories
- Current count: set to 0 to reset
- Show overlay / Notify on goal
