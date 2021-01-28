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

- **we just need much better exploration code**
- Still breaks on the wander points for some reason
- Politician
    - Concentrate patrolling on the side where the attacks come from
    - Assist politicians can also flank maybe?
- Slanderer
- Muckraker
    - muckrakers are not sparse enough
    - need more muckrakers to constantly get in and attack - maybe adjust the muckraker ratio a bit now that they're decent?
    - Send buffraker right after taken all the neutral ecs
    - Cardinal direction muckrakers for defense
    - early buff mucks
    - flanking seems very effective
    - account for 1 cost poli
    - heuristic isn't really working, look into a fix
- EC
    - Deal with big polis near our ec
- Analyze top teams:
    - Specifically:
    - baby ducks, PP, Super Cow Powers, Kryptonite, Nikola, Chop Suey, Malott Fat Cats
- Price calculation with unit count and influence

Order:
3. allocate some muckrakers to explore instead of all congregating to ec
4. signal big polis near ECs to either more poli reinforcements or more muckraker reinforcements
5. need to space our muckrakers more even still: analyze better muckraker micro, probably wololo

do until 1/27

3. code more robust defense; a lot of the times our ec has a lot of influence to work with but doesn't spend it and loses
6. when only small politicians around ec, don't care. if a big poli around ec, then just build muckrakers on cardinal directions
8. implement pathfinding to avoid 0.1 passability tiles at least for a little bit
9. remove ID from list to check if outputs an invalid category (maybe by adding a IC.REMOVEID category?)

do until deadline



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
- ~~Muckrakers still crowd around a politician, probably bc they come back after getting out of range 10~~
- ~~Not spawning polis to take over neutral ECs fast enough~~
- ~~If muckraker buff is large enough, buff EC it just spawned from~~
- ~~Debug wander and why units get stuck -> java ghosthug last round~~
- ~~Spawn less muckrakers and more politicians in the midgame~~
- ~~Attack politicians check for buffrakers on the way, and kill them if they can~~
- ~~Defense politician more efficient when defending against buffrakers~~
- ~~Remove self buff code~~
- ~~Self destruct useless politicians~~
- ~~fix attack politicians not exploding buffrakers~~
- ~~take neutral ecs first~~
- ~~build more slanderers even when we have more money~~
- ~~copy build order from top teams~~
- ~~make politicians surround the hq and take up spaces rather than attacking~~
- ~~Make closeness to HQ have a lot more weight than closeness to enemy HQ~~
- ~~improve muckraker spacing and spreading~~
- ~~fix mapSpots bug - maps are switched for some reason?~
- ~~EC not building polis to take over neutrals~~
- ~~Assist politicians~~
- ~~more politicians, less muckrakers~~
- ~~fix build order for newly converted ecs~~
- ~~build more politicians than slanderers~~
- ~~fix signals bouncing back and forth with neutral ecs: feeling it's probably something with relevant flags~~
- ~~completely revamp the wander function~~
- ~~need to find corners much faster~~
- ~~use symmetry to find supposed ec locations~~