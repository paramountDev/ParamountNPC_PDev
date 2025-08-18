package dev.paramountdev.paramountNpc_PDev;

import dev.paramountdev.paramountNpc_PDev.npc.NpcCommand;
import dev.paramountdev.paramountNpc_PDev.npc.NpcManager;
import dev.paramountdev.paramountNpc_PDev.npc.dialogues.DialogueManager;
import dev.paramountdev.paramountNpc_PDev.npc.quests.QuestManager;
import dev.paramountdev.paramountNpc_PDev.npc.trades.TradeManager;
import dev.paramountdev.paramountNpc_PDev.npc.vault.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ParamountNpc_PDev extends JavaPlugin {

    private static ParamountNpc_PDev instance;

    private NpcManager npcManager;
    private QuestManager questManager;
    private TradeManager tradeManager;
    private DialogueManager dialogueManager;

    @Override
    public void onEnable() {
        instance = this;


        this.npcManager = new NpcManager(this);
        getCommand("npc").setExecutor(new NpcCommand(npcManager));


        this.questManager = new QuestManager(getDataFolder());
        Bukkit.getPluginManager().registerEvents(questManager, this);


        this.tradeManager = new TradeManager();
        Bukkit.getPluginManager().registerEvents(tradeManager, this);


        dialogueManager = new DialogueManager();
        Bukkit.getPluginManager().registerEvents(dialogueManager, this);

        npcManager.loadNpcsFromFile();


        VaultHook.setup(this);


        sendSignatureToConsole("enabled");
    }

    @Override
    public void onDisable() {
        if(npcManager != null) {
            npcManager.saveNpcsToFile();
            npcManager.removeAllNpcs();
        }


       sendSignatureToConsole("disabled");
    }

    public static ParamountNpc_PDev getInstance() {
        return instance;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }




    private void sendSignatureToConsole(String pluginStatus) {
        getLogger().log(Level.INFO, "\n");
        getLogger().info("\u001B[35m!---------------ParamountNpc Plugin " + pluginStatus + "---------------!\u001B[0m");
        getLogger().info("\u001B[35m!---------------Made by Paramount_Dev---------------!\u001B[0m");
        getLogger().info("\u001B[35m!FunPay Link: https://funpay.com/uk/users/14397429/ !\u001B[0m");
        getLogger().log(Level.INFO, "\n");
    }
}
