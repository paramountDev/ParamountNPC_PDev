package dev.paramountdev.paramountNpc_PDev.npc.quests;

import org.bukkit.entity.Player;

public interface Requirement {
    boolean isMetBy(Player player);
    void consumeFrom(Player player);
    String getDescription();
}

