package dev.paramountdev.paramountNpc_PDev;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Utils {



    public static ItemStack createFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName("");
        filler.setItemMeta(fillerMeta);
        return filler;
    }

    public static void fillInventoryWithFiller(Inventory inventory) {
        ItemStack filler = createFiller();
        for(int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }
}
