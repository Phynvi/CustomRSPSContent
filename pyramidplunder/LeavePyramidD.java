package com.rs.game.minigames.pyramidplunder;

import com.rs.Settings;
import com.rs.game.player.dialogues.Dialouge;

public class LeavePyramidD extends Dialogue
{
	private Game game;

	@Override
	public void start() {
		
		stage = 0;
		game = (Game) parameters[0];
		sendOptionsDialogue("Leave the Pyramid?", "Yes, leave.", "No, don't leave.");

	}

	@Override
	public void run(int interfaceId, int componentId) {
		
		if(stage == 0)
		{
			if(componentId == OPTION_1)
			{
				player.getControlerManager().getControler().removeControler();
				player.setNextWorldTile(Settings.HOME_LOCATION);
				game.getCurrentFloor().destroyFloor(game.getCurrentFloor().getFloorPlan());
				end();

			}
			else if(componentId == OPTION_2)
			{
				player.getPackets().sendGameMessage("You choose to stay and continue looting treasure.");
				end();
			}
		}

	}

	@Override
	public void finish() {	}

}
