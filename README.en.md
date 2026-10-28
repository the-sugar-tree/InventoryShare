<p align="center"><b>ENGLISH</b> | <a href="README.md">한국어</a></p>

# Inventory Sharing Plugin (v4.0)

> [!note]   
> For the ProtocolLib plugin for versions [1.12-1.20.4], see this [link](https://github.com/dmulloy2/ProtocolLib/releases)!

> [!note]   
> For the ProtocolLib plugin for versions [1.20.5+], see this [link](https://ci.dmulloy2.net/job/ProtocolLib/lastBuild/)! ([How to download](https://github.com/the-sugar-tree/InventoryShare/wiki/How-to-download-ProtocolLib))


> [!important]   
> Please read the whole README (below) first!!!


## Change Log   
- Supports 1.12.2 – 26.3 (Spigot, Paper)
- On 1.20.5 and above, items are saved in Minecraft's native format. Shulker box contents, player head skins, etc. are no longer lost, and Spigot and Paper can read each other's data. (See [Updating from 3.x](#updating-from-3x))
- Added the command permission `inventoryshare.command` (default: OP)
- Fixed an issue where data was saved repeatedly, once per world, whenever worlds were saved

---
<p align="center">
    <a href="https://github.com/the-sugar-tree/InventoryShare/releases/download/v4.0/InventoryShare-4.0.jar">
        <img src="https://img.shields.io/badge/Plugin%20Download-CLICK-bbbbbb?style=for-the-badge" height="40" width="260">
    </a>
</p>

---
[![Youtube](https://img.shields.io/badge/youtube-sugar_tree-red.svg?logo=youtube&style=for-the-badge)](https://www.youtube.com/channel/UCtqLK2FrJI9BNB0BI8-sWHA)
[![Release](https://img.shields.io/github/v/release/the-sugar-tree/InventoryShare?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare/releases/latest)
[![Java](https://img.shields.io/badge/java-8%2B-ED8B00.svg?logo=java&style=for-the-badge)](https://www.azul.com/)
[![DownloadCount](https://img.shields.io/github/downloads/the-sugar-tree/InventoryShare/total?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare/releases)   
[![SupportVersion](https://img.shields.io/badge/Supported%20Minecraft%20Version-1.12.2—26.3-green?style=for-the-badge)](https://github.com/the-sugar-tree/InventoryShare)
[![License](https://img.shields.io/github/license/the-sugar-tree/InventoryShare?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Gradle](https://img.shields.io/badge/gradle-9.8.2-02303A.svg?logo=gradle&style=for-the-badge)](https://gradle.org)

---
### Contacts
<sugar_tree@outlook.kr>
<tjfxkdskan@gmail.com>

### Additional Notes

If you use this plugin in a YouTube video, please add my GitHub link [ https://github.com/the-sugar-tree/InventoryShare ] to the video description.   

https://www.koreaminecraft.net/plugins/2902818   
https://blog.naver.com/sugar-tree/222556850608   

If the plugin is being distributed on any site other than the posts above and this GitHub page, please let me know at <sugar_tree@outlook.kr>. Thank you.

Using the external plugin \[[ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)] fixes the problems that occur when multiple players break blocks at the same time. ([How to download](https://github.com/the-sugar-tree/InventoryShare/wiki/How-to-download-ProtocolLib))

---

## Minecraft Version
### Current Versions
- 1.12.2-26.3 (Release 4.0) \[[Download](https://github.com/the-sugar-tree/InventoryShare/releases/download/v4.0/InventoryShare-4.0.jar)]

## Caveats
**This plugin is based on Spigot and Paper!**   
**Be sure to use it on a supported server software (bukkit)!**

### Updating from 3.x
On Minecraft 1.20.5 and above, the item storage format changes. **Before upgrading your server version**, start the server once with the new plugin on your current version, then shut it down. See the [wiki](https://github.com/the-sugar-tree/InventoryShare/wiki/Updating-to-4.0) for details.

## Help
To use the team sharing feature, first set up teams with the commands below
- /team add <team name> 
- /team join <team name> \[nickname]


- /inventoryshare teaminventory true

then you can share inventories within a team.  

The original inventory is stored separately, and players not assigned to a team use the global inventory.  
Team inventory sharing only works while global inventory sharing is enabled, and team inventories are also stored separately.  
However, due to a limitation of Paper servers, teams with no members are deleted when the server restarts, so please keep this in mind.

## Known Issues
- Even with team inventory enabled, advancements are not split per team; all players share them ([#6](https://github.com/the-sugar-tree/InventoryShare/issues/6)). Advancement sharing, including progress, will be moved to a separate plugin, and will be removed from this plugin at that point.

## Commands
![](https://raw.githubusercontent.com/the-sugar-tree/InventoryShare/master/help_message.png)   

Permission: `inventoryshare.command` (default: OP). You can grant it to non-OP players with a permissions plugin.

### Default Values
- inventory: true
- advancement: true
- announcedeath: false
- teaminventory: false

## Contributing
For build instructions and the contribution process, see [CONTRIBUTING.en.md](CONTRIBUTING.en.md); for an overview of the structure, see [docs/ARCHITECTURE.en.md](docs/ARCHITECTURE.en.md).

## License

This project is licensed under the \[GNU General Public License v3.0]. For details, see the [LICENSE](https://github.com/the-sugar-tree/InventoryShare/blob/master/LICENSE.md) file.

## Youtube

[![Video Label](http://img.youtube.com/vi/PI6eTCOs-x4/0.jpg)](https://www.youtube.com/watch?v=PI6eTCOs-x4)
