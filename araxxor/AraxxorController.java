package com.rs.game.player.content.araxxor;

import com.rs.Settings;
import com.rs.game.Animation;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.player.Inventory;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.npcs.Araxxor;
import com.rs.game.player.controllers.Controller;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Colors;
import com.rs.utils.DialogueOptionEvent;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)<br/>
 *        1.1 (beta, 08/19/2017)
 */
public class AraxxorController extends Controller {

    private AraxxorFight fight;
    private boolean readKeyStroke;
    private boolean correctKeyStroke;
    private int selectedKey = -1;

    private int spamClicks;

    @Override
    public void start() {
        fight = (AraxxorFight) getArguments()[0];
        setArguments(null);
    }

    @Override
    public boolean sendDeath() {
        player.lock();
        player.stopAll();
        WorldTasksManager.schedule(new WorldTask() {
            int loop;

            @Override
            public void run() {
                if (loop == 0) {
                    player.setNextAnimation(new Animation(836));
                    player.sendMessage("Oh dear, you have died.");
                }
                if (loop == 3) {
                    player.getPackets().sendMusicEffect(90);
                    player.getDeathManager().setSafe(true);
                    player.reset();
                    player.unlock();
                    player.setNextAnimation(new Animation(-1));
                    fight.safeRemovePlayer(player);
                    stop();
                }
                loop++;
            }
        }, 0, 1);
        return false;
    }

    @Override
    public boolean checkWalkStep(int lastX, int lastY, int nextX, int nextY) {
        phaseProcess(nextX, nextY);
        return true;
    }


    @Override
    public boolean processMagicTeleport(WorldTile toTile) {
        player.sendMessage("The only way out is victory or death!");
        return false;
    }

    @Override
    public boolean logout() {
        player.setLocation(Settings.START_PLAYER_LOCATION);
        fight.safeRemovePlayer(player);
        return true;
    }

    @Override
    public boolean processButtonClick(int interfaceId, int componentId, int slotId, int slotId2, int packetId) {
        if(fight.getPhase() == 5) {
            return AraxxorInterfaces.handleRewardsInterface(player, fight,
                    interfaceId, componentId, slotId, slotId2, packetId);
        } else {
            if(interfaceId == Inventory.INVENTORY_INTERFACE) {
                if(slotId2 == 33870 && fight.getPhase() == 0) {
                    player.getDialogueManager().startDialogue(new PheromoneDialogue());
                    return false;
                } else if(slotId2 == 33870 && fight.getPhase() > 0) {
                    player.sendMessage("It's too late to do that now!");
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean processCommand(String s, boolean b, boolean c) {
        if(player.isDev())
            return true;
        player.sendMessage(Colors.RED+"Commands cannot be used in dungeoneering, PM an admin if you need help!");
        return false;
    }

    @Override
    public boolean canMove(int dir) {
        if(player.isCantWalk()) {
            spamClicks++;
            if(spamClicks >= 4)
                fight.getBoss().releaseCocoon();
            return false;
        }
        return true;
    }

    public void resetSpamClicks() {
        spamClicks = 0;
    }

    @Override
    public boolean processObjectClick1(WorldObject object) {


        switch(fight.getPhase()) {
            case 0:
                return false;
            case 1:
                return processObjectClickPhaseOne(object);
            case 2:
                return false;
            case 3:
                return processObjectClickPhaseThree(object);
            case 5:
                return processPostFightObjectClick(object);
        }

        return true;
    }

    private boolean processObjectClickPhaseOne(WorldObject object) {

        final AraxxorEnvironment.Paths webOne = fight.getConfig().getCurrentPaths()[0];
        final AraxxorEnvironment.Paths webTwo = fight.getConfig().getCurrentPaths()[1];
        if(object.getId() == webOne.getObjectId() || object.getId() == webTwo.getObjectId()) {
            if(fight.webIsBurning())
                return false;
            fight.getConfig().burnWeb(object.getId());
            return true;
        }

        return false;
    }

    private boolean processObjectClickPhaseThree(WorldObject object) {
        if(object.getId() == 91670) {
            player.setNextWorldTile(fight.getConfig().getRelativeTile(new WorldTile(4561, 6265, 1)));
            fight.stopSuffocate();
            return true;
        }
        return false;
    }

    private boolean processPostFightObjectClick(WorldObject object) {
        if(object.getId() == 91673) {
            AraxxorInterfaces.sendRewardsInterface(player, fight.getReward(player));
            return false;
        } else if(object.getId() == 45803) {
            player.sendOptionsDialogue("Leave the Araxyte cave?", new String[] {
                    "Yes",
                    "No",
            }, new DialogueOptionEvent() {
                @Override
                public void run(Player player) {
                    if(getOption() == OPTION_1) {
                        fight.safeRemovePlayer(player);
                    }
                }
            });
            return false;
        }
        return true;
    }

    private void phaseProcess(int nextX, int nextY) {
        if(fight.getPhase() < 2 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.LIGHT &&
                nextX > fight.getConfig().relativeX(4515) && nextY < fight.getConfig().relativeY(6262))
            fight.nextPhase();
        else if(fight.getPhase() < 2 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.MINIONS &&
                player.getY() > fight.getConfig().relativeY(6270) &&
                player.getX() > fight.getConfig().relativeX(4510))
            fight.nextPhase();
        else if(fight.getPhase() < 2 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.ACID &&
                player.getY() > fight.getConfig().relativeY(6259) && player.getY() < fight.getConfig().relativeY(6271) &&
                player.getX() > fight.getConfig().relativeX(4519))
            fight.nextPhase();
        else if(fight.getPhase() < 3 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.MINIONS &&
                player.getX() > fight.getConfig().relativeX(4567))
            fight.nextPhase();
    }

    @Override
    public boolean processKeyPress(int key) {
        if(readKeyStroke) {
            int correct = ((Araxxor) fight.getBoss()).getSwingDirection().getCorrectKeyStroke();
            if(key == correct) {
                correctKeyStroke = true;
            }
            selectedKey = key;
            readKeyStroke = false;
        }
        return true;
    }

    void allowKeyStroke() {
        readKeyStroke = true;
    }

    boolean checkCorrectKey() {
        boolean correct = correctKeyStroke;
        correctKeyStroke = false;
        return correct;
    }

    Animation getAnimationForKeyPress() {
        return Araxxor.SwingDirection.animForKeyStroke(selectedKey);
    }
}
