# Changelog

All notable changes to this project will be documented in this file.

## [1.3] - 2024-06-25
### Added
- Comprehensive item acquisition tracking:
  - Crafting (CraftItemEvent)
  - Smelting (FurnaceExtractEvent)
  - Trading (MerchantInventory result slot)
  - Brewing (BrewerInventory result slot)
  - Bucket interactions (water, lava, milk, powder snow, etc.)
  - General inventory clicks (except plugin GUIs)
- Only Survival mode players can collect items (anti-cheat)
- Improved documentation and README

### Fixed
- Player heads in leaderboard now show correct skins
- Prevented collection from plugin GUIs

## [1.2] - 2024-06-25
### Changed
- Leaderboard command renamed to `/coltop` for uniqueness
- Leaderboard now always shows the viewing player in the bottom right if not in the top 44
- Only one leaderboard page (no pagination)

## [1.1] - 2024-06-25
### Added
- Updated for Minecraft 1.21.6 and Spigot API 1.21.6
- Enhanced creative-only item filtering
- Improved logging for collectible item count

## [1.0] - 2024-06-25
### Added
- Initial release
- Track item pickups and display collection GUI
- Persistent player collections
- Basic statistics and progress tracking 