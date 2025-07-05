# CollectionTracker

A comprehensive Minecraft plugin that allows players to track and collect all obtainable items in the game. Players can view their collection progress through an intuitive GUI interface and receive notifications when they discover new items.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.6-green.svg)
![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)
![Spigot API](https://img.shields.io/badge/Spigot%20API-1.21.6-orange.svg)

## 🌟 Features

- **Automatic Item Tracking**: Detects when players pick up, craft, smelt, trade, brew, or interact with buckets to obtain new items (Survival mode only)
- **Interactive GUI**: Browse your collection with a user-friendly interface
- **Collection Leaderboards**: Compete with other players and see who has the most complete collection
- **Progress Statistics**: View completion percentage and collection statistics
- **Cross-Version Compatibility**: Automatically supports new items from Minecraft updates
- **Flexible Storage**: Support for both SQLite and MySQL databases with automatic migration
- **Smart Filtering**: Excludes creative-only and unobtainable items
- **Real-time Notifications**: Get notified when you collect new items (toggleable)
- **Ranking System**: Color-coded rankings with special indicators for top 3 players
- **Database Migration**: Seamless migration between SQLite and MySQL with a single command
- **Robust Error Handling**: Comprehensive data validation and corruption protection

## 📋 Requirements

- **Minecraft Server**: 1.21.6 or higher
- **Java**: Version 17 or higher
- **Server Software**: Spigot, Paper, or any Spigot-based server
- **Database**: SQLite (included) or MySQL 5.7+ (optional)

## 🚀 Installation

1. **Download** the latest `collectiontracker-2.0.jar` from the releases
2. **Place** the JAR file in your server's `plugins` folder
3. **Restart** your server
4. **Configure** database settings in `plugins/CollectionTracker/config.yml` (optional)
5. **Verify** installation by checking the console for:
   ```
   [INFO] CollectionTracker initialized with [X] collectible items
   ```

## 📖 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/collection` | Opens your personal collection book | `collectiontracker.use` |
| `/coltop` | Opens the collection leaderboard | `collectiontracker.use` |
| `/collectionnotify` | Toggle collection notifications on/off | `collectiontracker.use` |
| `/collectiondbmigrate` | Migrate data between SQLite and MySQL | `collectiontracker.admin` |

## 🎮 Usage

### For Players

1. **Start Collecting**: Play the game normally—items you pick up, craft, smelt, trade, or brew are automatically tracked
2. **View Your Collection**: Use `/collection` to open your collection book
3. **Check Leaderboards**: Use `/coltop` to see how you rank against other players
4. **Toggle Notifications**: Use `/collectionnotify` to turn collection messages on/off
5. **Track Progress**: Check the statistics book in the GUI to see your completion percentage
6. **Navigate**: Use the arrow buttons to browse through different pages of items or leaderboard entries

### For Server Administrators

#### Basic Configuration
The plugin works out of the box with SQLite storage. For MySQL support, edit `plugins/CollectionTracker/config.yml`:

```yaml
database:
  type: "sqlite"  # or "mysql"
  
  # SQLite settings (default)
  sqlite:
    filename: "collections.db"
  
  # MySQL settings (optional)
  mysql:
    host: "localhost"
    port: 3306
    database: "collectiontracker"
    username: "root"
    password: "password"
```

#### Database Migration
To migrate between database types:

1. **Change the config** to your desired database type
2. **Restart the server** (plugin will connect to new database)
3. **Run the migration command**:
   ```
   /collectiondbmigrate
   ```
4. **The plugin automatically**:
   - Detects which database has data
   - Migrates all collections and notification settings
   - Creates backups of the original database
   - Reconnects to the new database

## 🔧 Technical Details

### Database Support

#### SQLite (Default)
- **File**: `plugins/CollectionTracker/collections.db`
- **Pros**: No setup required, portable, included with plugin
- **Best for**: Small to medium servers, development, testing

#### MySQL
- **Pros**: Better performance for large servers, concurrent access, backup tools
- **Best for**: Large servers, production environments, multiple servers sharing data

#### Migration Features
- **Bidirectional**: Migrate from SQLite to MySQL or vice versa
- **Automatic Detection**: Detects which database contains data
- **Safe Merging**: Uses `INSERT IGNORE` to prevent data loss
- **Backup Creation**: Automatically backs up original database
- **Data Validation**: Comprehensive error checking and corruption protection

### Comprehensive Item Detection

The plugin tracks new items acquired by Survival mode players via:
- **Picking up** items from the ground
- **Crafting** items in a crafting table
- **Smelting** items in a furnace or blast/smoker
- **Trading** with villagers (merchant inventories)
- **Brewing** potions in a brewing stand
- **Bucket interactions** (filling with water, lava, milk, powder snow, etc.)
- **Any inventory click** that adds a new item to the player's inventory (except in plugin GUIs)

> **Note:** Only items obtained in Survival mode are counted, to prevent cheating or accidental triggers in Creative/Adventure/Spectator modes.

### Smart Filtering System

The plugin automatically excludes:
- Creative-only items (spawn eggs, command blocks, etc.)
- Technical blocks (barrier, light, jigsaw, etc.)
- Unobtainable items (bedrock, end portal frames, etc.)
- Armor trim templates (creative-only variants)

### Data Validation & Error Handling

The plugin includes comprehensive data validation:
- **Material Validation**: Ensures materials are valid and collectible
- **UUID Validation**: Validates player UUIDs are properly formatted
- **Schema Validation**: Checks database table structure integrity
- **Migration Safety**: Handles corrupted or invalid data gracefully
- **Logging**: Detailed warnings for any data issues found

## 🛠️ Building from Source

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build Steps
```bash
# Clone the repository
git clone https://github.com/Fate101/Spigot-Plugin-MC-CollectionTracker
cd Spigot-Plugin-MC-CollectionTracker

# Build the plugin
mvn clean package

# Find the JAR file in target/collectiontracker-2.0.jar
```

## 📊 Collection Statistics

The plugin tracks:
- **Total Items**: All obtainable items in the current Minecraft version
- **Collected Items**: Items the player has picked up at least once
- **Completion Percentage**: Progress towards completing the collection
- **Real-time Updates**: Statistics update immediately when new items are collected

## 🔄 Version Compatibility

| Minecraft Version | Plugin Version | Status |
|------------------|----------------|--------|
| 1.21.6+ | 2.0 | ✅ Supported |
| 1.21.4 | 1.0–1.4 | ✅ Supported |
| 1.21.x | 1.0–1.4 | ✅ Supported |

## 🎯 New Items in 1.21.6

The plugin automatically supports all new items from Minecraft 1.21.6, including:
- **Breeze Rods** and related items
- **Trial Keys** and **Trial Chambers** items
- **Copper Bulbs** and related redstone components
- **Crafter** blocks and related items
- **Pottery Sherds** and **Decorated Pots**
- **Various new decorative blocks**

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📝 License

This project is licensed under the GNU General Public License v3 (GPL-3.0) - see the LICENSE file for details.

**What this means:**
- ✅ You are free to use, modify, and distribute this code
- ✅ You must keep any modifications open source under the same license
- ✅ You cannot lock this code behind a paywall or make it proprietary
- ✅ If you distribute this code or modifications, you must provide the source code

## 🆘 Support

If you encounter any issues or have questions:

1. **Check the console** for error messages
2. **Verify compatibility** with your server version
3. **Check database configuration** if using MySQL
4. **Report issues** on the GitHub repository
5. **Check existing issues** for similar problems

## 🔮 Future Plans

- [x] Collection leaderboards
- [x] Comprehensive item acquisition tracking (crafting, smelting, trading, brewing, etc.)
- [x] Database support (SQLite/MySQL)
- [x] Notification toggle system
- [x] Database migration tools
- [x] Enhanced error handling and data validation
- [ ] Collection categories (blocks, items, tools, etc.)
- [ ] Collection rewards system
- [ ] Export collection data
- [ ] Collection sharing between players

---

**Made with ❤️ for the Minecraft community**

*Last updated for Minecraft 1.21.6* 