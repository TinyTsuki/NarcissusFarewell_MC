🌐 [中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

# Narcissus Farewell (水仙辞)

A Minecraft Forge Teleport Command Mod.

## Table of Contents

- [Narcissus Farewell](#narcissus-farewell)
    - [Table of Contents](#table-of-contents)
    - [Definitions](#definitions)
    - [Introduction](#introduction)
    - [Features](#features)
    - [Configuration Instructions](#configuration-instructions)
    - [Command Instructions](#command-instructions)
    - [Notes](#notes)
    - [License](#license)

## Definitions

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

- **MORE**: ...

## Configuration Instructions

The local configuration file is located at [
`world/serverconfig/narcissus_farewell-server.toml`](narcissus_farewell-server.toml),
[`config/narcissus_farewell-common.toml`](narcissus_farewell-common.toml). For further details,
please refer to the comments in the default config file.

## Command Instructions

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

## Notes

- **Version Migration**: Upgrading a save file that used this mod from Minecraft 1.12.2 to a higher version may lead to
  various issues due to data incompatibility.

## License

MIT License

---

If you have any questions or suggestions, feel free to submit Issues or Pull requests.
