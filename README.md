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

All of the bold points should be implemented before sprint 2

- Politician
    - **If muckraker buff is large enough, buff EC it just spawned from**
    - Concentrate patrolling on the side where the attacks come from
    - Account for enemy sending buffrakers
    - Check if there's too much of an imbalance between attack and defense politicians
- Slanderer
    - Position the slanderers away from the enemy ECs if they are known
    - Try to avoid muckrakers from a further range
- Muckraker AI
    - __Ideas for a 1000 muckraker rush? Surrounded by 1 cost muck/polis__
    - **Muckrakers still crowd around a politician, probably bc they come back after getting out of range 10**
    - **When wandering has a tendency to move in groups, split them up somehow?**
- Need to figure out a way to stop getting overwhelmed when enemy sends muckrakers
    - Keep track of fPolcount and limit politician spawn
    - Just spam muckrakers anyway
- Slanderer coms doesn't seem to be working properly? Need to test/debug more
- Analyze top teams:
    - Specifically:
    - baby ducks, PP, Super Cow Powers, Kryptonite, Nikola
    - ~~**Use IDs like baby duck to keep only a few muckrakers on each enemy politician to avoid too many units dying**~~

Order:
7. Slanderers not moving away from the EC for some reason
8. If muckraker buff is large enough, buff EC it just spawned from
9. Change moveAway such that it's in the movement so that people don't walk into it accidentally, and then this makes the people in the +4 radius not have to move away


Done:
- ~~Defense politician only needs to spread out from other defense, otherwise muckrakers can slip by~~
- ~~Need to attack EC if there's a ton of attack politicians not doing anything~~
- ~~Communicate EC influence~~
- ~~Change the cost of the search() politician, since devs changed the specs accordingly~~
- ~~Need to spawn more slanderers for more eco: I think pp does 1:1:1 ratio?~~
    - ~~Maybe separate between the initial ECs and the new ECs:~~
    - ~~The initial ECS focus on slanderers and politicians, while the new ones focus on muckrakers~~
    - ~~Still need more eco for the newer ecs~~
- ~~Fix bug where IC.MUCKRAKER is being used for ID and location, and also it sometimes overrides the signal for the muckraker to escape properly~~
- ~~When wandering has a tendency to move in groups, split them up somehow?~~
- ~~Improve muckraker spacing from each other, sometimes they just go wayy backwards~~
- Muckrakers still crowd around a politician, probably bc they come back after getting out of range 10

