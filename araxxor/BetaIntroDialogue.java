package com.rs.game.player.content.araxxor;

import com.rs.game.player.content.araxxor.npcs.Araxyte;
import com.rs.game.player.dialogue.Dialogue;
import com.rs.utils.Colors;

public class BetaIntroDialogue extends Dialogue {

    private static final int npcId = Araxyte.ARAXXOR_NPC_ID_MELEE;

    @Override
    public void start() {
        stage = 0;
        sendNPCDialogue(npcId, Dialogue.CALM, "Hello, human... I have grown in strength" +
                " quite a bit since the last time we encountered one another...");
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if(stage == 0) {
            stage = 1;
            sendPlayerDialogue(Dialogue.CALM, "Oh really? How so?");
        } else if(stage == 1) {
            stage = 2;
            sendNPCDialogue(npcId, Dialogue.CALM, "Come to the Araxyte cave, and you shall see...");
        } else if(stage == 2) {
            stage = 3;
            sendNPCDialogue(npcId, Dialogue.CALM, "However - because I pity you, I will answer any questions " +
                    "you may have before you fight me.");
        } else if(stage == 3) {
            stage = 4;
            sendOptionsDialogue("Ask Araxxor a question", "What should I be testing?", "What new powers do you have?",
                    "I have no questions - I'm ready.");
        } else if(stage == 4) {
            switch(componentId) {
                case OPTION_1:
                    stage = 25;
                    sendNPCDialogue(npcId, Dialogue.CALM, "I need you to test my abilities " +
                            "- am I performing my specials correctly? " +
                            "Am I walking and targeting correctly?");
                    break;
                case OPTION_2:
                    stage = 50;
                    sendNPCDialogue(npcId, Dialogue.CALM, "I posses many great new abilities... " +
                            "I can Cleave and Cocoon my enemies, my minions are much more powerful, " +
                            "I can heal like never before, and...");
                    break;
                case OPTION_3:
                    stage = 5;
                    sendPlayerDialogue(Dialogue.CALM, "No more questions - I'm ready to fight you.");
                    break;
            }
        } else if(stage == 25) {
            stage = 26;
            sendNPCDialogue(npcId, Dialogue.CALM, "I also need you to test " +
                    "the environment. My creator, dlo3, already knows you can " +
                    "walk slightly past the webs before they are burned down.");
        } else if(stage == 26) {
            stage = 27;
            sendNPCDialogue(npcId, Dialogue.CALM, "Note everything you can " +
                    "about how I fight... That way I can perfect my technique!");
        } else if(stage == 27) {
            stage = 3;
            sendNPCDialogue(npcId, Dialogue.CALM, "Also note everything you can about the " +
                    "fight phase progression. If anything isn't happening correctly, make a note and" +
                    " report it to my creator...");
        } else if(stage == 50) {
            stage = 51;
            sendNPCDialogue(npcId, Dialogue.CALM, "... if you follow the bottom path, I will make" +
                    " you truly face the darkness of the Araxyte cave.");
        } else if(stage == 51) {
            stage = 52;
            sendNPCDialogue(npcId, Dialogue.CALM, "When I charge you, you'll have to use the " +
                    Colors.RED + "arrow keys</col>" + " to dodge my charges. If my legs go to the left," +
                    "dodge left. If my legs go to the right, dodge right...");
        } else if(stage == 52) {
            stage = 3;
            sendNPCDialogue(npcId, Dialogue.CALM, "... if my legs are about to throw you upward, jump over me. If my legs " +
                    "are about to slam down on you, duck underneath me...");
        } else if(stage == 5) {
            stage = 6;
            sendNPCDialogue(npcId, Dialogue.CALM, "Very well, human... Use the command " +
                    Colors.RED + "::araxxorbeta</col> to start a solo fight, or create a group fight. Good luck...");
        } else if(stage == 6) {
            sendPlayerDialogue(Dialogue.CALM, "See you there!");
            stage = 7;
        } else if (stage == 7) end();
    }

    @Override
    public void finish() {

    }
}
