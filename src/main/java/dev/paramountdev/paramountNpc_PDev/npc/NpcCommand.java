package dev.paramountdev.paramountNpc_PDev.npc;

import com.bnstra.npclib.api.skin.Skin;
import com.bnstra.npclib.api.state.NPCSlot;
import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpcCommand implements CommandExecutor, TabCompleter {
    private final NpcManager npcManager;
    private final String PREFIX = ChatColor.DARK_GREEN + "[" + ChatColor.GOLD + "ParamountNpc" + ChatColor.DARK_GREEN + "] " + ChatColor.RESET;

    public NpcCommand(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Только игрок может использовать эту команду.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(PREFIX + "Использование: /npc <create|setskin|remove|info|setitems|author>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                String fullInput = String.join(" ", args);
                Pattern pattern = Pattern.compile("create\\s+\"([^\"]+)\"\\s+\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(fullInput);

                if (!matcher.find()) {
                    player.sendMessage(PREFIX + ChatColor.YELLOW + "Пример: /npc create \"Имя\" \"Доп. Имя\"");
                    return true;
                }

                String name = matcher.group(1);
                String subName = matcher.group(2);

                int id = npcManager.spawnNpc(player.getLocation(), name, subName);
                player.sendMessage(PREFIX + ChatColor.GREEN + "NPC создан. ID: " + ChatColor.GOLD + id);
            }


            case "setskin" -> {
                if (args.length < 4 || !args[2].equalsIgnoreCase("--url")) {
                    player.sendMessage(PREFIX + ChatColor.YELLOW + "/npc setskin <id> --url <mineskin_url>");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    String url = args[3];
                    npcManager.setNpcSkinFromUrl(id, url, player);
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "ID должен быть числом.");
                }
            }

            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(PREFIX + ChatColor.YELLOW + "/npc remove <id>");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    if (npcManager.removeNpc(id)) {
                        player.sendMessage(PREFIX + ChatColor.GREEN + "NPC удалён.");
                    } else {
                        player.sendMessage(PREFIX + ChatColor.RED + "NPC с таким ID не найден.");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "ID должен быть числом.");
                }
            }

            case "setitems" -> {
                if (args.length < 4) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Использование: /npc setitems <id> <body part> <item name>");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    String slotName = args[2].toUpperCase();
                    String itemName = args[3].toUpperCase();
                    npcManager.setNpcItem(id, slotName, itemName, player);
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "ID должен быть числом.");
                }
            }

            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage(PREFIX + ChatColor.YELLOW + "/npc info <имя>");
                    return true;
                }
                String name = args[1];
                npcManager.getNpcByName(name).ifPresentOrElse(entry -> {
                    NpcData data = entry.getValue();
                    Location loc = data.getNpc().getLocation();
                    Skin skin = data.getSkin();

                    player.sendMessage("");
                    player.sendMessage(PREFIX + ChatColor.AQUA + "Информация об NPC:");
                    player.sendMessage(ChatColor.GRAY + " ▪ ID: " + ChatColor.GOLD + data.getId());
                    player.sendMessage(ChatColor.GRAY + " ▪ Имя: " + ChatColor.YELLOW + data.getName());
                    player.sendMessage(ChatColor.GRAY + " ▪ Доп. Имя: " + ChatColor.YELLOW + data.getSubName());
                    player.sendMessage(ChatColor.GRAY + " ▪ Локация: " + ChatColor.GREEN + loc.getWorld().getName()
                            + ChatColor.GRAY + " [x=" + ChatColor.GOLD + loc.getBlockX()
                            + ChatColor.GRAY + ", y=" + ChatColor.GOLD + loc.getBlockY()
                            + ChatColor.GRAY + ", z=" + ChatColor.GOLD + loc.getBlockZ() + "]");
                    player.sendMessage(ChatColor.GRAY + " ▪ Скин: " + (skin != null ? ChatColor.AQUA + skin.getValue().substring(0, 16) + "..." : ChatColor.RED + "Нет"));
                    player.sendMessage("");
                }, () -> player.sendMessage(PREFIX + ChatColor.RED + "NPC с именем '" + name + "' не найден."));
            }

            case "author" -> {
                player.sendMessage("");
                player.sendMessage("");
                player.sendMessage(ChatColor.DARK_GREEN + "=====[ " + ChatColor.GOLD + "ParamountNpc" + ChatColor.DARK_GREEN + " ]=====");
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "Автор: " + ChatColor.GREEN + "ParamountDev");
                player.sendMessage("");

                // FunPay
                TextComponent funpayPrefix = new TextComponent("• ");
                funpayPrefix.setColor(net.md_5.bungee.api.ChatColor.GOLD);

                TextComponent funpayLink = new TextComponent("FunPay профиль");
                funpayLink.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                funpayLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://funpay.com/uk/users/14397429/"));
                funpayLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Открыть профиль FunPay").create()));
                funpayPrefix.addExtra(funpayLink);
                player.spigot().sendMessage(funpayPrefix);

                // Telegram
                TextComponent tgPrefix = new TextComponent("• ");
                tgPrefix.setColor(net.md_5.bungee.api.ChatColor.GOLD);

                TextComponent tgLink = new TextComponent("Telegram: @paramount1_dev");
                tgLink.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                tgLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://t.me/paramount1_dev"));
                tgLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Открыть Telegram").create()));
                tgPrefix.addExtra(tgLink);
                player.spigot().sendMessage(tgPrefix);

                player.sendMessage("");
                player.sendMessage(ChatColor.DARK_GREEN + "===============================");
                player.sendMessage("");
                player.sendMessage("");
            }

            case "reload" -> {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Перезагрузка плагина...");

                ParamountNpc_PDev.getInstance().getQuestManager().loadQuests(ParamountNpc_PDev.getInstance().getDataFolder());

                player.sendMessage(PREFIX + ChatColor.GREEN + "Плагин успешно перезагружен.");
            }


            default -> player.sendMessage(PREFIX + ChatColor.RED + "Неизвестная подкоманда.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();
        List<String> subcommands = Arrays.asList("create", "setskin", "remove", "info", "setitems", "author", "reload");

        if (args.length == 1) {
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) suggestions.add(sub);
            }
            return suggestions;
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "remove", "setskin", "setitems" -> {
                    for (Integer id : npcManager.getNpcMap().keySet()) {
                        suggestions.add(String.valueOf(id));
                    }
                }
                case "info" -> {
                    for (Map.Entry<Integer, NpcData> entry : npcManager.getNpcMap().entrySet()) {
                        suggestions.add(entry.getValue().getName());
                    }
                }
            }
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && sub.equals("setskin")) {
            return Collections.singletonList("--url");
        }

        if (args.length == 3 && sub.equals("setitems")) {
            for (NPCSlot slot : NPCSlot.values()) {
                suggestions.add(slot.name());
            }
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .toList();
        }

        if (args.length == 4 && sub.equals("setitems")) {
            for (Material material : Material.values()) {
                if (material.isItem()) {
                    suggestions.add(material.name());
                }
            }
            return suggestions.stream()
                    .filter(s -> s.startsWith(args[3].toUpperCase()))
                    .limit(50)
                    .toList();
        }

        return Collections.emptyList();
    }
}
