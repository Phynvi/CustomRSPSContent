package com.rs.game.player.content.araxxor;

import com.rs.cores.CoresManager;
import com.rs.game.World;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.npcs.Araxxor;
import com.rs.game.player.content.araxxor.npcs.Araxyte;
import com.rs.network.packet.PacketRepository;
import com.rs.utils.Colors;
import com.rs.utils.ItemExamines;
import com.rs.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An interface utility class responsible for sending an updating
 * player interfaces during the fight.
 *
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public final class AraxxorInterfaces {

    private static int ACID_INTERFACE = 15;
    private static int RAMP_HEALTH_CID = 10;
    private static int RAMP_HEALTH_DISPLAY_CID = 17;
    private static int ABSORBED_ACID_CID = 13;
    private static int ABSORBED_ACID_DISPLAY_CID = 18;
    private static int UNUSED = 16;

    private static int ABSORBED_CONFIG_FILE = 651;
    private static int GLOBAL_HEALTH_CONFIG = 816;
    private static int HEALTH_CONFIG_FILE = 817;

    /**
     * Sends the boss health overlay interface.
     * @param players the players to send the overlay to
     * @param araxyte the current boss NPC
     */
    public static void updateBossHealthInterface(List<Player> players, Araxyte araxyte) {
        players.forEach(player -> {

            player.getInterfaceManager().sendSecondaryOverlay(945);
            for (int i = 19; i < Utils.getInterfaceDefinitionsComponentsSize(945); i++)
                player.getPackets().sendHideIComponent(945, i, true);
            player.getPackets().sendHideIComponent(945, 0, true);
            player.getPackets().sendHideIComponent(945, 1, true);
            player.getPackets().sendHideIComponent(945, 2, false);
            player.getPackets().sendGlobalString(315, araxyte instanceof Araxxor ?
                    "Araxxor's Health" : "Araxxi's Health");
            player.getPackets().sendGlobalConfig(1233, (int) (((double) araxyte.getHitpoints()
                    / araxyte.getMaxHitpoints()) * 212) - 12);
        });
    }

    /**
     * Sends the acid absorption/ramp health secondary overlay interface.
     * @param players the players to send the overlay to
     */
    public static void sendAcidInterface(List<Player> players) {
        players.forEach(player -> {
            player.getInterfaceManager().sendOverlay(ACID_INTERFACE, false);
            player.getPackets().sendIComponentText(ACID_INTERFACE, RAMP_HEALTH_CID, "Ramp");
            player.getPackets().sendIComponentText(ACID_INTERFACE, ABSORBED_ACID_CID, "Absorbed");
            player.getPackets().sendIComponentText(ACID_INTERFACE, UNUSED, "");
            player.getPackets().sendIComponentText(ACID_INTERFACE, RAMP_HEALTH_DISPLAY_CID, "Ramp Health: " +
                    100 + "%");
            player.getPackets().sendIComponentText(ACID_INTERFACE, ABSORBED_ACID_DISPLAY_CID, "Acid Absorbed: " +
                    0 + "%");
        });
    }

    /**
     * Updates the acid absorption/ramp health overlay, given the update values.
     * @param players the players to send the interface update to
     * @param rampHealth the current ramp health
     * @param acidAbsorbed the current level of acid absorption
     */
    public static void updateAcidInterface(List<Player> players, int rampHealth, int acidAbsorbed) {
        if(rampHealth == 0 || acidAbsorbed == 0) {
            players.forEach(player ->
                player.getInterfaceManager().closeOverlay(false)
            );
            return;
        }
        Double scaledUIrampHealth = (double) rampHealth * (125.0 / 100.0);
        int scaledUIacidAbsorbed = acidAbsorbed * 10;
        players.forEach(player -> {
            player.getPackets().sendIComponentText(ACID_INTERFACE, RAMP_HEALTH_CID, "Ramp");
            player.getPackets().sendIComponentText(ACID_INTERFACE, ABSORBED_ACID_CID, "Absorbed");
            player.getPackets().sendIComponentText(ACID_INTERFACE, UNUSED, "");
            player.getPackets().sendIComponentText(ACID_INTERFACE, RAMP_HEALTH_DISPLAY_CID, "Ramp Health: " +
                    Integer.toString(rampHealth) + "%");
            player.getPackets().sendIComponentText(ACID_INTERFACE, ABSORBED_ACID_DISPLAY_CID, "Acid Absorbed: " +
                    Integer.toString(acidAbsorbed) + "%");
            player.getPackets().sendGlobalConfig(GLOBAL_HEALTH_CONFIG, 2);
            player.getPackets().sendGlobalConfig(HEALTH_CONFIG_FILE, scaledUIrampHealth.intValue());
            player.getPackets().sendConfigByFile(ABSORBED_CONFIG_FILE, scaledUIacidAbsorbed);
        });
    }

    /**
     * Only very slightly dimmed.
     */
    static final int LIGHT = 150;
    /**
     * Very dark, difficult to see.
     */
    static final int DARK = 75;
    /**
     * A flag to make the screen red.
     */
    static final int RED = -1;

    /**
     * Responsbile for sending (in dark mode) the lighting overlay interface.
     * @param player the player to send the overlay to
     */
    static void sendDarkness(Player player) {
        for (int i = 2; i < 12; i++)
            player.getPackets().sendHideIComponent(601, i, true);
        player.getInterfaceManager().closeSecondaryOverlay();
        CoresManager.getServiceProvider().executeWithDelay(() -> {
            player.getInterfaceManager().sendSecondaryOverlay(601);
            updateLighting(player, DARK);
        }, 2);

    }

    /**
     * Updates the lighting overlay interface with the specified
     * brightness (config).
     * @param player the player whose interface should be updated
     * @param config the brightness level
     * @see AraxxorInterfaces#DARK
     * @see AraxxorInterfaces#LIGHT
     * @see AraxxorInterfaces#RED
     */
    static void updateLighting(Player player, int config) {
        if(config == RED) {
            player.getInterfaceManager().closeSecondaryOverlay();
            player.getInterfaceManager().sendSecondaryOverlay(1140);
        } else {
            player.getPackets().sendGlobalConfig(1435, config);
        }

    }

    static void sendRewardsInterface(Player player, AraxxorReward reward) {
        player.getInterfaceManager().sendInterface(1284);
        player.getPackets().sendInterSetItemsOptionsScript(1284, 7, 100,
                8, 3, "Take", "Bank", "Discard", "Examine");
        player.getPackets().sendUnlockIComponentOptionSlots(1284, 7, 0, 10, 0, 1, 2, 3);
        player.getPackets().sendItems(100, reward.getItems());
        reward.notifyWorld(player.getDisplayName());
    }

    static boolean handleRewardsInterface(Player player, AraxxorFight fight,
                                       int interfaceId,
                                       int componentId, int slotId,
                                       int slotId2, int packetId)
    {
        if(interfaceId == 1284) {
            AraxxorReward reward = fight.getReward(player);
            switch (componentId) {
                case 8:
                    if (reward.isEmpty()) {
                        player.sendMessage("The corpse has already been looted.");
                        return false;
                    }
                    Arrays.stream(reward.getItems()).filter(i -> i != null)
                            .forEach(i -> player.getBank().addItem(i, true));
                    reward.discardItems();
                    player.getPackets().sendGameMessage("All of the items were moved to your bank.");
                    player.getPackets().sendItems(100, reward.getItems());
                    player.getBank().refreshItems();
                    return true;
                case 9:
                    if (reward.isEmpty()) {
                        player.sendMessage("The corpse has already been looted.");
                        return false;
                    }
                    reward.discardItems();
                    player.getPackets().sendGameMessage("All of the items were discarded.");
                    player.getPackets().sendItems(100, reward.getItems());
                    return true;
                case 10:
                    if (reward.isEmpty()) {
                        player.sendMessage("The corpse has already been looted.");
                        break;
                    }
                    AtomicBoolean outOfSpace = new AtomicBoolean();
                    Arrays.stream(reward.getItems()).filter(i -> i != null)
                            .forEach(i -> {
                                boolean freeUnstackable = !i.getDefinitions().isStackable()
                                        && player.getInventory().getFreeSlots() >= i.getAmount();
                                boolean stackableExists = i.getDefinitions().isStackable()
                                        && player.getInventory().containsItem(i);
                                boolean stackableFree = i.getDefinitions().isStackable()
                                        && player.getInventory().hasFreeSlots();
                                boolean check = freeUnstackable || stackableExists || stackableFree;
                                if(check) {
                                    player.getInventory().addItem(i);
                                    reward.removeItem(i);
                                } else
                                    outOfSpace.set(true);
                            });
                    if(outOfSpace.get())
                        player.sendMessage("You don't have enough free " +
                                "space in your inventory to hold any more items..");
                    reward.shift();
                    player.getPackets().sendItems(100, reward.getItems());
                    return true;
                case 7:
                    Item i = reward.getAt(slotId);
                    if(reward.size() < slotId + 1)
                        return false;
                    if(i == null)
                        return false;
                    if(i.getId() != slotId2)
                        return false;
                    if(packetId == 14) {
                        boolean freeUnstackable = !i.getDefinitions().isStackable()
                                && player.getInventory().getFreeSlots() >= i.getAmount();
                        boolean stackableExists = i.getDefinitions().isStackable()
                                && player.getInventory().containsItem(i);
                        boolean stackableFree = i.getDefinitions().isStackable()
                                && player.getInventory().hasFreeSlots();
                        boolean check = freeUnstackable || stackableExists || stackableFree;
                        if(check) {
                            player.getInventory().addItem(i);
                            reward.removeItemAndShift(i);
                            player.getPackets().sendItems(100, reward.getItems());
                        } else
                            player.sendMessage("You don't have enough free" +
                                    " space in inventory to hold any more items.");
                    } else if(packetId == 67) {
                        player.getBank().addItem(i, true);
                        reward.removeItemAndShift(i);
                        player.getPackets().sendItems(100, reward.getItems());
                    } else if(packetId == 5) {
                        reward.removeItemAndShift(i);
                        player.getPackets().sendItems(100, reward.getItems());
                    } else if(packetId == 55) {
                        player.sendMessage(ItemExamines.getExamine(i));
                        player.sendMessage("Grand Exchange guide price:" +
                                Utils.getFormattedNumber(
                                        ItemExamines.getPrice(i)) + ".");
                    }
                    break;
                default:
                    return true;
            }
        }
        return true;
    }

    /**
     * Closes all possible interfaces the player may have had open during the fight.
     * @param player the player to close screen interfaces for
     */
    static void closeAll(Player player) {
        player.getInterfaceManager().closeSecondaryOverlay();
        player.getInterfaceManager().closeOverlay(false);
    }

}
