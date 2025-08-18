package dev.paramountdev.paramountNpc_PDev.npc.trades;

import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
import dev.paramountdev.paramountNpc_PDev.Utils;
import dev.paramountdev.paramountNpc_PDev.npc.vault.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ... все импорты остаются такими же
import org.bukkit.enchantments.Enchantment;

public class TradeManager implements Listener {
    private ParamountNpc_PDev main = ParamountNpc_PDev.getInstance();

    public void openTradeMenu(Player player, String npcId) {
        File file = new File(main.getDataFolder(), "npc_trades.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String base = "npc_trades." + npcId + ".trades";
        if (!config.isConfigurationSection(base)) {
            player.sendMessage("§cНет товаров для NPC ID " + npcId);
            return;
        }

        Inventory gui;
        String title;

        try {
            int npcIdInt = Integer.parseInt(npcId);
            Optional<String> npcNameOpt = main.getNpcManager().getNpcNameById(npcIdInt);
            title = ChatColor.DARK_GREEN + "Торговля с " + npcNameOpt.orElse("NPC #" + npcId);
        } catch (NumberFormatException e) {
            title = ChatColor.DARK_GREEN + "Торговля с NPC #" + npcId;
        }

        gui = Bukkit.createInventory(null, 54, title);

        for (String key : config.getConfigurationSection(base).getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(base + "." + key);
            if (section == null) continue;

            Material mat = Material.matchMaterial(section.getString("item.type"));
            if (mat == null) continue;

            int amount = section.getInt("item.amount", 1);
            ItemStack item = new ItemStack(mat, amount);
            ItemMeta meta = item.getItemMeta();

            if (section.contains("item.name")) meta.setDisplayName(section.getString("item.name"));
            if (section.contains("item.lore")) meta.setLore(section.getStringList("item.lore"));
            if (section.contains("item.custom_model_data")) meta.setCustomModelData(section.getInt("item.custom_model_data"));

            // Зачарования
            if (section.contains("item.enchantments")) {
                ConfigurationSection enchants = section.getConfigurationSection("item.enchantments");
                for (String enchName : enchants.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByName(enchName.toUpperCase());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, enchants.getInt(enchName), true);
                    }
                }
            }

            item.setItemMeta(meta);

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // Цена в деньгах
            if (section.contains("cost.money")) {
                lore.add(ChatColor.GOLD + "Цена: " + section.getInt("cost.money") + "$");
            }

            // Цена в предметах
            if (section.contains("cost.items")) {
                lore.add(ChatColor.YELLOW + "Цена в предметах:");
                ConfigurationSection items = section.getConfigurationSection("cost.items");
                for (String itemName : items.getKeys(false)) {
                    String costPath = itemName;
                    int amountCost = items.getInt(itemName + ".amount", 1);
                    lore.add(" - " + itemName + " x" + amountCost);
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.addItem(item);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onTradeMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Торговля с")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        File file = new File(main.getDataFolder(), "npc_trades.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String npcId = main.getNpcManager().getLastNpcId(player);
        if (npcId == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить NPC.");
            return;
        }

        String base = "npc_trades." + npcId + ".trades";

        for (String key : config.getConfigurationSection(base).getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(base + "." + key);
            if (section == null) continue;

            String name = section.getString("item.name");
            if (name == null || !ChatColor.stripColor(name).equals(ChatColor.stripColor(clicked.getItemMeta().getDisplayName())))
                continue;

            // Проверка денег
            if (section.contains("cost.money")) {
                int price = section.getInt("cost.money");
                if (!VaultHook.has(player, price)) {
                    player.sendMessage(ChatColor.RED + "Недостаточно денег!");
                    return;
                }
                VaultHook.withdraw(player, price);
            }

            // Проверка предметов
            // Проверка предметов
            if (section.contains("cost.items")) {
                ConfigurationSection items = section.getConfigurationSection("cost.items");
                for (String itemName : items.getKeys(false)) {
                    ConfigurationSection itemSection = items.getConfigurationSection(itemName);
                    int amount = itemSection.getInt("amount", 1);
                    int cmd = itemSection.getInt("custom_model_data", -1);
                    String costName = itemSection.getString("name");

                    Material mat = Material.matchMaterial(itemName);
                    if (mat == null) continue;

                    int matched = 0;
                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem == null || invItem.getType() != mat) continue;

                        ItemMeta meta = invItem.getItemMeta();
                        if (cmd != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != cmd)) continue;
                        if (costName != null && (!meta.hasDisplayName() || !ChatColor.stripColor(meta.getDisplayName()).equals(ChatColor.stripColor(costName)))) continue;

                        matched += invItem.getAmount();
                    }

                    if (matched < amount) {
                        player.sendMessage(ChatColor.RED + "Недостаточно " + itemName + (cmd != -1 ? " (CMD " + cmd + ")" : ""));
                        return;
                    }
                }

                // Удаление предметов
                for (String itemName : items.getKeys(false)) {
                    ConfigurationSection itemSection = items.getConfigurationSection(itemName);
                    int amount = itemSection.getInt("amount", 1);
                    int cmd = itemSection.getInt("custom_model_data", -1);
                    String costName = itemSection.getString("name");

                    Material mat = Material.matchMaterial(itemName);
                    int toRemove = amount;

                    for (ItemStack invItem : player.getInventory().getContents()) {
                        if (invItem == null || invItem.getType() != mat) continue;

                        ItemMeta meta = invItem.getItemMeta();
                        if (cmd != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != cmd)) continue;
                        if (costName != null && (!meta.hasDisplayName() || !ChatColor.stripColor(meta.getDisplayName()).equals(ChatColor.stripColor(costName)))) continue;

                        int removeAmount = Math.min(toRemove, invItem.getAmount());
                        invItem.setAmount(invItem.getAmount() - removeAmount);
                        toRemove -= removeAmount;
                        if (toRemove <= 0) break;
                    }
                }
            }

            // Выдача предмета
            Material mat = Material.matchMaterial(section.getString("item.type"));
            if (mat == null) continue;

            int amount = section.getInt("item.amount", 1);
            ItemStack reward = new ItemStack(mat, amount);
            ItemMeta meta = reward.getItemMeta();
            if (section.contains("item.name")) meta.setDisplayName(section.getString("item.name"));
            if (section.contains("item.custom_model_data")) meta.setCustomModelData(section.getInt("item.custom_model_data"));

            // Зачарования
            if (section.contains("item.enchantments")) {
                ConfigurationSection enchants = section.getConfigurationSection("item.enchantments");
                for (String enchName : enchants.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByName(enchName.toUpperCase());
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, enchants.getInt(enchName), true);
                    }
                }
            }

            // Очистка лора (цены)
            meta.setLore(new ArrayList<>());
            reward.setItemMeta(meta);

            player.getInventory().addItem(reward);
            player.sendMessage(ChatColor.GREEN + "Вы купили: " + reward.getItemMeta().getDisplayName());
            return;
        }
    }
}
