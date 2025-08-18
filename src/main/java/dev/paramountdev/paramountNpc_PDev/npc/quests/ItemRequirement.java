package dev.paramountdev.paramountNpc_PDev.npc.quests;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRequirement implements Requirement {

    private final Material material;
    private final int amount;

    public ItemRequirement(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Override
    public boolean isMetBy(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
                if (total >= amount) return true;
            }
        }
        return false;
    }

    @Override
    public void consumeFrom(Player player) {
        int toRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int amt = item.getAmount();
                if (amt <= toRemove) {
                    toRemove -= amt;
                    contents[i] = null;
                } else {
                    item.setAmount(amt - toRemove);
                    toRemove = 0;
                }
            }
        }

        player.getInventory().setContents(contents);
    }

    @Override
    public String getDescription() {
        return amount + " x " + material.name();
    }
}

