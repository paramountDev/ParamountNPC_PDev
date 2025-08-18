package dev.paramountdev.paramountNpc_PDev.npc.quests;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LocationRequirement implements Requirement {

    private final Location target;
    private final double radius;

    public LocationRequirement(Location target, double radius) {
        this.target = target;
        this.radius = radius;
    }

    @Override
    public boolean isMetBy(Player player) {
        if (!player.getWorld().equals(target.getWorld())) return false;
        return player.getLocation().distance(target) <= radius;
    }

    @Override
    public void consumeFrom(Player player) {
        // Не нужно ничего удалять
    }

    @Override
    public String getDescription() {
        return "Придите в точку: " + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ();
    }

    public Location getTarget() {
        return target;
    }
}
