package dev.paramountdev.paramountNpc_PDev.npc.dialogues;

import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
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
import java.util.*;

public class DialogueManager implements Listener {

    private final ParamountNpc_PDev main = ParamountNpc_PDev.getInstance();

    public void openDialogueMenu(Player player, int npcId) {
        File file = new File(main.getDataFolder(), "npc_dialogues.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("npc_dialogues." + npcId);
        if (section == null) {
            player.sendMessage("§eЭтот NPC пока ничего не рассказывает.");
            return;
        }

        Set<String> questions = new LinkedHashSet<>(section.getKeys(false));
        questions.remove("unlocks");

        ConfigurationSection unlocksSection = section.getConfigurationSection("unlocks");
        if (unlocksSection != null) {
            Set<String> completedQuests = main.getQuestManager().getCompletedQuestIds(player); // ты добавишь этот метод
            for (String questId : unlocksSection.getKeys(false)) {
                if (completedQuests.contains(questId)) {
                    ConfigurationSection unlocked = unlocksSection.getConfigurationSection(questId);
                    if (unlocked != null) {
                        questions.addAll(unlocked.getKeys(false));
                    }
                }
            }
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.BLUE + "Диалог");

        int slot = 0;
        for (String question : questions) {
            if (slot >= 27) break;
            ItemStack qItem = new ItemStack(Material.PAPER);
            ItemMeta meta = qItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + question);
            qItem.setItemMeta(meta);
            inv.setItem(slot++, qItem);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onDialogueClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(ChatColor.BLUE + "Диалог")) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        Player player = (Player) event.getWhoClicked();
        String npcIdStr = main.getNpcManager().getLastNpcId(player);
        if (npcIdStr == null) return;

        int npcId = Integer.parseInt(npcIdStr);
        String question = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        File file = new File(main.getDataFolder(), "npc_dialogues.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String answer = config.getString("npc_dialogues." + npcId + "." + question);
        if (answer == null) {
            ConfigurationSection unlocks = config.getConfigurationSection("npc_dialogues." + npcId + ".unlocks");
            if (unlocks != null) {
                for (String questId : unlocks.getKeys(false)) {
                    ConfigurationSection section = unlocks.getConfigurationSection(questId);
                    if (section != null && section.contains(question)) {
                        answer = section.getString(question);
                        break;
                    }
                }
            }
        }

        if (answer != null) {
            Optional<String> npcName = main.getNpcManager().getNpcNameById(npcId);
            player.sendMessage(ChatColor.GOLD + "[" + npcName.orElse("NPC") + "] " + ChatColor.WHITE + answer);
        } else {
            player.sendMessage("§cНет ответа на этот вопрос.");
        }

        player.closeInventory();
    }
}
