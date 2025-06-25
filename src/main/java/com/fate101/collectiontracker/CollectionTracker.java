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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CollectionTracker extends JavaPlugin implements Listener {
    private Map<UUID, Set<Material>> playerCollections;
    private File collectionsFile;
    private FileConfiguration collectionsConfig;
    private static final String GUI_TITLE = "Collection Tracker";
    private static final int GUI_SIZE = 54; // 6 rows of inventory
    private Map<UUID, Integer> playerPages;
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

        if (!event.getView().getTitle().startsWith(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int currentPage = playerPages.get(player.getUniqueId());

        // Handle navigation buttons
        if (event.getCurrentItem() != null) {
            if (event.getSlot() == 45 && currentPage > 0) { // Previous page
                openCollectionGUI(player, currentPage - 1);
            } else if (event.getSlot() == 53 && (currentPage + 1) * 45 < collectibleItems.size()) { // Next page
                openCollectionGUI(player, currentPage + 1);
            }
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
}