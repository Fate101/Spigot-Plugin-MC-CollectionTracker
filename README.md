# CollectionTracker

A comprehensive Minecraft plugin that allows players to track and collect all obtainable items in the game. Players can view their collection progress through an intuitive GUI interface and receive notifications when they discover new items.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.6-green.svg)
![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)
![Spigot API](https://img.shields.io/badge/Spigot%20API-1.21.6-orange.svg)

## 🌟 Features

- **Automatic Item Tracking**: Automatically detects when players pick up new items
- **Interactive GUI**: Browse your collection with a user-friendly interface
- **Collection Leaderboards**: Compete with other players and see who has the most complete collection
- **Progress Statistics**: View completion percentage and collection statistics
- **Cross-Version Compatibility**: Automatically supports new items from Minecraft updates
- **Persistent Storage**: Player collections are saved and persist across server restarts
- **Smart Filtering**: Excludes creative-only and unobtainable items
- **Real-time Notifications**: Get notified when you collect new items
- **Ranking System**: Color-coded rankings with special indicators for top 3 players

## 📋 Requirements

- **Minecraft Server**: 1.21.6 or higher
- **Java**: Version 17 or higher
- **Server Software**: Spigot, Paper, or any Spigot-based server

## 🚀 Installation

1. **Download** the latest `collectiontracker-1.1-SNAPSHOT.jar` from the releases
2. **Place** the JAR file in your server's `plugins` folder
3. **Restart** your server
4. **Verify** installation by checking the console for:
   ```
   [INFO] CollectionTracker initialized with [X] collectible items
   ```

## 📖 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/collection` | Opens your personal collection book | `collectiontracker.use` |
| `/coltop` | Opens the collection leaderboard | `collectiontracker.use` |

## 🎮 Usage

### For Players

1. **Start Collecting**: Simply play the game normally - the plugin automatically tracks items you pick up
2. **View Your Collection**: Use `/collection` to open your collection book
3. **Check Leaderboards**: Use `/coltop` to see how you rank against other players
4. **Track Progress**: Check the statistics book in the GUI to see your completion percentage
5. **Navigate**: Use the arrow buttons to browse through different pages of items or leaderboard entries

### For Server Administrators

The plugin requires no configuration and works out of the box. All player data is automatically saved to `plugins/CollectionTracker/collections.yml`.

## 🔧 Technical Details

### Automatic Item Detection

The plugin dynamically loads all available materials from the Bukkit API, ensuring compatibility with:
- Current Minecraft version (1.21.6)
- Future Minecraft updates
- All obtainable items and blocks

### Smart Filtering System

The plugin automatically excludes:
- Creative-only items (spawn eggs, command blocks, etc.)
- Technical blocks (barrier, light, jigsaw, etc.)
- Unobtainable items (bedrock, end portal frames, etc.)
- Armor trim templates (creative-only variants)

### Data Storage

- **File**: `plugins/CollectionTracker/collections.yml`
- **Format**: YAML configuration
- **Backup**: Automatically saves on plugin disable and item collection

## 🛠️ Building from Source

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Build Steps
```bash
# Clone the repository
git clone [repository-url]
cd Spigot-Plugin-MC-CollectionTracker

# Build the plugin
mvn clean package

# Find the JAR file in target/collectiontracker-1.1-SNAPSHOT.jar
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
| 1.21.6+ | 1.1-SNAPSHOT | ✅ Supported |
| 1.21.4 | 1.0-SNAPSHOT | ✅ Supported |
| 1.21.x | 1.0-SNAPSHOT | ✅ Supported |

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
3. **Report issues** on the GitHub repository
4. **Check existing issues** for similar problems

## 🔮 Future Plans

- [x] Collection leaderboards
- [ ] Collection categories (blocks, items, tools, etc.)
- [ ] Collection rewards system
- [ ] Export collection data
- [ ] Collection sharing between players

---

**Made with ❤️ for the Minecraft community**

*Last updated for Minecraft 1.21.6* 