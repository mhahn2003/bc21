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

- ~~Fix EC coms~~
- ~~Add unit differentiation in coms~~
- ~~Potential bug - not recognizing when EC switches sides? Need to test/debug more~~
- Politicians and Slanderers not patrolling HQ properly
- Politician AI
    - ~~Attacker~~
    - ~~Defender~~
    - ~~Fix running out of bytecode~~
    - ~~Should kill lone muckrakers if they're too close to the EC, since it prevents them from spawning slanderers~~
    - Check if running out of bytecode
- Slanderer AI
    - ~~Stay close to EC & muckraker~~
    - Pathfind to an edge, not going through enemy ECs
    - ~~Convert and run politician code when it switches~~
- Muckraker AI
    - ~~Spacing out when politicians are near~~
- General attack plan on enemy EC
    - Just spam politicians with conv 400, maybe add in lesser cost polis later
- EC building logic
    - ~~Less muckraker spam~~
    - ~~More politician building of cost ~15~~
    - ~~More slanderer building~~
    - Improve constants/round numbers
    - Fix EC building logic bug
- EC should bid if they are converted
    - Ideas: bid the amount of passive income?
    - ~~Currently bids if over 600 influence~~
- Analyze top teams:
    - Specifically: ~~baby ducks~~
    