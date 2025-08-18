package dev.paramountdev.paramountNpc_PDev.npc.quests;

import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class QuestManager implements Listener {

    private final Map<String, Quest> allQuests = new HashMap<>();
    private final Map<UUID, Map<String, Long>> activeQuests = new HashMap<>();
    private final Map<UUID, Set<String>> completedQuests = new HashMap<>();

    private final Logger logger = Bukkit.getLogger();
    private FileConfiguration questConfig;

    public QuestManager(File dataFolder) {
        loadQuests(dataFolder);
    }

    public void loadQuests(File dataFolder) {

        File file = new File(dataFolder, "quests.yml");
        if (!file.exists()) {
            logger.warning("Файл quests.yml не найден!");
        }
        questConfig = YamlConfiguration.loadConfiguration(file);

        if (!questConfig.isConfigurationSection("quests")) {
            Bukkit.broadcastMessage(ChatColor.RED + "Quests file not found!");
            return;
        }

        for (String id : questConfig.getConfigurationSection("quests").getKeys(false)) {
            String base = "quests." + id;

            String name = questConfig.getString(base + ".name", id);
            String description = questConfig.getString(base + ".description", "Нет описания");
            String npcId = questConfig.getString(base + ".npc_id", "default");

            List<Requirement> requirements = new ArrayList<>();
            if (questConfig.isConfigurationSection(base + ".required_items")) {
                for (String mat : questConfig.getConfigurationSection(base + ".required_items").getKeys(false)) {
                    int amount = questConfig.getInt(base + ".required_items." + mat, 0);
                    Material material = Material.matchMaterial(mat.toUpperCase());
                    if (material != null) {
                        requirements.add(new ItemRequirement(material, amount));
                    } else {
                        logger.warning("Неизвестный материал: " + mat + " в квесте " + id);
                    }
                }
            }
            if (questConfig.isConfigurationSection(base + ".required_location")) {
                String worldName = questConfig.getString(base + ".required_location.world", "world");
                double x = questConfig.getDouble(base + ".required_location.x", 0);
                double y = questConfig.getDouble(base + ".required_location.y", 0);
                double z = questConfig.getDouble(base + ".required_location.z", 0);
                double radius = questConfig.getDouble(base + ".required_location.radius", 5);

                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                requirements.add(new LocationRequirement(loc, radius));
            }


            List<Reward> rewards = new ArrayList<>();
            if (questConfig.isConfigurationSection(base + ".rewards")) {
                for (String type : questConfig.getConfigurationSection(base + ".rewards").getKeys(false)) {
                    int value = questConfig.getInt(base + ".rewards." + type, 0);
                    Reward reward = RewardRegistry.create(type.toUpperCase(), value);
                    if (reward != null) {
                        rewards.add(reward);
                    } else {
                        logger.warning("Неизвестный тип награды: " + type + " в квесте " + id);
                    }
                }
            }

            List<String> dependencies = questConfig.getStringList(base + ".dependencies");
            int cooldown = questConfig.getInt(base + ".cooldown", 0);

            Bukkit.getLogger().info("");
            Bukkit.getLogger().info(
                    "\n"
                    +  "Загружен квест: \n"
                    + "ID квеста: " + id + "\n"
                    + "Название квеста: " + name + "\n"
                    + "Описание квеста: " + description + "\n"
                    + "Квест присвоен к НПС: " + npcId + "\n"
                    + "\n"
            );

            allQuests.put(id, new Quest(id, name, description, npcId, requirements, rewards, dependencies, cooldown));
        }
    }

    public void openQuestMenu(Player player, String npcId) {

        Inventory gui;
        String title;

        try {
            int npcIdInt = Integer.parseInt(npcId);
            Optional<String> npcNameOpt = ParamountNpc_PDev.getInstance().getNpcManager().getNpcNameById(npcIdInt);

            if (npcNameOpt.isPresent()) {
                title = ChatColor.DARK_PURPLE + "Квесты от " + npcNameOpt.get();
            } else {
                title = ChatColor.DARK_PURPLE + "Квесты от NPC #" + npcId;
            }
        } catch (NumberFormatException e) {
            title = ChatColor.DARK_PURPLE + "Квесты от NPC #" + npcId;
        }

        gui = Bukkit.createInventory(null, 54, title);

        for (Quest quest : allQuests.values()) {

            if (!quest.getNpcId().equals(npcId)) {
                continue;
            }

            boolean completed = hasCompleted(player, quest.getId());
            boolean active = isActive(player, quest.getId());


            ItemStack icon = new ItemStack(Material.BOOK);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + quest.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + quest.getDescription());

            quest.getRequirements().forEach(req -> lore.add(ChatColor.AQUA + "Нужно: " + req.getDescription()));

            if (!quest.getDependencies().isEmpty()) {
                lore.add(ChatColor.RED + "Требует завершения: " + String.join(", ", quest.getDependencies()));
            }

            if (active) lore.add(ChatColor.RED + "Уже активен!");
            else if (completed) {
                lore.add(ChatColor.GRAY + "Уже выполнен");
            }

            meta.setLore(lore);
            icon.setItemMeta(meta);
            if(!completed) {
                gui.addItem(icon);
            } else {
                gui.removeItem(icon);
            }

        }

        player.openInventory(gui);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Квесты от")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String name = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        Optional<Quest> questOpt = allQuests.values().stream()
                .filter(q -> q.getName().equalsIgnoreCase(name)).findFirst();

        if (questOpt.isEmpty()) return;

        Quest quest = questOpt.get();

        if (isActive(player, quest.getId())) {
            if (quest.canBeCompletedBy(player)) {
                quest.complete(player);
                completedQuests.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(quest.getId());
                activeQuests.getOrDefault(player.getUniqueId(), new HashMap<>()).remove(quest.getId());
                player.sendMessage(ChatColor.GREEN + "Квест '" + quest.getName() + "' выполнен!");
                player.closeInventory();
            }         boolean hasLocationReq = quest.getRequirements().stream()
                    .anyMatch(req -> req instanceof LocationRequirement);
            if (hasLocationReq) {
                player.sendMessage(ChatColor.YELLOW + "Пройдите на указанные координаты!");
            } else {
                player.closeInventory();
            }
    } else if (!canAccept(player, quest)) {
            player.sendMessage(ChatColor.RED + "Нельзя взять этот квест!");
        } else {
            activeQuests.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                    .put(quest.getId(), System.currentTimeMillis() + quest.getCooldown() * 1000L);
            player.sendMessage(ChatColor.AQUA + "Вы взяли квест: " + quest.getName());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Map<String, Long> playerQuests = activeQuests.getOrDefault(player.getUniqueId(), Collections.emptyMap());

        for (String questId : playerQuests.keySet()) {
            Quest quest = allQuests.get(questId);

            // Если квест содержит LocationRequirement
            boolean hasLocationReq = quest.getRequirements().stream()
                    .anyMatch(req -> req instanceof LocationRequirement);

            if (!hasLocationReq) continue;

            if (quest.canBeCompletedBy(player)) {
                quest.complete(player);
                completedQuests.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(questId);
                activeQuests.getOrDefault(player.getUniqueId(), new HashMap<>()).remove(questId);
                player.sendMessage(ChatColor.GREEN + "Квест '" + quest.getName() + "' выполнен!");
            }
        }
    }


    private boolean canAccept(Player player, Quest quest) {
        long now = System.currentTimeMillis();

        Map<String, Long> playerQuests = activeQuests.getOrDefault(player.getUniqueId(), new HashMap<>());

        // Максимум 3 активных
        if (playerQuests.values().stream().filter(time -> time > now).count() >= 3) return false;

        // Проверка зависимостей
        for (String dep : quest.getDependencies()) {
            if (!hasCompleted(player, dep)) return false;
        }

        // Только один от NPC
        for (String id : playerQuests.keySet()) {
            Quest q = allQuests.get(id);
            if (q != null && q.getNpcId().equalsIgnoreCase(quest.getNpcId())) return false;
        }

        return true;
    }

    private boolean isActive(Player player, String questId) {
        return activeQuests.getOrDefault(player.getUniqueId(), Collections.emptyMap()).containsKey(questId);
    }

    private boolean hasCompleted(Player player, String questId) {
        return completedQuests.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(questId);
    }

    public Quest getQuestById(String id) {
        return allQuests.get(id);
    }

    public Set<String> getCompletedQuestIds(Player player) {
        return completedQuests.getOrDefault(player.getUniqueId(), new HashSet<>());
    }


}

