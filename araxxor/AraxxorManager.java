package com.rs.game.player.content.araxxor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.rs.cores.CoresManager;
import com.rs.cores.FixedLengthRunnable;
import com.rs.game.player.Player;
import com.rs.utils.Colors;
import com.rs.utils.Logger;


/**
 * The hub of all Araxxor fights. Calculates the current
 * rotation at startup time, and tracks and processes all the live
 * instances.
 *
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017) <br/>
 *        1.1 (beta, 08/19/2017) <br/>
 */
public final class AraxxorManager {

    private static final List<Integer> ROT_1;
    private static final List<Integer> ROT_2;
    private static final List<Integer> ROT_3;

    private static AraxxorEnvironment.Paths[] CURRENT_PATHS =
            new AraxxorEnvironment.Paths[2];
    private static AraxxorEnvironment.Paths OFF_PATH;

    private static Map<Player, AraxxorFight> fightMap;

    static
    {
        ROT_1 = new ArrayList<>();
        ROT_2 = new ArrayList<>();
        ROT_3 = new ArrayList<>();
        for(int i = 1; i <= 31; i+= 6) {
            ROT_1.add(i);
            ROT_1.add(i + 1);
            ROT_2.add(i + 2);
            ROT_2.add(i + 3);
            ROT_3.add(i + 4);
            ROT_3.add(i + 5);
        }
    }

    private static void addFightToMap(AraxxorFight fight) {
        if(fightMap == null)
            fightMap = new ConcurrentHashMap<>();
        fight.getPlayers().forEach(player -> fightMap.put(player, fight));
    }

    static void unmapFight(Player p) {
        fightMap.remove(p);
    }

    static AraxxorFight fightOf(Player player) {
        return fightMap.get(player);
    }

    /**
     * Synchronize the Araxxor rotation for today's date by
     * setting the current rotation paths for the environment configurations.
     * @param calendar the system calendar to pull today's date
     */
    public static void init(Calendar calendar) {

        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if(ROT_1.contains(day)) {
            Logger.log(Logger.DEBUG, "Current Araxxor rotation: 1 --> Light and Acid");
            CURRENT_PATHS = new AraxxorEnvironment.Paths[] {AraxxorEnvironment.Paths.LIGHT,
                    AraxxorEnvironment.Paths.ACID};
            OFF_PATH = AraxxorEnvironment.Paths.MINIONS;
        } else if(ROT_2.contains(day)) {
            Logger.log(Logger.DEBUG, "Current Araxxor rotation: 2 --> Light and Minions");
            CURRENT_PATHS = new AraxxorEnvironment.Paths[] {AraxxorEnvironment.Paths.LIGHT,
                    AraxxorEnvironment.Paths.MINIONS};
            OFF_PATH = AraxxorEnvironment.Paths.ACID;
        } else if(ROT_3.contains(day)) {
            Logger.log(Logger.DEBUG, "Current Araxxor rotation: 3 --> Minions and Acid");
            CURRENT_PATHS = new AraxxorEnvironment.Paths[] {AraxxorEnvironment.Paths.MINIONS,
                    AraxxorEnvironment.Paths.ACID};
            OFF_PATH = AraxxorEnvironment.Paths.LIGHT;
        }

        Logger.log(Logger.DEBUG, "AraxxorManager initialized.");
    }

    static AraxxorEnvironment.Paths[] getCurrentPaths() {
        return CURRENT_PATHS;
    }

    /**
     * Safely finish the final internal steps of an {@link AraxxorFight}.<br/>
     * Send the players a congratulatory message, informing them of their new Araxxor
     * kill count, and warn them they have 3 minutes left to loot the corpse and leave
     * the instance. Starts a {@link FixedLengthRunnable} to perform player removal
     * if necessary after 3 minutes.
     * @param fight the fight instance to process and finish
     */
    static void process(AraxxorFight fight) {
        fight.getPlayers().forEach(p -> {
            p.addRaxKill();
            p.sendMessage(Colors.GREEN + "Nice kill! Your Araxxor kill count is now " + p.getRaxKC() + ".");
            p.sendMessage(Colors.GREEN + "You have 3 minutes to claim your loot and exit before you are forcefully removed.");
        });
        CoresManager.getServiceProvider().scheduleFixedLengthTask(new FixedLengthRunnable() {

            int time = 0;

            @Override
            public boolean repeat() {
                if(fight.shuttingDown())
                    return false;
                if(time == 180) {
                    fight.getPlayers().forEach(fight::safeRemovePlayer);
                    return false;
                } else if(time == 60) {
                    fight.notifyPlayers(Colors.GREEN + "You have 2 minutes " +
                            "to claim your loot and exit " +
                            "before you are forcefully removed.");
                } else if(time == 120) {
                    fight.notifyPlayers(Colors.YELLOW + "You have 1 minute " +
                            "to claim your loot and exit " +
                            "before you are forcefully removed.");
                } else if(time == 150) {
                    fight.notifyPlayers(Colors.RED + "You have 30 seconds" +
                            "to claim your loot and exit " +
                            "before you are forcefully removed.</col>");
                }
                time++;
                return true;
            }

        }, 0, 1);
    }

    static void generateFight(List<Player> players) {
        addFightToMap(new AraxxorFight(players));
    }

    public static AraxxorController controllerOf(Player player) {
        return (AraxxorController) player.getControlerManager().getControler();
    }

    public static AraxxorEnvironment.Paths getOffPath() {
        return OFF_PATH;
    }

}
