package com.rs.game.player.content.araxxor;

import java.util.ArrayList;
import java.util.Collections;

import com.rs.game.World;
import com.rs.game.player.Player;
import com.rs.game.player.dialogue.Dialogue;
import com.rs.utils.Colors;
import com.rs.utils.InputStringEvent;

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
public class AraxxorStartDialogue extends Dialogue {

    @Override
    public void start() {
        sendDialogue(Colors.RED + ("Beyond this point is the Araxyte hive."),
                     Colors.RED + ("There is no way out other than death or Victory."),
                     Colors.RED + ("Only those who can endure dangerous encounters should proceed."));
        stage = 0;

    }

    @Override
    public void run(int interfaceId, int componentId) {
        if(stage == 0) {
            stage = 1;
            sendOptionsDialogue("What would you like to do?",
                    "Enter the Araxyte Cave.",
                    "Start a custom session.");

        } else if(stage == 1) {
            switch (componentId) {
                case OPTION_1:
                    AraxxorManager.generateFight(new ArrayList<>(Collections.singletonList(player)));
                    end();
                    break;
                case OPTION_2:
                    player.sendInputString("Who would you like to " +
                            "invite to your group?", new InputStringEvent() {
                        @Override
                        public void run(Player player) {
                            String username = getString().toLowerCase();
                            Player groupMember = World.getPlayer(username);
                            if(groupMember != null) {
                                if(groupMember == player) {
                                    AraxxorManager.generateFight(new ArrayList<>(Collections.singletonList(player)));
                                    return;
                                }
                                groupMember.getDialogueManager().startDialogue("AcceptAraxxorD", player);
                                player.sendMessage("Sending " + groupMember.getDisplayName() + " an invite...");
                            }
                            else player.sendMessage("No player " + username + " is online.");
                        }
                    });
                    end();
                    break;
            }
        }
    }

    @Override
    public void finish() {

    }
}
