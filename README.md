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

- I don't think the wander() function is completely operational?
- Use IDs like baby duck to keep only a few muckrakers on each enemy politician to avoid too many units dying
- Need to figure out a way to stop getting overwhelmed when enemy sends muckrakers
    - Keep track of fPolcount and limit politician spawn
    - Just spam muckrakers anyway
- Muckrakers still getting stuck on edges for some reason
- How to deal with clogged up units near EC
- Optimize coms in terms of bytecode
- Change the cost of the search() politician when devs change the map specs
- Some politician either running out of bytecode or giving an error
- Politician
    - If muckraker buff is large enough, buff EC it just spawned from
    - Concentrate patrolling on the side where the attacks come from
- Slanderer
    - Try to avoid muckrakers from a further range
- Muckraker AI
    - Ideas for a 1000 muckraker rush?
- General attack plan on enemy EC
    - If there's a lot of politicians around the EC
- EC building logic
    - If it's too clogged up, use coms to indicate to increase the politician patrol radius
- ~~Fix EC bidding bug~~
- Analyze top teams:
    - Specifically:
    - baby ducks, PP, Super Cow Powers, Kryptonite, Nikola
- 500 cost muckraker surrounded with 1 cost polis