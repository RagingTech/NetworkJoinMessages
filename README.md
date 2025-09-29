# NetworkJoinMessages

![GitHub release (latest by date)](https://img.shields.io/github/v/release/RagingTech/NetworkJoinMessages)
![zlib license](https://img.shields.io/badge/license-zlib-orange)
![GitHub downloads](https://img.shields.io/github/downloads/RagingTech/NetworkJoinMessages/total?label=GitHub%20downloads)
[![SpigotMC downloads](https://img.shields.io/spiget/downloads/118643?label=SpigotMC%20downloads)](https://www.spigotmc.org/resources/118643/)

A modern continuation of [BungeeJoinMessages](https://github.com/Tirco/BungeeJoinMessages), this plugin handles customizable join, leave, and server-swap messages across proxy servers like BungeeCord and Velocity.

## Features
- Flexible joining, leaving, and server-swapping notifications
- Compatible with BungeeCord and Velocity proxy setups
- Easy-to-use configuration with powerful placeholder support
  - [LuckPerms](https://luckperms.net/) (prefix and suffix)
  - [PAPIProxyBridge](https://www.spigotmc.org/resources/papiproxybridge.108415/)
  - [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders)
- Support for legacy and MiniMessage formatting
- Support for [SayanVanish](https://modrinth.com/plugin/sayanvanish), [PremiumVanish](https://www.spigotmc.org/resources/premiumvanish-stay-hidden-bungee-velocity-support.14404/) and [LimboAPI](https://github.com/Elytrium/LimboAPI)

## Installation
1. Download the latest release from [SpigotMC](https://www.spigotmc.org/resources/118643/)
2. Place the JAR file in the `plugins` directory of your proxy server
3. Restart the proxy to generate the default configuration
4. Edit the `config.yml` file in the plugin's directory to your liking following changes with `/njoinreload`
5. Optionally, configure Discord messages in the `discord.yml` file also following changes with `/njoinreload`

> [!NOTE]
> If you want the vanilla join/leave messages to go away, you can remove them with [EssentialsX](https://essentialsx.net/downloads.html), or use [this](https://www.spigotmc.org/resources/join-and-leave-message-disabler.88850/) basic plugin.

## Placeholders

Buit-in placeholders are described below. As mentioned before, PlaceholderAPI placeholders are supported given the PAPIProxyBridge plugin is on the proxy and backend servers. MiniPlaceholders are also supported given the MiniPlaceholders plugin is on the proxy.

### Any of the three message types:

- _%player%_ Player username
- _%displayname%_ Custom name the player has according to the proxy
- _%player_prefix%_ Player prefix (requires LuckPerms on the proxy)
- _%player_suffix%_ Player suffix (requires LuckPerms on the proxy)

### Swap server message:

- _%to%_ The server they are going to
- _%to_clean%_ ^ Same, but with color codes removed.
- _%from%_ The server they are coming from
- _%from_clean%_ ^ Same, but with color codes removed.
- _%playercount_to%_ The amount of players on the server they going to.
- _%playercount_from%_ The amount of players on the server they are coming from.
- _%playercount_network%_ The amount of players on the network.

### Join & leave network messages:
- _%server_name%_ The name of the server they joined to or was last on when they left.
- _%server_name_clean%_ ^ Same, but with color codes removed.
- _%playercount_server%_ The amount of players on the server they connected to, or was last on when they left.
- _%playercount_network%_ The amount of players on their network.

> [!TIP]
> Remember that the command `/njoinreload` can be used to reload the messages without having to restart the server. Requires the networkjoinmessages.reload permission.

## Permissions

|Permission|Description|
|---|---|
|networkjoinmessages.silent|No message is displayed on join, leave or swap for the holder. This permission is meant for those who often join while vanished.|
|networkjoinmessages.spoof|Allows the use of the `/njoinspoof` command. The command will display a fake join/leave/swap message, based on the arguments given.|
|networkjoinmessages.reload|Allows you to reload the configuration file with the `/njoinreload` command.|
|networkjoinmessages.toggle|Allows the usage of the `/njointoggle` command, that lets users choose not to receive certain messages just for them.|
|networkjoinmessages.import|Allows importing users from backend servers' usercache.json files with the `/njoinimport` command.|

## Commands

Required arguments are enclosed with angle brackets (i.e. <this\>) and optional arguments are enclosed with square brackets (i.e. [this\]). An ellipsis (...) indicates an argument can be specified several times separated by spaces.

|Command|Description|
|---|---|
|/njoinspoof <join \| leave \| swap \| toggle\> [<from\> <to\>\]|Displays a fake message using the issuer as the trigger player.|
|/njoinreload|Reloads the configuration.|
|/njointoggle <all \| join \| leave \| swap\> <on \| off\>|Toggles the issuers receiving of specific messages.|
|/njoinimport <pathtousercache.json\> [pathtousercache.json...\]|Imports user's from backend servers' usercache.json files into the joined database so that they will not trigger first join messages. Useful for adding this plugin and using first join messages on an already running network.|

## Contributing

Contributions are encouraged!

- Open issues for bug reports or suggestions
- Submit pull requests for new features or fixes

## License

Licensed under [zlib License](https://zlib.net/zlib_license.html) by the original author Tirco. You are free to use, modify, and redistribute this plugin, as long as credit is retained.

## Support

- Open an issue for bugs or feature requests
- Join the [discord server](https://earthcow.xyz/discord) for help


