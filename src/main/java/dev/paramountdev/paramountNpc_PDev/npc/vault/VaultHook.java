package dev.paramountdev.paramountNpc_PDev.npc.vault;

import dev.paramountdev.paramountNpc_PDev.ParamountNpc_PDev;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {
    private static Economy econ = null;

    public static void setup(ParamountNpc_PDev plugin) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    public static boolean has(Player player, double amount) {
        return econ != null && econ.has(player, amount);
    }

    public static void withdraw(Player player, double amount) {
        if (econ != null) econ.withdrawPlayer(player, amount);
    }

    public static void deposit(Player player, double amount) {
        if (econ != null) econ.depositPlayer(player, amount);
    }
}

