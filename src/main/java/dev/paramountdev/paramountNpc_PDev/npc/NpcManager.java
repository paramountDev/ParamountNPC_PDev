package dev.paramountdev.paramountNpc_PDev.npc;

import com.bnstra.npclib.NPCLib;
import com.bnstra.npclib.api.NPC;
import com.bnstra.npclib.api.events.NPCInteractEvent;
import com.bnstra.npclib.api.skin.Skin;
import com.bnstra.npclib.api.state.NPCSlot;
import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
import dev.paramountdev.paramountNpc_PDev.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class NpcManager implements Listener {

    private final ParamountNpc_PDev main;
    private final NPCLib npcLib;
    private final Map<Integer, NpcData> npcs = new HashMap<>();
    private int nextId = 1;
    private Map<UUID, String> lastNpcClicked = new HashMap<>();

    public NpcManager(ParamountNpc_PDev main) {
        this.main = main;
        this.npcLib = new NPCLib(main);
        Bukkit.getPluginManager().registerEvents(this, main);
        startLookAtTask();
    }

    public int spawnNpc(Location loc, String name, String subname) {
        NPC npc = npcLib.createNPC(Arrays.asList(
                ChatColor.GOLD + name,
                ChatColor.GRAY + subname
        ));

        npc.setLocation(loc);
        npc.create();

        for (Player player : Bukkit.getOnlinePlayers()) {
            npc.show(player);
        }

        int id = nextId++;
        npcs.put(id, new NpcData(id, npc, name, subname));

        createDefaultPhraseFile(id);
        createDefaultQuestFile(id, name);
        createTradeFileForNpc(id);
        createDefaultDialogueFile(id);



        return id;
    }

    public boolean removeNpc(int id) {
        NpcData data = npcs.remove(id);
        if (data != null) {
            data.getNpc().destroy();
            deleteNpcFromFiles(id);
            return true;
        }
        return false;
    }

    private void deleteNpcFromFiles(int id) {

        File npcFile = new File(main.getDataFolder(), "npcs.yml");
        FileConfiguration npcConfig = YamlConfiguration.loadConfiguration(npcFile);
        npcConfig.set("npcs." + id, null);
        try {
            npcConfig.save(npcFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Удаление из npc_phrases.yml
        File phrasesFile = new File(main.getDataFolder(), "npc_phrases.yml");
        FileConfiguration phrasesConfig = YamlConfiguration.loadConfiguration(phrasesFile);
        phrasesConfig.set("npc_phrases." + id, null);
        try {
            phrasesConfig.save(phrasesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Удаление из npc_trades.yml
        File tradesFile = new File(main.getDataFolder(), "npc_trades.yml");
        FileConfiguration tradesConfig = YamlConfiguration.loadConfiguration(tradesFile);
        tradesConfig.set("npc_trades." + id, null);
        try {
            tradesConfig.save(tradesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Удаление из npc_dialogues.yml
        File dialoguesFile = new File(main.getDataFolder(), "npc_dialogues.yml");
        FileConfiguration dialoguesConfig = YamlConfiguration.loadConfiguration(dialoguesFile);
        dialoguesConfig.set("npc_dialogues." + id, null);
        try {
            dialoguesConfig.save(dialoguesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Удаление из quests.yml
        File questsFile = new File(main.getDataFolder(), "quests.yml");
        FileConfiguration questsConfig = YamlConfiguration.loadConfiguration(questsFile);

        // Находим и удаляем квесты, привязанные к этому NPC
        ConfigurationSection questsSection = questsConfig.getConfigurationSection("quests");
        if (questsSection != null) {
            for (String key : new ArrayList<>(questsSection.getKeys(false))) {
                String path = "quests." + key + ".npc_id";
                if (String.valueOf(id).equals(questsConfig.getString(path))) {
                    questsConfig.set("quests." + key, null);
                }
            }
        }

        try {
            questsConfig.save(questsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Optional<Map.Entry<Integer, NpcData>> getNpcByName(String name) {
        return npcs.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<String> getNpcNameById(int id) {
        NpcData data = npcs.get(id);
        return data != null ? Optional.of(data.getName()) : Optional.empty();
    }

    public void setNpcSkinFromUrl(int id, String mineskinUrl, CommandSender sender) {
        NpcData data = npcs.get(id);
        if (data == null) {
            sender.sendMessage("§cNPC с ID " + id + " не найден.");
            return;
        }

        String skinId = extractSkinIdFromUrl(mineskinUrl);
        if (skinId == null) {
            sender.sendMessage("§cНеверный URL скина.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.mineskin.org/get/id/" + skinId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(response.toString());

                    JSONObject dataObj = (JSONObject) json.get("data");
                    JSONObject textureObj = (JSONObject) dataObj.get("texture");

                    String value = (String) textureObj.get("value");
                    String signature = (String) textureObj.get("signature");

                    Skin skin = new Skin(value, signature);

                    Bukkit.getScheduler().runTask(main, () -> {

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            data.getNpc().hide(player);
                        }
                        data.getNpc().setSkin(skin);
                        data.setSkin(skin);
                        data.getNpc().updateSkin(skin);

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            data.getNpc().show(player); // Показываем с новым скином
                        }

                        sender.sendMessage("§aСкин NPC обновлён из URL.");

                    });
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(main, () -> {
                        sender.sendMessage("§cНе удалось загрузить скин по URL.");
                    });
                }
            }
        }.runTaskAsynchronously(main);
    }

    private String extractSkinIdFromUrl(String url) {
        if (url == null) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == url.length() - 1) return null;
        return url.substring(lastSlash + 1);
    }


    private void startLookAtTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (NpcData npcData : npcs.values()) {
                    NPC npc = npcData.getNpc();
                    Location npcLoc = npc.getLocation();
                    if (npcLoc == null || npcLoc.getWorld() == null) continue;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(npcLoc.getWorld()) &&
                                player.getLocation().distance(npcLoc) <= 5) {
                            npc.lookAt(player.getLocation());
                        }
                    }
                }
            }
        }.runTaskTimer(main, 0L, 10L);
    }

    public void setNpcItem(int id, String slotName, String itemName, CommandSender sender) {
        NpcData data = npcs.get(id);
        if (data == null) {
            sender.sendMessage("§cNPC с ID " + id + " не найден.");
            return;
        }

        NPCSlot slot;
        try {
            slot = NPCSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cНеверная часть тела. Возможные значения: " +
                    Arrays.toString(NPCSlot.values()));
            return;
        }

        Material material = Material.getMaterial(itemName);
        if (material == null || !material.isItem()) {
            sender.sendMessage("§cНеверное имя предмета: " + itemName);
            return;
        }

        ItemStack item = new ItemStack(material);
        data.getNpc().setItem(slot, item);
        sender.sendMessage("§aПредмет " + itemName + " установлен на " + slot.name() + " NPC с ID " + id + ".");
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(main, () -> {
            for (NpcData data : npcs.values()) {
                data.getNpc().show(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        for (NpcData data : npcs.values()) {
            NPC npc = data.getNpc();
            Location npcLoc = npc.getLocation();

            if (!npcLoc.getWorld().equals(playerLoc.getWorld())) continue;

            double distance = npcLoc.distance(playerLoc);
            if (distance <= 3) {
                long now = System.currentTimeMillis();
                long lastTalk = data.getLastTalkTime();
                int cooldownSeconds = data.getCooldown();

                if ((now - lastTalk) < cooldownSeconds * 1000L) continue; // NPC на КД

                List<String> phrases = data.getPhrases();
                if (!phrases.isEmpty()) {
                    String randomPhrase = phrases.get(new Random().nextInt(phrases.size()));
                    player.sendMessage(ChatColor.GOLD + "[" + data.getName() + "] " + ChatColor.WHITE + randomPhrase);
                    data.setLastTalkTime(now);
                }
            }
        }
    }



    //


    //


    // ГЛАВНОЕ МЕНЮ

    @EventHandler
    public void onNpcClickOpenMenu(NPCInteractEvent event) {
        Player player = event.getWhoClicked();
        NPC clickedNpc = event.getNPC();
        for (Map.Entry<Integer, NpcData> entry : npcs.entrySet()) {

            if (entry.getValue().getNpc().equals(clickedNpc)) {
                int npcId = entry.getKey(); // <-- наш ID, который тебе нужен
                lastNpcClicked.put(player.getUniqueId(), String.valueOf(npcId));

                Bukkit.getScheduler().runTask(main, () -> openMainMenu(player));
                return;
            }
        }

        player.sendMessage(ChatColor.RED + "Не удалось определить ID NPC.");
    }


    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Меню NPC");

        Utils.fillInventoryWithFiller(gui);

        // Торговля
        ItemStack trade = new ItemStack(Material.EMERALD);
        ItemMeta tradeMeta = trade.getItemMeta();
        tradeMeta.setDisplayName(ChatColor.GREEN + "💰 Торговля");
        trade.setItemMeta(tradeMeta);
        gui.setItem(11, trade);

        // Квесты
        ItemStack quest = new ItemStack(Material.BOOK);
        ItemMeta questMeta = quest.getItemMeta();
        questMeta.setDisplayName(ChatColor.GOLD + "📝 Квесты");
        quest.setItemMeta(questMeta);
        gui.setItem(13, quest);

        // Диалоги
        ItemStack dialog = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta dialogMeta = dialog.getItemMeta();
        dialogMeta.setDisplayName(ChatColor.AQUA + "❓ Общение");
        dialog.setItemMeta(dialogMeta);
        gui.setItem(15, dialog);

        player.openInventory(gui);
    }

    @EventHandler
    public void onMainMenuClick(InventoryClickEvent event) {
      if(!event.getView().getTitle().equalsIgnoreCase(ChatColor.DARK_GRAY + "Меню NPC")) {
          return;
      }

      event.setCancelled(true);

      ItemStack item = event.getCurrentItem();
      if(item == null) return;
      if(!item.hasItemMeta()) return;
      if(!item.getItemMeta().hasDisplayName()) return;


      if(item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "📝 Квесты")) {
          Player player = (Player) event.getWhoClicked();
          String npcId = getLastNpcId(player);

          if (npcId == null) {
              player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить NPC.");
              return;
          }

          main.getQuestManager().openQuestMenu(player, npcId);
          return;
      }

        if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GREEN + "💰 Торговля")) {
            Player player = (Player) event.getWhoClicked();
            String npcId = getLastNpcId(player);

            if (npcId == null) {
                player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить NPC.");
                return;
            }

            main.getTradeManager().openTradeMenu(player, npcId);
            return;
        }

        if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.AQUA + "❓ Общение")) {
            Player player = (Player) event.getWhoClicked();
            String npcId = getLastNpcId(player);
            if (npcId == null) {
                player.sendMessage(ChatColor.RED + "Ошибка: не удалось определить NPC.");
                return;
            }

            main.getDialogueManager().openDialogueMenu(player, Integer.parseInt(npcId));
        }

    }


    // ГЛАВНОЕ МЕНЮ


    //


    // ЗАГРУЗКА И ВЫГРУЗКА НПС

    public void saveNpcsToFile() {
        File file = new File(main.getDataFolder(), "npcs.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<Integer, NpcData> entry : npcs.entrySet()) {
            int id = entry.getKey();
            NpcData data = entry.getValue();
            Location loc = data.getNpc().getLocation();

            String path = "npcs." + id;
            config.set(path + ".name", data.getName());
            config.set(path + ".subname", data.getSubName());
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());


            if (data.getSkin() != null) {
                config.set(path + ".skin.value", data.getSkin().getValue());
                config.set(path + ".skin.signature", data.getSkin().getSignature());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadNpcsFromFile() {
        File file = new File(main.getDataFolder(), "npcs.yml");
        if (!file.exists()) return;

        File phrasesFile = new File(main.getDataFolder(), "npc_phrases.yml");
        if (!phrasesFile.exists()) return;

        FileConfiguration phrasesConfig = YamlConfiguration.loadConfiguration(phrasesFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.isConfigurationSection("npcs")) return;

        for (String idStr : config.getConfigurationSection("npcs").getKeys(false)) {
            try {
                int id = Integer.parseInt(idStr);
                String name = config.getString("npcs." + idStr + ".name");
                String subName = config.getString("npcs." + idStr + ".subname", "Click to say Hi");
                String worldName = config.getString("npcs." + idStr + ".world");
                double x = config.getDouble("npcs." + idStr + ".x");
                double y = config.getDouble("npcs." + idStr + ".y");
                double z = config.getDouble("npcs." + idStr + ".z");
                float yaw = (float) config.getDouble("npcs." + idStr + ".yaw");
                float pitch = (float) config.getDouble("npcs." + idStr + ".pitch");

                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);

                NPC npc = npcLib.createNPC(Arrays.asList(
                        ChatColor.GOLD + name,
                        ChatColor.GRAY + subName
                ));
                npc.setLocation(loc);
                npc.create();

                Skin skin = null;
                if (config.contains("npcs." + idStr + ".skin")) {
                    String value = config.getString("npcs." + idStr + ".skin.value");
                    String signature = config.getString("npcs." + idStr + ".skin.signature");
                    skin = new Skin(value, signature);
                    npc.setSkin(skin);
                    npc.updateSkin(skin);
                }

                for (Player player : Bukkit.getOnlinePlayers()) npc.show(player);

                NpcData data = new NpcData(id, npc, name, subName);
                data.setSkin(skin);

                if (phrasesConfig.contains("npc_phrases." + id)) {
                    List<String> phrases = phrasesConfig.getStringList("npc_phrases." + id + ".phrases");
                    int cd = phrasesConfig.getInt("npc_phrases." + id + ".cooldown", 10);
                    data.setPhrases(phrases);
                    data.setCooldown(cd);
                }

                npcs.put(id, data);


                if (id >= nextId) nextId = id + 1;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Ошибка при загрузке NPC с ID " + idStr);
                e.printStackTrace();
            }
        }
    }

    // ЗАГРУЗКА И ВЫГРУЗКА НПС


    //


    // БАЗОВАЯ КОНФИГУРАЦИЯ НПС


    private void createDefaultDialogueFile(int id) {
        File file = new File(main.getDataFolder(), "npc_dialogues.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String basePath = "npc_dialogues." + id;
        if (config.contains(basePath)) return;

        config.set(basePath + ".Спросить о погоде", "Сегодня чудесная погода, не так ли?");

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void createTradeFileForNpc(int npcId) {
        File file = new File(main.getDataFolder(), "npc_trades.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String basePath = "npc_trades." + npcId + ".trades";
        if (config.contains(basePath)) return; // уже создан

        // Пример торговли — продаётся алмаз за 100 денег
        ConfigurationSection section = config.createSection(basePath + ".0");
        section.set("item.type", "DIAMOND");
        section.set("item.name", "§bЧистый алмаз");
        section.set("item.lore", Collections.singletonList("§7Ценный ресурс"));
        section.set("item.custom_model_data", 1);

        section.set("cost.money", 100);
        // Или можно: section.set("cost.items.DIRT", 32);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void createDefaultPhraseFile(int id) {
        File file = new File(main.getDataFolder(), "npc_phrases.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = "npc_phrases." + id;
        if (config.contains(path)) return; // Уже существует

        config.set(path + ".phrases", Collections.singletonList("Привет"));
        config.set(path + ".cooldown", 10);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultQuestFile(int id, String npcName) {
        File file = new File(main.getDataFolder(), "quests.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String questId = "npc_" + id + "_quest";
        String path = "quests." + questId;

        if (config.contains(path)) {
            Bukkit.broadcastMessage("Квест уже существует!");
            return;
        }

        config.set(path + ".name", "Первый квест");
        config.set(path + ".description", "Принеси 1 камень");
        config.set(path + ".npc_id", String.valueOf(id));

        config.set(path + ".required_items.STONE", 1);
        config.set(path + ".rewards.EXPERIENCE", 50);

        config.set(path + ".dependencies", new ArrayList<>());
        config.set(path + ".cooldown", 60);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // БАЗОВАЯ КОНФИГУРАЦИЯ НПС

    //


    public void removeAllNpcs() {
        for (Map.Entry<Integer, NpcData> entry : npcs.entrySet()) {
            entry.getValue().getNpc().destroy();
        }
        npcs.clear();
        lastNpcClicked.clear();
    }

    public String getLastNpcId(Player player) {
        return lastNpcClicked.get(player.getUniqueId());
    }

    public void setLastNpcClicked(Map<UUID, String> lastNpcClicked1) {
        lastNpcClicked = lastNpcClicked1;
    }

    public Map<UUID, String> getLastNpcClicked() {
        return lastNpcClicked;
    }

    public Map<Integer, NpcData> getNpcMap() {
        return npcs;
    }


}
