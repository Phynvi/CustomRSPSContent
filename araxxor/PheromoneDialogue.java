package com.rs.game.player.content.araxxor;

import com.rs.game.Graphics;
import com.rs.game.player.dialogue.Dialogue;

import java.util.Arrays;


final class PheromoneDialogue extends Dialogue {

    @Override
    public void start() {
        stage = 0;
        sendDialogue("The Araxytes are enraged at " +
                player.getAraxxorEnrage() + "%. Do you want to release" +
                " the pheromone to calm them down?");
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if(stage == 0) {
            stage = 1;
            sendOptionsDialogue("Release Pheromone?", "Yes", "No");
        } else if(stage == 1) {
            if(componentId == OPTION_1) {
                player.setNextGraphics(new Graphics(1));
                player.removeAllAraxxorEnrage();
                player.getPackets().sendPlayerMessageBox("The Araxytes are no longer " +
                        "enraged with you.");
            }
            end();
        }
        Arrays.asList(1);
    }

    @Override
    public void finish() {

    }
}
