# Battlecode 2021 Scaffold

This is the Battlecode 2021 scaffold, containing an `examplefuncsplayer`. Read https://2021.battlecode.org/getting-started!

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.


### Useful Commands

- `./gradlew run`
    Runs a game with the settings in gradle.properties
- `./gradlew update`
    Update to the newest version! Run every so often


### To-do list

- Optimize the wander function
    - Keep an array of the map compacted to keep track of where we visited
- Optimize coms in terms of bytecode
- Politician
    - If muckraker buff is large enough, buff EC it just spawned from
- Slanderer
    - ~~Stay in a lattice to allow others to path~~
    - Try to avoid muckrakers from a further range
- Muckraker AI
    - ~~Make them go towards enemy, not only enemy EC~~
    - Muckrakers should never be in our base, always in theirs or scouting
    - Use IDs like baby duck to keep only a few on each enemy politician to avoid too many units dying
    - Ideas for a 1000 muckraker rush?
- General attack plan on enemy EC
    - If there's a lot of politicians around the EC
- EC building logic
    - If there's a constant clog in the middle, use coms to communicate to spread out and increase the patrol radius
- ~~Fix EC bidding bug~~
- Analyze top teams:
    - Specifically:
    - baby ducks, PP, Super Cow Powers, Kryptonite, Nikola
