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

## Building the jar on GitHub (no local setup)
1. Push this repository, including the `ironsuit-project-with-ci/ironsuit` folder and the root `.github` folder, to GitHub.
2. Open the **Actions** tab -> **Build Iron Suit mod** (it also runs on every push; or press **Run workflow**).
3. When it is green, open the run and download the **ironsuit-jar** artifact (a zip containing the `.jar`).
4. To publish a Release with the jar attached, push a tag: `git tag v0.1.0 && git push origin v0.1.0`.

The jar is built with JDK 25 + Gradle 9.1.0 installed by the workflow, so `gradlew` is not required.
