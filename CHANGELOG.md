# Changelog

All notable changes to this project will be documented in this file.

## [2.0] - 05/07/2025
### Added
- **Database Support**: Added support for both SQLite and MySQL databases
  - SQLite as default (no configuration required)
  - MySQL support for larger servers and production environments
  - Automatic table creation and schema management
- **Database Migration System**: 
  - `/collectiondbmigrate` command for seamless migration between database types
  - Automatic detection of which database contains data
  - Bidirectional migration (SQLite ↔ MySQL)
  - Safe data merging using `INSERT IGNORE` to prevent data loss
  - Automatic backup creation of original database
  - Plugin reconnection after migration (no server restart required)
- **Notification Toggle System**:
  - `/collectionnotify` command to toggle collection notifications
  - GUI toggle button in collection interface
  - Persistent notification preferences stored in database
- **Enhanced Error Handling & Data Validation**:
  - Comprehensive material validation (ensures items are collectible)
  - UUID validation and format checking
  - Database schema validation
  - Graceful handling of corrupted or invalid data
  - Detailed logging for data issues and migration progress
- **Configuration System**:
  - New `config.yml` with database settings
  - User-friendly MySQL configuration with comments
  - Automatic YAML to database migration for existing users

### Changed
- **Storage System**: Migrated from YAML files to database storage
  - Better performance and data integrity
  - Support for concurrent access
  - Automatic migration from existing YAML data
- **Plugin Initialization**: Enhanced error handling during startup
  - Detailed error messages for database connection issues
  - Graceful plugin disable on initialization failure
  - Better logging for troubleshooting

### Fixed
- **Database Detection**: Fixed migration command to properly detect database with actual data
- **SQL Syntax**: Fixed SQLite-specific syntax issues during table creation
- **NullPointerExceptions**: Fixed issues during plugin disable with null database connections
- **Data Integrity**: Improved handling of invalid materials and UUIDs during migration

## [1.3] - 25/06/2024
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

## [1.2] - 25/06/2024
### Changed
- Leaderboard command renamed to `/coltop` for uniqueness
- Leaderboard now always shows the viewing player in the bottom right if not in the top 44
- Only one leaderboard page (no pagination)

## [1.1] - 25/06/2024
### Added
- Updated for Minecraft 1.21.6 and Spigot API 1.21.6
- Enhanced creative-only item filtering
- Improved logging for collectible item count

## [1.0] - 25/06/2024
### Added
- Initial release
- Track item pickups and display collection GUI
- Persistent player collections
- Basic statistics and progress tracking 