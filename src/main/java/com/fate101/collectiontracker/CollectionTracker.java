package com.fate101.collectiontracker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CollectionTracker extends JavaPlugin implements Listener {
    private Map<UUID, Set<Material>> playerCollections;
    private File collectionsFile;
    private FileConfiguration collectionsConfig;
    private static final String GUI_TITLE = "Collection Tracker";
    private static final String LEADERBOARD_TITLE = "Collection Leaderboard";
    private static final int GUI_SIZE = 54; // 6 rows of inventory
    private static final int LEADERBOARD_SIZE = 54; // 6 rows of inventory
    private Map<UUID, Integer> playerPages;
    private Map<UUID, Integer> leaderboardPages;
    private List<Material> collectibleItems;

    private boolean isCreativeOnlyItem(Material material) {
        // List of creative-only items and patterns
        String name = material.name().toLowerCase();

        return name.contains("spawn_egg") ||           // Spawn eggs
                name.contains("command") ||             // Command blocks and related items
                name.startsWith("debug_") ||            // Debug items
                name.contains("barrier") ||             // Barrier blocks
                name.contains("structure") ||           // Structure blocks and related
                name.equals("light") ||                 // Light blocks
                name.contains("jigsaw") ||              // Jigsaw blocks
                material == Material.KNOWLEDGE_BOOK ||   // Knowledge book
                name.contains("command_block") ||       // Command blocks (explicit check)
                name.contains("reinforced_deepslate") || // Reinforced deepslate
                name.contains("trial_spawner") ||       // Trial spawner (1.21+)
                name.contains("vault") ||               // Vault blocks (1.21+)
                // Creative-only armor trims
                name.contains("flow_armor_trim") ||     // Flow armor trim
                name.contains("spire_armor_trim") ||    // Spire armor trim
                name.contains("wayfinder_armor_trim") || // Wayfinder armor trim
                name.contains("raiser_armor_trim") ||   // Raiser armor trim
                name.contains("shaper_armor_trim") ||   // Shaper armor trim
                name.contains("host_armor_trim") ||     // Host armor trim
                name.contains("warden_armor_trim") ||   // Warden armor trim
                name.contains("silence_armor_trim") ||  // Silence armor trim
                name.contains("tide_armor_trim") ||     // Tide armor trim
                name.contains("snout_armor_trim") ||    // Snout armor trim
                name.contains("rib_armor_trim") ||      // Rib armor trim
                name.contains("eye_armor_trim") ||      // Eye armor trim
                name.contains("dune_armor_trim") ||     // Dune armor trim
                name.contains("coast_armor_trim") ||    // Coast armor trim
                name.contains("wild_armor_trim") ||     // Wild armor trim
                name.contains("ward_armor_trim") ||     // Ward armor trim
                name.contains("sentry_armor_trim") ||   // Sentry armor trim
                name.contains("vex_armor_trim");        // Vex armor trim
    }

    @Override
    public void onEnable() {
        playerCollections = new HashMap<>();
        playerPages = new HashMap<>();
        leaderboardPages = new HashMap<>();

        // Initialize collectible items list with filtering
        collectibleItems = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir() && !isCreativeOnlyItem(material)) {
                // Additional checks for obtainable items
                if (material.isBlock()) {
                    // Only add blocks that should be collectible
                    if (!material.name().contains("INFESTED") &&    // Remove infested blocks
                            !material.name().contains("PORTAL") &&      // Remove portal blocks
                            !material.name().equals("BEDROCK") &&       // Remove bedrock
                            !material.name().contains("END_PORTAL") &&  // Remove end portal frames
                            !material.name().contains("CHORUS_FLOWER")) { // Remove chorus flower (technical block)
                        collectibleItems.add(material);
                    }
                } else {
                    // Add non-block items by default unless they're in creative-only list
                    collectibleItems.add(material);
                }
            }
        }

        // Sort the items alphabetically
        Collections.sort(collectibleItems, (a, b) ->
                formatMaterialName(a.name()).compareTo(formatMaterialName(b.name())));

        // Log the total number of collectible items
        getLogger().info("CollectionTracker initialized with " + collectibleItems.size() + " collectible items");

        // Create config file
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        collectionsFile = new File(getDataFolder(), "collections.yml");
        if (!collectionsFile.exists()) {
            try {
                collectionsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create collections file!");
            }
        }
        collectionsConfig = YamlConfiguration.loadConfiguration(collectionsFile);

        loadCollections();

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("collection").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                playerPages.putIfAbsent(player.getUniqueId(), 0);
                openCollectionGUI(player, playerPages.get(player.getUniqueId()));
                return true;
            }
            return false;
        });

        getCommand("coltop").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                openLeaderboardGUI(player, 0);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDisable() {
        saveCollections();
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        Material material = event.getItem().getItemStack().getType();

        Set<Material> collection = playerCollections.computeIfAbsent(
                player.getUniqueId(),
                k -> new HashSet<>()
        );

        if (!collection.contains(material)) {
            collection.add(material);
            player.sendMessage("§a✔ New item collected: " + material.name());
            saveCollections();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE) && !title.startsWith(LEADERBOARD_TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        // Handle collection GUI navigation
        if (title.startsWith(GUI_TITLE)) {
            int currentPage = playerPages.get(player.getUniqueId());
            if (event.getCurrentItem() != null) {
                if (event.getSlot() == 45 && currentPage > 0) { // Previous page
                    openCollectionGUI(player, currentPage - 1);
                } else if (event.getSlot() == 53 && (currentPage + 1) * 45 < collectibleItems.size()) { // Next page
                    openCollectionGUI(player, currentPage + 1);
                }
            }
        }
        // Leaderboard GUI - no navigation needed (single page)
        else if (title.startsWith(LEADERBOARD_TITLE)) {
            // No navigation for leaderboard - just cancel the event
        }
    }

    @EventHandler
    public void onAnyInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        String title = event.getView().getTitle();
        // Ignore plugin GUIs
        if (title.startsWith(GUI_TITLE) || title.startsWith(LEADERBOARD_TITLE)) {
            return;
        }
        // Only care about items being added to the player's inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || isCreativeOnlyItem(clicked.getType())) {
            return;
        }
        Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!collection.contains(clicked.getType()) && collectibleItems.contains(clicked.getType())) {
            collection.add(clicked.getType());
            player.sendMessage("§a✔ New item collected: " + clicked.getType().name());
            saveCollections();
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result == null || result.getType().isAir() || isCreativeOnlyItem(result.getType())) return;
        Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!collection.contains(result.getType()) && collectibleItems.contains(result.getType())) {
            collection.add(result.getType());
            player.sendMessage("§a✔ New item collected: " + result.getType().name());
            saveCollections();
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        Material type = event.getItemType();
        if (type.isAir() || isCreativeOnlyItem(type)) return;
        Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!collection.contains(type) && collectibleItems.contains(type)) {
            collection.add(type);
            player.sendMessage("§a✔ New item collected: " + type.name());
            saveCollections();
        }
    }

    @EventHandler
    public void onSpecialInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        String title = event.getView().getTitle();
        if (title.startsWith(GUI_TITLE) || title.startsWith(LEADERBOARD_TITLE)) return;
        // Merchant (trading)
        if (event.getInventory() instanceof MerchantInventory && event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir() && !isCreativeOnlyItem(item.getType())) {
                Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                if (!collection.contains(item.getType()) && collectibleItems.contains(item.getType())) {
                    collection.add(item.getType());
                    player.sendMessage("§a✔ New item collected: " + item.getType().name());
                    saveCollections();
                }
            }
        }
        // Brewing stand
        if (event.getInventory() instanceof BrewerInventory && event.getSlotType() == InventoryType.SlotType.RESULT) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir() && !isCreativeOnlyItem(item.getType())) {
                Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
                if (!collection.contains(item.getType()) && collectibleItems.contains(item.getType())) {
                    collection.add(item.getType());
                    player.sendMessage("§a✔ New item collected: " + item.getType().name());
                    saveCollections();
                }
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        ItemStack filled = event.getItemStack();
        if (filled == null || filled.getType().isAir() || isCreativeOnlyItem(filled.getType())) return;
        Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!collection.contains(filled.getType()) && collectibleItems.contains(filled.getType())) {
            collection.add(filled.getType());
            player.sendMessage("§a✔ New item collected: " + filled.getType().name());
            saveCollections();
        }
    }

    @EventHandler
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        // Only handle milking (cow, goat, camel, etc.)
        Material type = Material.MILK_BUCKET;
        if (isCreativeOnlyItem(type)) return;
        Set<Material> collection = playerCollections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!collection.contains(type) && collectibleItems.contains(type)) {
            collection.add(type);
            player.sendMessage("§a✔ New item collected: " + type.name());
            saveCollections();
        }
    }

    private void openCollectionGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " - Page " + (page + 1));
        Set<Material> collection = playerCollections.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Calculate start and end index for current page
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, collectibleItems.size());

        // Add items for current page
        for (int i = startIndex; i < endIndex; i++) {
            Material material = collectibleItems.get(i);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // Set item name and lore
                meta.setDisplayName("§f" + formatMaterialName(material.name()));
                List<String> lore = new ArrayList<>();

                if (collection.contains(material)) {
                    lore.add("§aCollected!");
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add("§cNot collected");
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(i - startIndex, item);
        }

        // Add navigation buttons
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§ePrevious Page");
            prevPage.setItemMeta(prevMeta);
            gui.setItem(45, prevPage);
        }

        if ((page + 1) * 45 < collectibleItems.size()) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§eNext Page");
            nextPage.setItemMeta(nextMeta);
            gui.setItem(53, nextPage);
        }

        // Add collection statistics
        ItemStack stats = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.setDisplayName("§6Collection Statistics");
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7Items Collected: §e" + collection.size());
        statsLore.add("§7Total Items: §e" + collectibleItems.size());
        statsLore.add("§7Completion: §e" +
                String.format("%.1f%%", (collection.size() * 100.0) / collectibleItems.size()));
        statsMeta.setLore(statsLore);
        stats.setItemMeta(statsMeta);
        gui.setItem(49, stats);

        player.openInventory(gui);
    }

    private String formatMaterialName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private void saveCollections() {
        for (Map.Entry<UUID, Set<Material>> entry : playerCollections.entrySet()) {
            List<String> materials = new ArrayList<>();
            for (Material material : entry.getValue()) {
                materials.add(material.name());
            }
            collectionsConfig.set(entry.getKey().toString(), materials);
        }

        try {
            collectionsConfig.save(collectionsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save collections!");
        }
    }

    private void loadCollections() {
        for (String uuidString : collectionsConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            List<String> materialNames = collectionsConfig.getStringList(uuidString);
            Set<Material> materials = new HashSet<>();

            for (String name : materialNames) {
                try {
                    materials.add(Material.valueOf(name));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid material name: " + name);
                }
            }

            playerCollections.put(uuid, materials);
        }
    }

    // Leaderboard methods
    private List<LeaderboardEntry> getLeaderboardEntries() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        for (Map.Entry<UUID, Set<Material>> entry : playerCollections.entrySet()) {
            UUID playerUUID = entry.getKey();
            Set<Material> collection = entry.getValue();
            
            // Get player name
            String playerName = "Unknown Player";
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                playerName = player.getName();
            } else {
                // Try to get offline player name
                try {
                    playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
                    if (playerName == null) {
                        playerName = "Unknown Player";
                    }
                } catch (Exception e) {
                    playerName = "Unknown Player";
                }
            }
            
            double completionPercentage = (collection.size() * 100.0) / collectibleItems.size();
            entries.add(new LeaderboardEntry(playerUUID, playerName, collection.size(), completionPercentage));
        }
        
        // Sort by completion percentage (highest first), then by items collected (highest first)
        entries.sort((a, b) -> {
            int completionCompare = Double.compare(b.completionPercentage, a.completionPercentage);
            if (completionCompare != 0) {
                return completionCompare;
            }
            return Integer.compare(b.itemsCollected, a.itemsCollected);
        });
        
        return entries;
    }

    private void openLeaderboardGUI(Player player, int page) {
        leaderboardPages.put(player.getUniqueId(), page);
        
        List<LeaderboardEntry> entries = getLeaderboardEntries();
        Inventory gui = Bukkit.createInventory(null, LEADERBOARD_SIZE, LEADERBOARD_TITLE);
        
        // Find the viewing player's entry
        LeaderboardEntry viewingPlayerEntry = null;
        for (LeaderboardEntry entry : entries) {
            if (entry.playerUUID.equals(player.getUniqueId())) {
                viewingPlayerEntry = entry;
                break;
            }
        }
        
        // Show top 44 entries (leaving space for viewing player if needed)
        int entriesToShow = Math.min(44, entries.size());
        
        // Add leaderboard entries
        for (int i = 0; i < entriesToShow; i++) {
            LeaderboardEntry entry = entries.get(i);
            int rank = i + 1;
            
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            
            if (meta != null) {
                // Set the player head to the correct player skin
                try {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.playerUUID));
                } catch (Exception e) {
                    getLogger().warning("Could not set player head for " + entry.playerName);
                }
                // Set rank and player info
                String rankColor = getRankColor(rank);
                String displayName = rankColor + "#" + rank + " " + entry.playerName;
                // Highlight the viewing player
                if (entry.playerUUID.equals(player.getUniqueId())) {
                    displayName = "§b§lYOU - " + displayName;
                }
                meta.setDisplayName(displayName);
                List<String> lore = new ArrayList<>();
                lore.add("§7Items Collected: §e" + entry.itemsCollected + "/" + collectibleItems.size());
                lore.add("§7Completion: §e" + String.format("%.1f%%", entry.completionPercentage));
                // Add special indicators for top 3
                if (rank == 1) {
                    lore.add("§6🥇 First Place!");
                } else if (rank == 2) {
                    lore.add("§7🥈 Second Place!");
                } else if (rank == 3) {
                    lore.add("§c🥉 Third Place!");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i, item);
        }
        // Add viewing player in bottom right if they're not already shown
        if (viewingPlayerEntry != null) {
            boolean playerAlreadyShown = false;
            for (int i = 0; i < entriesToShow; i++) {
                if (entries.get(i).playerUUID.equals(player.getUniqueId())) {
                    playerAlreadyShown = true;
                    break;
                }
            }
            if (!playerAlreadyShown) {
                // Find the player's actual rank
                int actualRank = 0;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).playerUUID.equals(player.getUniqueId())) {
                        actualRank = i + 1;
                        break;
                    }
                }
                ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    try {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(viewingPlayerEntry.playerUUID));
                    } catch (Exception e) {
                        getLogger().warning("Could not set player head for " + viewingPlayerEntry.playerName);
                    }
                    String rankColor = getRankColor(actualRank);
                    meta.setDisplayName("§b§lYOU - " + rankColor + "#" + actualRank + " " + viewingPlayerEntry.playerName);
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Items Collected: §e" + viewingPlayerEntry.itemsCollected + "/" + collectibleItems.size());
                    lore.add("§7Completion: §e" + String.format("%.1f%%", viewingPlayerEntry.completionPercentage));
                    lore.add("§b§lYour Position");
                    meta.setLore(lore);
                }
                gui.setItem(53, item); // Bottom right slot
            }
        }
        
        // Add leaderboard statistics
        ItemStack stats = new ItemStack(Material.GOLD_INGOT);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.setDisplayName("§6Leaderboard Statistics");
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7Total Participants: §e" + entries.size());
        if (!entries.isEmpty()) {
            statsLore.add("§7Top Completion: §e" + String.format("%.1f%%", entries.get(0).completionPercentage));
            statsLore.add("§7Average Completion: §e" + String.format("%.1f%%", 
                entries.stream().mapToDouble(e -> e.completionPercentage).average().orElse(0.0)));
        }
        statsMeta.setLore(statsLore);
        stats.setItemMeta(statsMeta);
        gui.setItem(49, stats);
        
        player.openInventory(gui);
    }
    
    private String getRankColor(int rank) {
        if (rank == 1) return "§6"; // Gold
        if (rank == 2) return "§7"; // Silver
        if (rank == 3) return "§c"; // Bronze
        if (rank <= 10) return "§a"; // Green
        if (rank <= 25) return "§e"; // Yellow
        return "§f"; // White
    }
    
    // Helper class for leaderboard entries
    private static class LeaderboardEntry {
        final UUID playerUUID;
        final String playerName;
        final int itemsCollected;
        final double completionPercentage;
        
        LeaderboardEntry(UUID playerUUID, String playerName, int itemsCollected, double completionPercentage) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.itemsCollected = itemsCollected;
            this.completionPercentage = completionPercentage;
        }
    }
}