package com.fate101.collectiontracker;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final FileConfiguration config;
    private Connection connection;
    private final String databaseType;
    
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.getConfig();
        this.databaseType = config.getString("database.type", "sqlite").toLowerCase();
    }
    
    public boolean initialize() {
        try {
            if ("mysql".equals(databaseType)) {
                return initializeMySQL();
            } else {
                return initializeSQLite();
            }
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean migrateFromYaml() {
        File oldCollectionsFile = new File(plugin.getDataFolder(), "collections.yml");
        if (!oldCollectionsFile.exists()) {
            logger.info("No existing YAML data found to migrate");
            return true;
        }
        
        try {
            logger.info("Found existing YAML data, starting migration...");
            FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldCollectionsFile);
            
            int migratedPlayers = 0;
            int migratedItems = 0;
            
            for (String uuidString : oldConfig.getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    List<String> materialNames = oldConfig.getStringList(uuidString);
                    Set<Material> materials = new HashSet<>();
                    
                    for (String materialName : materialNames) {
                        try {
                            Material material = Material.valueOf(materialName);
                            // Additional validation: check if material is actually collectible
                            if (material.isItem() && !material.isAir()) {
                                materials.add(material);
                                migratedItems++;
                            } else {
                                logger.warning("Skipping non-collectible material during migration: " + materialName);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid material name during migration: " + materialName);
                        }
                    }
                    
                    if (!materials.isEmpty()) {
                        savePlayerCollection(playerUUID, materials);
                        migratedPlayers++;
                    }
                    
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID during migration: " + uuidString);
                }
            }
            
            // Backup the old file
            File backupFile = new File(plugin.getDataFolder(), "collections.yml.backup");
            if (oldCollectionsFile.renameTo(backupFile)) {
                logger.info("Migration completed successfully!");
                logger.info("Migrated " + migratedPlayers + " players with " + migratedItems + " total items");
                logger.info("Original YAML file backed up to: collections.yml.backup");
                return true;
            } else {
                logger.warning("Migration completed but could not backup original file");
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Failed to migrate YAML data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean migrateToMySQL(String mysqlHost, int mysqlPort, String mysqlDatabase, String mysqlUsername, String mysqlPassword) {
        if (!"sqlite".equals(databaseType)) {
            logger.warning("Migration to MySQL only works when currently using SQLite");
            return false;
        }
        
        try {
            logger.info("Starting migration from SQLite to MySQL...");
            
            // Load all current data from SQLite
            Map<UUID, Set<Material>> allCollections = loadAllCollections();
            Set<UUID> notificationSettings = loadNotificationSettings();
            
            if (allCollections.isEmpty() && notificationSettings.isEmpty()) {
                logger.info("No data to migrate from SQLite");
                return true;
            }
            
            // Create MySQL connection
            String mysqlUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                    mysqlHost, mysqlPort, mysqlDatabase);
            
            try (Connection mysqlConnection = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword)) {
                
                // Create tables in MySQL
                createTablesInConnection(mysqlConnection);
                
                // Migrate collections
                int migratedPlayers = 0;
                int migratedItems = 0;
                
                for (Map.Entry<UUID, Set<Material>> entry : allCollections.entrySet()) {
                    UUID playerUUID = entry.getKey();
                    Set<Material> materials = entry.getValue();
                    
                    // Insert collections into MySQL
                    String insertSql = "INSERT IGNORE INTO player_collections (player_uuid, material_name) VALUES (?, ?)";
                    try (PreparedStatement stmt = mysqlConnection.prepareStatement(insertSql)) {
                        for (Material material : materials) {
                            stmt.setString(1, playerUUID.toString());
                            stmt.setString(2, material.name());
                            stmt.executeUpdate();
                            migratedItems++;
                        }
                        migratedPlayers++;
                    }
                }
                
                // Migrate notification settings
                int migratedNotifications = 0;
                for (UUID playerUUID : notificationSettings) {
                    String insertSql = "INSERT IGNORE INTO player_notifications (player_uuid, notifications_disabled) VALUES (?, TRUE)";
                    try (PreparedStatement stmt = mysqlConnection.prepareStatement(insertSql)) {
                        stmt.setString(1, playerUUID.toString());
                        stmt.executeUpdate();
                        migratedNotifications++;
                    }
                }
                
                logger.info("MySQL migration completed successfully!");
                logger.info("Migrated " + migratedPlayers + " players with " + migratedItems + " total items");
                logger.info("Migrated " + migratedNotifications + " notification settings");
                
                // Create backup of SQLite database
                File sqliteFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.filename", "collections.db"));
                if (sqliteFile.exists()) {
                    File backupFile = new File(plugin.getDataFolder(), "collections.db.backup");
                    if (sqliteFile.renameTo(backupFile)) {
                        logger.info("SQLite database backed up to: collections.db.backup");
                    } else {
                        logger.warning("Could not backup SQLite database");
                    }
                }
                
                return true;
                
            } catch (SQLException e) {
                logger.severe("Failed to connect to MySQL database: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Failed to migrate to MySQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean migrateDatabase() {
        String actualCurrentType = detectCurrentDatabaseType();
        String targetType = config.getString("database.type", "sqlite").toLowerCase();
        
        if ("none".equals(actualCurrentType)) {
            logger.warning("No existing database found to migrate from.");
            return false;
        }
        
        if (actualCurrentType.equals(targetType)) {
            logger.warning("Already using " + targetType + " database. No migration needed.");
            return false;
        }
        
        try {
            if ("mysql".equals(targetType)) {
                return migrateToMySQL();
            } else if ("sqlite".equals(targetType)) {
                return migrateToSQLite();
            } else {
                logger.severe("Unknown target database type: " + targetType);
                return false;
            }
        } catch (Exception e) {
            logger.severe("Failed to migrate database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean migrateToMySQL() {
        try {
            logger.info("Starting migration from SQLite to MySQL...");
            
            // Load all data directly from SQLite file
            Map<UUID, Set<Material>> allCollections = loadAllCollectionsFromSQLite();
            Set<UUID> notificationSettings = loadNotificationSettingsFromSQLite();
            
            if (allCollections.isEmpty() && notificationSettings.isEmpty()) {
                logger.info("No data to migrate from SQLite");
                return true;
            }
            
            // Get MySQL settings from config
            String mysqlHost = config.getString("database.mysql.host", "localhost");
            int mysqlPort = config.getInt("database.mysql.port", 3306);
            String mysqlDatabase = config.getString("database.mysql.database", "collectiontracker");
            String mysqlUsername = config.getString("database.mysql.username", "root");
            String mysqlPassword = config.getString("database.mysql.password", "password");
            
            // Create MySQL connection
            String mysqlUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                    mysqlHost, mysqlPort, mysqlDatabase);
            
            try (Connection mysqlConnection = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword)) {
                
                // Create tables in MySQL
                createTablesInConnection(mysqlConnection);
                
                // Migrate collections
                int migratedPlayers = 0;
                int migratedItems = 0;
                
                for (Map.Entry<UUID, Set<Material>> entry : allCollections.entrySet()) {
                    UUID playerUUID = entry.getKey();
                    Set<Material> materials = entry.getValue();
                    
                    // Insert collections into MySQL
                    String insertSql = "INSERT IGNORE INTO player_collections (player_uuid, material_name) VALUES (?, ?)";
                    try (PreparedStatement stmt = mysqlConnection.prepareStatement(insertSql)) {
                        for (Material material : materials) {
                            stmt.setString(1, playerUUID.toString());
                            stmt.setString(2, material.name());
                            stmt.executeUpdate();
                            migratedItems++;
                        }
                        migratedPlayers++;
                    }
                }
                
                // Migrate notification settings
                int migratedNotifications = 0;
                for (UUID playerUUID : notificationSettings) {
                    String insertSql = "INSERT IGNORE INTO player_notifications (player_uuid, notifications_disabled) VALUES (?, TRUE)";
                    try (PreparedStatement stmt = mysqlConnection.prepareStatement(insertSql)) {
                        stmt.setString(1, playerUUID.toString());
                        stmt.executeUpdate();
                        migratedNotifications++;
                    }
                }
                
                logger.info("MySQL migration completed successfully!");
                logger.info("Migrated " + migratedPlayers + " players with " + migratedItems + " total items");
                logger.info("Migrated " + migratedNotifications + " notification settings");
                
                // Create backup of SQLite database
                File sqliteFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.filename", "collections.db"));
                if (sqliteFile.exists()) {
                    File backupFile = new File(plugin.getDataFolder(), "collections.db.backup");
                    if (sqliteFile.renameTo(backupFile)) {
                        logger.info("SQLite database backed up to: collections.db.backup");
                    } else {
                        logger.warning("Could not backup SQLite database");
                    }
                }
                
                return true;
                
            } catch (SQLException e) {
                logger.severe("Failed to connect to MySQL database: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Failed to migrate to MySQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean migrateToSQLite() {
        try {
            logger.info("Starting migration from MySQL to SQLite...");
            
            // Get current MySQL connection details from config
            String mysqlHost = config.getString("database.mysql.host", "localhost");
            int mysqlPort = config.getInt("database.mysql.port", 3306);
            String mysqlDatabase = config.getString("database.mysql.database", "collectiontracker");
            String mysqlUsername = config.getString("database.mysql.username", "root");
            String mysqlPassword = config.getString("database.mysql.password", "password");
            
            // Create MySQL connection to read from
            String mysqlUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                    mysqlHost, mysqlPort, mysqlDatabase);
            
            try (Connection mysqlConnection = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword)) {
                
                // Load all data from MySQL
                Map<UUID, Set<Material>> allCollections = new HashMap<>();
                Set<UUID> notificationSettings = new HashSet<>();
                
                // Load collections
                String collectionsSql = "SELECT player_uuid, material_name FROM player_collections";
                try (Statement stmt = mysqlConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(collectionsSql)) {
                    
                    while (rs.next()) {
                        String uuidString = rs.getString("player_uuid");
                        String materialName = rs.getString("material_name");
                        
                        try {
                            UUID playerUUID = UUID.fromString(uuidString);
                            Material material = Material.valueOf(materialName);
                            // Additional validation: check if material is actually collectible
                            if (material.isItem() && !material.isAir()) {
                                allCollections.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(material);
                            } else {
                                logger.warning("Skipping non-collectible material in MySQL: " + materialName);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid data in MySQL - UUID: " + uuidString + ", Material: " + materialName);
                        }
                    }
                }
                
                // Load notification settings
                String notificationsSql = "SELECT player_uuid FROM player_notifications WHERE notifications_disabled = TRUE";
                try (Statement stmt = mysqlConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(notificationsSql)) {
                    
                    while (rs.next()) {
                        String uuidString = rs.getString("player_uuid");
                        try {
                            notificationSettings.add(UUID.fromString(uuidString));
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid UUID in MySQL notifications: " + uuidString);
                        }
                    }
                }
                
                if (allCollections.isEmpty() && notificationSettings.isEmpty()) {
                    logger.info("No data to migrate from MySQL");
                    return true;
                }
                
                // Save all data to SQLite
                for (Map.Entry<UUID, Set<Material>> entry : allCollections.entrySet()) {
                    savePlayerCollection(entry.getKey(), entry.getValue());
                }
                
                for (UUID playerUUID : notificationSettings) {
                    saveNotificationSettings(playerUUID, true);
                }
                
                logger.info("SQLite migration completed successfully!");
                logger.info("Migrated " + allCollections.size() + " players with " + 
                    allCollections.values().stream().mapToInt(Set::size).sum() + " total items");
                logger.info("Migrated " + notificationSettings.size() + " notification settings");
                
                return true;
                
            } catch (SQLException e) {
                logger.severe("Failed to connect to MySQL database: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Failed to migrate to SQLite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean initializeSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String filename = config.getString("database.sqlite.filename", "collections.db");
        File dbFile = new File(dataFolder, filename);
        
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        
        // Create tables if they don't exist
        createTables();
        logger.info("SQLite database initialized successfully");
        return true;
    }
    
    private boolean initializeMySQL() throws SQLException {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "collectiontracker");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "password");
        
        // Build connection URL with advanced settings if configured
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", host, port, database));
        
        // Add connection pool settings if advanced config exists
        if (config.contains("database.mysql.advanced")) {
            int maxPoolSize = config.getInt("database.mysql.advanced.maximum-pool-size", 10);
            int minIdle = config.getInt("database.mysql.advanced.minimum-idle", 2);
            int connectionTimeout = config.getInt("database.mysql.advanced.connection-timeout", 30000);
            int idleTimeout = config.getInt("database.mysql.advanced.idle-timeout", 600000);
            int maxLifetime = config.getInt("database.mysql.advanced.max-lifetime", 1800000);
            
            urlBuilder.append(String.format("&maximumPoolSize=%d&minimumIdle=%d&connectionTimeout=%d&idleTimeout=%d&maxLifetime=%d",
                    maxPoolSize, minIdle, connectionTimeout, idleTimeout, maxLifetime));
        }
        
        String url = urlBuilder.toString();
        connection = DriverManager.getConnection(url, username, password);
        
        // Create tables if they don't exist
        createTables();
        logger.info("MySQL database initialized successfully");
        return true;
    }
    
    private void createTables() throws SQLException {
        // Check if we need to recreate the tables due to schema changes
        if (needsTableRecreation()) {
            logger.info("Detected schema changes, recreating tables...");
            dropTables();
        }
        
        String createCollectionsTable;
        String createNotificationsTable;
        
        if ("mysql".equals(databaseType)) {
            // MySQL syntax
            createCollectionsTable = """
                CREATE TABLE IF NOT EXISTS player_collections (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    material_name VARCHAR(100) NOT NULL,
                    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_player_material (player_uuid, material_name)
                )
                """;
                
            createNotificationsTable = """
                CREATE TABLE IF NOT EXISTS player_notifications (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    notifications_disabled BOOLEAN DEFAULT FALSE
                )
                """;
        } else {
            // SQLite syntax
            createCollectionsTable = """
                CREATE TABLE IF NOT EXISTS player_collections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    material_name VARCHAR(100) NOT NULL,
                    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(player_uuid, material_name)
                )
                """;
                
            createNotificationsTable = """
                CREATE TABLE IF NOT EXISTS player_notifications (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    notifications_disabled BOOLEAN DEFAULT FALSE
                )
                """;
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCollectionsTable);
            stmt.execute(createNotificationsTable);
        }
    }
    
    private boolean needsTableRecreation() {
        try {
            // Check if the old schema exists (player_uuid as primary key)
            String checkSql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='player_collections'";
            if ("mysql".equals(databaseType)) {
                checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='player_collections'";
            }
            
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                
                if (rs.next() && rs.getInt(1) > 0) {
                    // Table exists, check if it has the old schema
                    String columnCheckSql = "PRAGMA table_info(player_collections)";
                    if ("mysql".equals(databaseType)) {
                        columnCheckSql = "DESCRIBE player_collections";
                    }
                    
                    try (Statement columnStmt = connection.createStatement();
                         ResultSet columnRs = columnStmt.executeQuery(columnCheckSql)) {
                        
                        boolean hasIdColumn = false;
                        boolean hasOldPrimaryKey = false;
                        
                        while (columnRs.next()) {
                            String columnName = columnRs.getString("mysql".equals(databaseType) ? "Field" : "name");
                            String key = "mysql".equals(databaseType) ? columnRs.getString("Key") : columnRs.getString("pk");
                            
                            if ("id".equals(columnName)) {
                                hasIdColumn = true;
                            }
                            if ("player_uuid".equals(columnName) && "1".equals(key)) {
                                hasOldPrimaryKey = true;
                            }
                        }
                        
                        // If we have the old schema (player_uuid as primary key), we need to recreate
                        return hasOldPrimaryKey && !hasIdColumn;
                    }
                }
            }
        } catch (SQLException e) {
            logger.warning("Could not check table schema: " + e.getMessage());
        }
        
        return false;
    }
    
    private void dropTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS player_collections");
            stmt.execute("DROP TABLE IF EXISTS player_notifications");
        }
    }
    
    public void savePlayerCollection(UUID playerUUID, Set<Material> materials) {
        String deleteSql = "DELETE FROM player_collections WHERE player_uuid = ?";
        String insertSql;
        
        // Use database-specific INSERT syntax
        if ("mysql".equals(databaseType)) {
            insertSql = "INSERT IGNORE INTO player_collections (player_uuid, material_name) VALUES (?, ?)";
        } else {
            insertSql = "INSERT OR IGNORE INTO player_collections (player_uuid, material_name) VALUES (?, ?)";
        }
        
        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql);
             PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            
            // Delete existing collections for this player
            deleteStmt.setString(1, playerUUID.toString());
            deleteStmt.executeUpdate();
            
            // Insert new collections (INSERT OR IGNORE/INSERT IGNORE handles duplicates gracefully)
            for (Material material : materials) {
                insertStmt.setString(1, playerUUID.toString());
                insertStmt.setString(2, material.name());
                insertStmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to save player collection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Set<Material> loadPlayerCollection(UUID playerUUID) {
        Set<Material> materials = new HashSet<>();
        String sql = "SELECT material_name FROM player_collections WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String materialName = rs.getString("material_name");
                try {
                    materials.add(Material.valueOf(materialName));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid material name in database: " + materialName);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to load player collection: " + e.getMessage());
            e.printStackTrace();
        }
        
        return materials;
    }
    
    public Map<UUID, Set<Material>> loadAllCollections() {
        Map<UUID, Set<Material>> allCollections = new HashMap<>();
        String sql = "SELECT player_uuid, material_name FROM player_collections";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String uuidString = rs.getString("player_uuid");
                String materialName = rs.getString("material_name");
                
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    Material material = Material.valueOf(materialName);
                    
                    allCollections.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(material);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid data in database - UUID: " + uuidString + ", Material: " + materialName);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to load all collections: " + e.getMessage());
            e.printStackTrace();
        }
        
        return allCollections;
    }
    
    public void saveNotificationSettings(UUID playerUUID, boolean notificationsDisabled) {
        String sql;
        
        // Use database-specific INSERT syntax
        if ("mysql".equals(databaseType)) {
            sql = """
                INSERT INTO player_notifications (player_uuid, notifications_disabled) 
                VALUES (?, ?) 
                ON DUPLICATE KEY UPDATE notifications_disabled = ?
                """;
        } else {
            // SQLite uses INSERT OR REPLACE
            sql = "INSERT OR REPLACE INTO player_notifications (player_uuid, notifications_disabled) VALUES (?, ?)";
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setBoolean(2, notificationsDisabled);
            
            // Only set the third parameter for MySQL
            if ("mysql".equals(databaseType)) {
                stmt.setBoolean(3, notificationsDisabled);
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save notification settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Set<UUID> loadNotificationSettings() {
        Set<UUID> disabledNotifications = new HashSet<>();
        String sql = "SELECT player_uuid FROM player_notifications WHERE notifications_disabled = TRUE";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String uuidString = rs.getString("player_uuid");
                try {
                    disabledNotifications.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid UUID in notification settings: " + uuidString);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to load notification settings: " + e.getMessage());
            e.printStackTrace();
        }
        
        return disabledNotifications;
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.severe("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public String getCurrentDatabaseType() {
        return databaseType;
    }
    
    public boolean reinitialize() {
        try {
            // Close the current connection
            if (connection != null) {
                connection.close();
                connection = null;
            }
            
            // Reinitialize with the current config
            return initialize();
        } catch (Exception e) {
            logger.severe("Failed to reinitialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private Map<UUID, Set<Material>> loadAllCollectionsFromSQLite() {
        Map<UUID, Set<Material>> allCollections = new HashMap<>();
        
        try {
            // Create a direct connection to the SQLite file
            File sqliteFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.filename", "collections.db"));
            if (!sqliteFile.exists()) {
                logger.warning("SQLite database file not found: " + sqliteFile.getAbsolutePath());
                return allCollections;
            }
            
            String sqliteUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
            try (Connection sqliteConnection = DriverManager.getConnection(sqliteUrl)) {
                String sql = "SELECT player_uuid, material_name FROM player_collections";
                
                try (Statement stmt = sqliteConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        String uuidString = rs.getString("player_uuid");
                        String materialName = rs.getString("material_name");
                        
                        try {
                            UUID playerUUID = UUID.fromString(uuidString);
                            Material material = Material.valueOf(materialName);
                            // Additional validation: check if material is actually collectible
                            if (material.isItem() && !material.isAir()) {
                                allCollections.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(material);
                            } else {
                                logger.warning("Skipping non-collectible material in SQLite: " + materialName);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid data in SQLite - UUID: " + uuidString + ", Material: " + materialName);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to load collections from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
        
        return allCollections;
    }
    
    private Set<UUID> loadNotificationSettingsFromSQLite() {
        Set<UUID> disabledNotifications = new HashSet<>();
        
        try {
            // Create a direct connection to the SQLite file
            File sqliteFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.filename", "collections.db"));
            if (!sqliteFile.exists()) {
                logger.warning("SQLite database file not found: " + sqliteFile.getAbsolutePath());
                return disabledNotifications;
            }
            
            String sqliteUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
            try (Connection sqliteConnection = DriverManager.getConnection(sqliteUrl)) {
                String sql = "SELECT player_uuid FROM player_notifications WHERE notifications_disabled = TRUE";
                
                try (Statement stmt = sqliteConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        String uuidString = rs.getString("player_uuid");
                        try {
                            disabledNotifications.add(UUID.fromString(uuidString));
                        } catch (IllegalArgumentException e) {
                            logger.warning("Invalid UUID in SQLite notifications: " + uuidString);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Failed to load notification settings from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
        
        return disabledNotifications;
    }
    
    public String detectCurrentDatabaseType() {
        // Check if we have a SQLite database file with data
        File sqliteFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.filename", "collections.db"));
        if (sqliteFile.exists()) {
            // Check if SQLite actually has data
            try {
                String sqliteUrl = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
                try (Connection sqliteConnection = DriverManager.getConnection(sqliteUrl)) {
                    try (Statement stmt = sqliteConnection.createStatement()) {
                        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_collections");
                        if (rs.next() && rs.getInt(1) > 0) {
                            return "sqlite";
                        }
                    }
                }
            } catch (SQLException e) {
                // SQLite file exists but has no data or is corrupted
                logger.warning("SQLite file exists but has no data or is corrupted: " + e.getMessage());
            }
        }
        
        // Check if we can connect to MySQL with current config and it has data
        try {
            String mysqlHost = config.getString("database.mysql.host", "localhost");
            int mysqlPort = config.getInt("database.mysql.port", 3306);
            String mysqlDatabase = config.getString("database.mysql.database", "collectiontracker");
            String mysqlUsername = config.getString("database.mysql.username", "root");
            String mysqlPassword = config.getString("database.mysql.password", "password");
            
            String mysqlUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                    mysqlHost, mysqlPort, mysqlDatabase);
            
            try (Connection testConnection = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword)) {
                // Check if our tables exist in MySQL and have data
                try (Statement stmt = testConnection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_collections");
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "mysql";
                    }
                } catch (SQLException e) {
                    // Tables don't exist, so MySQL is not being used yet
                    return "none";
                }
            }
        } catch (SQLException e) {
            // Can't connect to MySQL, so it's not being used
            return "none";
        }
        
        return "none";
    }
    
    private void createTablesInConnection(Connection connection) throws SQLException {
        String createCollectionsTable = """
            CREATE TABLE IF NOT EXISTS player_collections (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                material_name VARCHAR(100) NOT NULL,
                collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_player_material (player_uuid, material_name)
            )
            """;
            
        String createNotificationsTable = """
            CREATE TABLE IF NOT EXISTS player_notifications (
                player_uuid VARCHAR(36) PRIMARY KEY,
                notifications_disabled BOOLEAN DEFAULT FALSE
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCollectionsTable);
            stmt.execute(createNotificationsTable);
        }
    }
    
    private boolean validateDatabaseSchema(Connection connection) {
        try {
            // Check if required tables exist
            try (Statement stmt = connection.createStatement()) {
                // Test player_collections table
                stmt.executeQuery("SELECT COUNT(*) FROM player_collections LIMIT 1");
                
                // Test player_notifications table
                stmt.executeQuery("SELECT COUNT(*) FROM player_notifications LIMIT 1");
                
                logger.info("Database schema validation passed");
                return true;
            }
        } catch (SQLException e) {
            logger.warning("Database schema validation failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateMaterialName(String materialName) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return false;
        }
        
        try {
            Material material = Material.valueOf(materialName);
            return material.isItem() && !material.isAir();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private boolean validateUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return false;
        }
        
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 