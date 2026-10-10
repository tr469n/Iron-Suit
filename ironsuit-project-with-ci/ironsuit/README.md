# Iron Suit (NeoForge 26.3)

Iron Man Mark 85 style armor mod.

## First-time setup
1. Install JDK 25.
2. Get the Gradle wrapper: copy `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`
   from the official template (https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle),
   or run `gradle wrapper` once with Gradle 9.1+ installed.
3. Open the folder in IntelliJ IDEA (Gradle project) and let it sync.
4. Run `./gradlew runClient`.

Check the newest NeoForge build at https://projects.neoforged.net/neoforged/neoforge
and update `neo_version` in gradle.properties if needed.

## Using the suit
1. Take the Nano-Reactor from the Iron Suit creative tab (or craft it) and right-click it to put it on your chest.
2. Take off other helmet / leggings / boots.
3. Press R (rebindable: Controls > Iron Suit) - the suit assembles piece by piece. Press R again to fold it back.
4. With helmet + chestplate on, the Mark 85 HUD is shown.
5. **Flight:** with the suit deployed, double-tap Space to take off (like creative flight). Space / Shift = up / down,
   Ctrl (sprint) = boost. Flying drains suit energy; at 0 energy flight stops. A powered suit cancels fall damage.
6. **Repulsors:** hold V (rebindable) to fire the palm repulsors. Costs a little energy per shot.
7. **Reactor beam:** hold B (rebindable) to charge the chest reactor, release to fire a heavy beam with an explosion.
   Charging drains energy; the longer the charge, the stronger the shot (minimum charge is about 0.75 s).
Tuning constants are at the top of `SuitFlight.java` and `SuitCombat.java`.
8. **Helmet:** press M (rebindable) to take the helmet off into the suit and press it again to put it on. With the helmet off
   the suit works exactly the same (flight, repulsors, reactor beam, fall protection); only the full helmet HUD is replaced
   by a small status panel. Folding the suit with R also takes the stored helmet back.
Weapon activity is logged to latest.log with the prefix `[IronSuit/combat]` (set `DEBUG = false` in `SuitCombat.java` to silence it).
