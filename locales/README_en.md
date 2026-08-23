<div align="center">

| [中文](../README.md) | [English](README_en.md) | [日本語](README_ja.md) |
|:------------------:|:-----------------------:|:-------------------:|

<img src="../assets/logo.png" alt="Narcissus Farewell" width="320" />

# Narcissus Farewell

**A Minecraft Forge, Fabric, and NeoForge teleport command mod.**

</div>

---

## Table of Contents

- [Narcissus Farewell](#narcissus-farewell)
    - [Table of Contents](#table-of-contents)
    - [Meaning](#meaning)
    - [Introduction](#introduction)
    - [Features](#features)
    - [Configuration](#configuration)
    - [Commands](#commands)
    - [Notes](#notes)
    - [Performance Tests](#performance-tests)
    - [Building](#building)
    - [License](#license)

## Meaning

- **Narcissus**: Symbolizes longing, auspiciousness, reunion, new beginnings, and beautiful hope.
- **Farewell**: To say goodbye, to not accept, to request leaving.
- **Narcissus Farewell**: Longing (home), reunion (tpa, tph), new beginnings (back), <del>leaving without farewell (tpx,
  tpr)</del>.
- <del>Drink the strong narcissus tea, then bid farewell to the world (feed).</del>

## Introduction

This project is designed for Minecraft Forge servers to implement commands such as teleporting to a player, going home,
or returning.
This mod is required on the server side, and optional on the client side.

## Features

- **Teleport with Specified Dimension**: In most teleportations, you can specify the dimension—go wherever you wish.
- **Teleport to Structures/Biomes**: Teleport to naturally generated structures or biomes nearby, so your mom will never
  worry about you not finding a village or fortress.
- **Teleport to a Player**: Request to teleport yourself to a player, or request to teleport a player to you.
- **Teleport to Spawn**: Teleport to the world spawn point or your personal spawn point.
- **Custom Teleport Points**: Set custom teleport points and use commands/hotkeys to teleport to them.
- **Teleport Upwards/Downwards**: Teleport to the nearest standable block above or below.
- **Teleport to Top/Bottom**: Teleport to the highest standable block above or the lowest standable block below.
- **Teleport to the End of Your Line of Sight**: Teleport in the direction you’re looking until a block obstructs your
  view or the preset maximum distance is reached.
- **Teleport to Server Preset Point**: Set a server default teleport point to allow players to teleport at will.
- **Return to the Last Departed Location**: Return to the location you teleported from last time.
- **Teleport to Gravestone/Corpse**: Teleport to your last death location or a nearby gravestone/corpse; supports
  multiple gravestone mods; when holding an obituary-like item, teleport to the recorded location.
- **Waypoint List Screen**: Client-side waypoint list for personal points and stations, with quick teleport.
- **Mod Integrations**: Works with gravestone mods (Gravestone Mod, Simple Tomb, Corail Tombstone, Corpse), map mods (
  Xaero's Minimap, JourneyMap, FTB Chunks), and ApricityUI for an extended teleport and map experience.
- **Safe Teleportation**: Choose safe teleportation with every teleport to avoid landing in the void, inside blocks, or
  in lava.
- **Hotkeys**: At any time and place, simply press the designated key to quickly execute home, back, accept, or decline
  commands.
- **Virtual Permission System**: Even without enabling cheat mode, you can grant players permission to use specific
  teleport commands by modifying the config file.
- **Terrible Translation**: The textual descriptions might be ambiguous or not clearly expressed <del>(not just in
  English)</del>.
- **Terrible Code**: Bad code + careless testing = a pile of stinky bugs.

## TODO

- **Add config**: Post-teleport cost
- **Add config**: Pre- and post-teleport prompts
- **Add config**: Disable teleport in the End when the Ender Dragon is not defeated
- **Add config**: Disable specific teleport commands in specified dimensions
- **Add config**: Particle effects on teleport

## Configuration

Configuration can be changed through the Narcissus Farewell configuration editor or by editing the files below. Refer
to in-game tooltips and generated comments for the meaning and valid range of each option.

### Shared Files

- Teleport points, history, permissions, and player preferences are stored in shared Vanilla Xin player data:
  `world/vanilla.xin/playerdata/*.nbt`
- Shared Vanilla Xin settings such as language preferences: `config/vanilla.xin/common_config.json`

### Mod Files

- Common and server-behavior Config: [`config/narcissus_farewell-common.toml`](/config/narcissus_farewell-common.toml)
- Client Config: [`config/narcissus_farewell-client.toml`](/config/narcissus_farewell-client.toml)

## Commands

- **dim**: Get the dimension ID of the current world.
- **tpx**: Teleport to the specified coordinates or player.
  **Parameter List**:
    1. `<player> [<safe teleport flag>]`
    2. `<coordinates> [<safe teleport flag>] [<dimension>]`

- **tpst**: Teleport to the specified structure or biome (safe by default).
  **Parameter List**:
    1. `<structure> [<search range>] [<dimension>]`
    2. `<biome> [<search range>] [<dimension>]`

- **tpa**: Request to teleport yourself to a player. If no target is specified, the player from the last request is
  used.
  **Parameter List**:
    1. `[<player>] [<safe teleport flag>]`

- **tpay**: Accept a request to teleport a player to you. If no parameters are specified, the most recent request is
  accepted.
  **Parameter List**:
    1. `[<player>]`
    2. `[<nth most recent request>]`
    3. `[<request ID>]`

- **tpan**: Decline a request to teleport a player to you. If no parameters are specified, the most recent request is
  declined.
  **Parameter List**:
    1. `[<player>]`
    2. `[<nth most recent request>]`
    3. `[<request ID>]`

- **tph**: Request to teleport a player to you. If no target is specified, the player from the last request is used.
  **Parameter List**:
    1. `[<player>] [<safe teleport flag>]`

- **tphy**: Accept a request to teleport yourself to a player. If no parameters are specified, the most recent request
  is accepted.
  **Parameter List**:
    1. `[<player>]`
    2. `[<nth most recent request>]`
    3. `[<request ID>]`

- **tphn**: Decline a request to teleport yourself to a player. If no parameters are specified, the most recent request
  is declined.
  **Parameter List**:
    1. `[<player>]`
    2. `[<nth most recent request>]`
    3. `[<request ID>]`

- **tpr**: Teleport to a random location.
  **Parameter List**:
    1. `[<random range>] [<safe teleport flag>] [<dimension>]`

- **tpsp**: Teleport to the player's own respawn point.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpws**: Teleport to the world spawn point.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpt**: Teleport to the farthest standable block above.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpb**: Teleport to the farthest standable block below.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpu**: Teleport to the nearest standable block above.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpd**: Teleport to the nearest standable block below.
  **Parameter List**:
    1. `[<safe teleport flag>]`

- **tpv**: Teleport to the end of your line of sight.
  **Parameter List**:
    1. `[<safe teleport flag>] [<maximum distance>]`

- **home**: Teleport to the preset personal teleport point. Each dimension can have one default teleport point.
  If no teleport point name is specified:
    - If there is only one personal teleport point, that point is used;
    - If multiple personal teleport points exist but only one is set as default, the default is used;
    - If multiple default teleport points exist, the default in the current dimension is used.
      **Parameter List**:

    1. `<teleport point name> [<safe teleport flag>] [<dimension>]`
    2. `[<safe teleport flag>] [<dimension>]`

- **sethome**: Add the player's current coordinates to the personal teleport point list. If no name is specified, the
  default name "home" is used.
  **Parameter List**:
    1. `<teleport point name> [<default teleport flag>]`

- **delhome**: Delete an already set personal teleport point. The current dimension ID can be checked with a command.
  **Parameter List**:
    1. `<teleport point name> [<dimension>]`

- **stage**: Teleport to the preset station (public teleport point). If no name is specified, teleport to the nearest
  station.
  **Parameter List**:
    1. `<station name> [<safe teleport flag>] [<dimension>]`

- **setstage**: Add the current or specified coordinates to a station (public teleport point).
  **Parameter List**:
    1. `<station name> [<coordinates>] [<dimension>]`

- **delstage**: Delete an already set station (public teleport point). The current dimension ID can be checked with a
  command.
  **Parameter List**:
    1. `<station name> [<dimension>]`

- **back**: Return to the coordinates from which you last teleported or your death location.
  **Parameter List**:
    1. `[<safe teleport flag>] [<teleport type>] [<dimension>]`

- **grave**: Teleport to your last death location or a nearby gravestone/corpse. If holding an obituary-like item,
  teleport to the location recorded by that item.
  **Parameter List**:
    1. No arguments: teleport to the most recent death location (or a nearby gravestone/corpse)
    2. `<search range>`: search for gravestone/corpse within the given block range and teleport there
    3. `<dimension>`: teleport to your last death location in that dimension

- **fly**: Enable or disable creative-style flight (optionally for a target player, with configurable speed).
  **Parameter List**:
    1. `[<enable/disable>] [<player>] [<fly speed>]`

---

## Notes

- **Version Migration**: Upgrading a save file that used this mod from Minecraft 1.12.2 to a higher version may lead to
  various issues due to data incompatibility.

---

## Performance Tests

| Minecraft | 1.16.5             |
|-----------|--------------------|
| OS        | Windows 11 (amd64) |
| Java      | 1.8.0_422, Temurin |
| Memory    | 14044MB            |
| CPU       | AMD Ryzen 7 9700X  |

The following timings, sorted by total duration, were measured in a development environment using the default
configuration in the End for versions 1.1.2 and earlier. The command teleports to a random safe coordinate:
`/narcissus tpr 10000 safe`. Results are for reference only.

| #  | Gen (ms) | Sort (ms) | Find (ms) | Total (ms) | Safe |
|----|----------|-----------|-----------|------------|------|
| 1  | 10       | 127       | 223       | 360        | Yes  |
| 2  | 8        | 124       | 433       | 565        | Yes  |
| 3  | 12       | 146       | 513       | 671        | Yes  |
| 4  | 8        | 132       | 944       | 1084       | Yes  |
| 5  | 8        | 132       | 1228      | 1368       | Yes  |
| 6  | 8        | 138       | 1718      | 1864       | Yes  |
| 7  | 9        | 137       | 2659      | 2805       | Yes  |
| 8  | 8        | 132       | 2882      | 3022       | Yes  |
| 9  | 7        | 128       | 3044      | 3179       | Yes  |
| 10 | 8        | 136       | 3595      | 3739       | Yes  |
| 11 | 7        | 118       | 3904      | 4029       | Yes  |
| 12 | 8        | 144       | 4234      | 4386       | Yes  |
| 13 | 10       | 144       | 4493      | 4647       | Yes  |
| 14 | 9        | 138       | 9745      | 9892       | Yes  |
| 15 | 11       | 144       | 54968     | 55123      | No   |

Version 1.1.3 (same test):

| #  | Gen (ms) | Sort (ms) | Find (ms) | Total (ms) | Safe |
|----|----------|-----------|-----------|------------|------|
| 1  | 0        | 4         | 387       | 391        | Yes  |
| 2  | 1        | 7         | 420       | 428        | Yes  |
| 3  | 0        | 4         | 514       | 518        | Yes  |
| 4  | 0        | 3         | 536       | 539        | Yes  |
| 5  | 0        | 3         | 561       | 564        | Yes  |
| 6  | 1        | 4         | 565       | 570        | Yes  |
| 7  | 1        | 3         | 1053      | 1057       | Yes  |
| 8  | 2        | 2         | 1336      | 1340       | No   |
| 9  | 0        | 5         | 1382      | 1387       | No   |
| 10 | 0        | 3         | 1392      | 1395       | No   |
| 11 | 1        | 4         | 1442      | 1447       | No   |
| 12 | 1        | 5         | 1443      | 1449       | No   |
| 13 | 0        | 4         | 1494      | 1498       | No   |
| 14 | 2        | 9         | 1499      | 1510       | No   |
| 15 | 1        | 3         | 1618      | 1622       | No   |

Version 1.1.6 (same test):

| #  | Total (ms) | Safe |
|----|------------|------|
| 1  | 214        | Yes  |
| 2  | 233        | Yes  |
| 3  | 383        | Yes  |
| 4  | 427        | Yes  |
| 5  | 429        | Yes  |
| 6  | 434        | Yes  |
| 7  | 605        | Yes  |
| 8  | 640        | Yes  |
| 9  | 658        | Yes  |
| 10 | 718        | Yes  |
| 11 | 1098       | Yes  |
| 12 | 1512       | No   |
| 13 | 1578       | No   |
| 14 | 1716       | No   |
| 15 | 1863       | No   |

---

## Building

The docs branch provides one build entry for all maintained branches:

```bat
scripts\build-all.bat
```

By default, it dynamically builds all local `forge/*`, `fabric/*`, and `neoforge/*` branches. Other namespaces such
as `dev/*` and `maintenance/*` are excluded. Each branch is built in a detached temporary worktree without switching
the current checkout.

List selected branches and validate JDK discovery without running Gradle:

```bat
scripts\build-all.bat -ListOnly
```

Select branches with glob expressions:

```bat
scripts\build-all.bat -BranchExpression "forge/*"
scripts\build-all.bat -BranchExpression "*/21.1"
scripts\build-all.bat -BranchExpression "forge/*,!forge/16.5"
scripts\build-all.bat -BranchExpression "fabric/18.2"
```

Expressions beginning with `!` exclude matching branches. The previous parameter name `-Branches` remains available
as an alias.

---

## License

**MIT License**

If you have any questions or suggestions, feel free to submit Issues or Pull requests.
