package gg.revival.factions.core.signs;

import gg.revival.factions.core.FC;
import gg.revival.factions.core.signs.listener.SignsListener;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;
import org.bukkit.material.SpawnEgg;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Signs {

    @Getter private FC core;

    public Signs(FC core) {
        this.core = core;

        onEnable();
    }

    /**
     * Contains players who have recently interacted with a shop sign, prevents double-clicks
     */
    @Getter Set<UUID> interactLock = new HashSet<>();

    /**
     * Returns an ItemStack based on name, checks for custom materials as well
     * @param itemName The item to be looked up
     * @return ItemStack based on name
     */
    public ItemStack getItemStackFromString(String itemName) {
        if(itemName.equalsIgnoreCase(CustomMaterial.CROWBAR.toString()))
            return core.getMechanics().getCrowbars().getCrowbar();

        if(itemName.equalsIgnoreCase(CustomMaterial.CHAIN_HELMET.toString().replace("_", " ")))
            return new ItemStack(Material.CHAINMAIL_HELMET);

        if(itemName.equalsIgnoreCase(CustomMaterial.CHAIN_CHESTPLATE.toString().replace("_", " ")))
            return new ItemStack(Material.CHAINMAIL_CHESTPLATE);

        if(itemName.equalsIgnoreCase(CustomMaterial.CHAIN_LEGGINGS.toString().replace("_", " ")))
            return new ItemStack(Material.CHAINMAIL_LEGGINGS);

        if(itemName.equalsIgnoreCase(CustomMaterial.CHAIN_BOOTS.toString().replace("_", " ")))
            return new ItemStack(Material.CHAINMAIL_BOOTS);

        if(itemName.equalsIgnoreCase(CustomMaterial.COW_EGG.toString().replace("_", " "))) {
            SpawnEgg spawnEgg = new SpawnEgg();
            spawnEgg.setSpawnedType(EntityType.COW);
            return spawnEgg.toItemStack();
        }

        if(itemName.equalsIgnoreCase(CustomMaterial.END_PORTAL_FRAME.toString().replace("_", " ")))
            return new ItemStack(Material.ENDER_PORTAL_FRAME);

        if(itemName.equalsIgnoreCase(CustomMaterial.LAPIS.toString().replace("_", " "))) {
            Dye dye = new Dye();
            dye.setColor(DyeColor.BLUE);
            return dye.toItemStack();
        }

        Material material = null;
        short data;

        String[] obj = itemName.split(":");

        if(obj.length == 2 && NumberUtils.isNumber(obj[1])) {
            material = Material.getMaterial(obj[0].toUpperCase());
            data = (short)NumberUtils.toInt(obj[1]);

            if(material != null) return new ItemStack(material, 1, data);
        }

        for(Material materials : Material.values()) {
            if(materials.name().replace("_", "").equalsIgnoreCase(itemName.toUpperCase()))
                material = materials;
        }

        if(material != null)
            return new ItemStack(material);

        return null;
    }

    /**
     * Returns true of the given strings meet a buysigns requirements
     * @param lineOne
     * @param lineTwo
     * @param lineThree
     * @param lineFour
     * @return Is a valid sign or not
     */
    public boolean isBuySign(String lineOne, String lineTwo, String lineThree, String lineFour) {
        if(!lineOne.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "- Buy -"))
            return false;

        if(!NumberUtils.isNumber(lineTwo.replace("Amt: ", "")))
            return false;

        ItemStack item = getItemStackFromString(lineThree);

        if(item == null)
            return false;

        if(!NumberUtils.isNumber(lineFour.replace("$", "")))
            return false;

        return true;
    }

    /**
     * Returns true if the given strings meet a sellsigns requirements
     * @param lineOne
     * @param lineTwo
     * @param lineThree
     * @param lineFour
     * @return Is a valid sell sign
     */
    public boolean isSellSign(String lineOne, String lineTwo, String lineThree, String lineFour) {
        if(!lineOne.equals(ChatColor.RED + "" + ChatColor.BOLD + "- Sell -"))
            return false;

        if(!NumberUtils.isNumber(lineTwo.replace("Amt: ", ""))) return false;

        ItemStack item = getItemStackFromString(lineThree);

        if(item == null) return false;

        if(!NumberUtils.isNumber(lineFour.replace("$", ""))) return false;

        return true;
    }

    /**
     * Returns true if the given values match proper kit sign syntax
     * @param lineOne
     * @param lineTwo
     * @return
     */
    public boolean isKitSign(String lineOne, String lineTwo) {
        if(!lineOne.equalsIgnoreCase(ChatColor.BLUE + "" + ChatColor.BOLD + "- Kit -")) return false;

        if(core.getKits().getKitByName(lineTwo) == null) return false;

        return true;
    }

    /**
     * Returns true if the given strings meet a valid signs requirements
     * @param lineTwo
     * @param lineThree
     * @param lineFour
     * @return
     */
    public boolean isValidSign(String lineTwo, String lineThree, String lineFour) {
        if(!NumberUtils.isNumber(lineTwo)) return false;

        ItemStack item = getItemStackFromString(lineThree);

        if(item == null) return false;

        if(!NumberUtils.isNumber(lineFour)) return false;

        return true;
    }

    public void onEnable() {
        loadListeners();
    }

    public void loadListeners() {
        Bukkit.getPluginManager().registerEvents(new SignsListener(core), core);
    }

}
