package com.rs.game.minigames.pyramidplunder;

import com.rs.game.Animation;
import com.rs.game.Hit;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.Hit.HitLook;
import com.rs.game.player.Player;
import com.rs.game.player.Skills;
import com.rs.game.player.content.FadingScreen;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

/**
 * 
 * A Pyramid Plunder {@code Game} object.<br><br>
 * The {@code Game} object holds all of the game logic, and is aware
 * of the current {@link Floor} the player is running.
 * @author David
 *
 */
public class Game 
{
	/** The {@link Player} running this floor. {@code transient} so as to avoid serializing issues. */
	private transient Player player;
	/** The current {@link Floor} the player is on. */
	private Floor currentFloor;
	/** The number of attempts the player has made to move on to the next floor. */
	private byte doorAttempts;
	
	/* 
	 * Animation and WorldObject ids
	 * ...
	 * ...
	 * ... 
	 */
	
	private static final int PICK_LOCK = 881;
	
	private static final int PASS_TRAP = 2246;
	private static final int SPEARS = 64;
	
	private static final int URNSEARCH_START = 4340;
	private static final int URNSEARCH_SUCCESS = 4342;
	private static final int URNSEARCH_FAIL = 4341;
	
	private static final int SEARCH_CHEST = 4238;
	
	private static final int SEARCHED_URN1 = 16505;
	private static final int SEARCHED_URN2 = 16506;
	private static final int SEARCHED_URN3 = 16507;
	
	private static final int SNAKE_URN1 = 16509;
	private static final int SNAKE_URN2 = 16510;
	private static final int SNAKE_URN3 = 16511;
	
	private static final int OPEN_CHEST = 16474;
	
	private static final int DEAD_END = 16460;
	
	/**
	 * Assign the player to this {@code Game} object and instantiate
	 * the first {@link Floor}.
	 * @param player : the player of this {@code Game}
	 */
	public Game(Player player)
	{
		this.player = player;
		currentFloor = new Floor(this.player, PPFloors.FLOOR_1);
	}
	
	/**
	 * Accessor method to retrieve the player running the floor in this {@code Game}.
	 * @return {@code player}
	 */
	public Player getPlayer()
	{
		return player;
	}
	/**
	 * Accessor method to retrieve the current {@link Floor} of this {@code Game}.
	 * @return {@code currentFloor}
	 */
	public Floor getCurrentFloor()
	{
		return currentFloor;
	}
	
	/**
	 * Dictates what happens when the {@code player} interacts with an object in the environment
	 * @param object : the {@link WorldObject} being interacted with
	 * @return false : This method is called from a controller and therefore overrides normal game behavior
	 */
	public boolean processObjectInteraction(WorldObject object)
	{
		if(object.getDefinitions().name.equals("Tomb Door"))
			leavePyramid(player);
		else if(object.getDefinitions().name.equals("Speartrap"))
			passTrap(player, object);
		else if(object.getId() == 16501 || 
				object.getId() == 16518 || 
				object.getId() == 16519 || 
				object.getId() == 16520 || 
				object.getId() == 16521 || 
				object.getId() == 16522 || 
				object.getId() == 16502 || 
				object.getId() == 16523 || 
				object.getId() == 16524 || 
				object.getId() == 16525 || 
				object.getId() == 16526 || 
				object.getId() == 16527 || 
				object.getId() == 16503 ||
				object.getId() == 16528 ||
				object.getId() == 16529 ||
				object.getId() == 16530 ||
				object.getId() == 16531 ||
				object.getId() == 16532)
			searchUrn(player, object);
		else if(object.getId() == 16537)
			searchChest(player, object);
		else if(object.getId() == 16542 || 
				object.getId() == 16540 || 
				object.getId() == 16539 || 
				object.getId() == 16541 )
			openDoor(player, object);
		else if(object.getId() == OPEN_CHEST || 
				object.getId() == SNAKE_URN1 || 
				object.getId() == SNAKE_URN2 || 
				object.getId() == SNAKE_URN3 || 
				object.getId() == SEARCHED_URN1 || 
				object.getId() == SEARCHED_URN2 || 
				object.getId() == SEARCHED_URN3 )
			player.getPackets().sendGameMessage("You already searched that!");

		return false;
	}

	/**
	 * Prompts the player with a dialogue to leave the pyramid plunder game.
	 * @param player
	 */
	private void leavePyramid(Player player)
	{
		player.getDialogueManager().startDialogue("LeavePyramid", this);
	}
	
	/**
	 * Handles the player passing through a spear trap.
	 * @param player
	 * @param trap {@code instanceof WorldObject}
	 */
	private void passTrap(Player player, WorldObject trap)
	{
		WorldTasksManager.schedule(new WorldTask(){

			int loop = 0;
			boolean success = false;
			
			@Override
			public void run() {

				if(loop == 0) {
					player.lock();
					player.getPackets().sendGameMessage("You attempt to sneak past the trap...");
					player.setNextAnimation(new Animation(PASS_TRAP));
				} else if(loop == 1) {
					if(calculateSuccess(player)) {
						success = true;
						if(trap.getX() == player.getX()) {
							if(trap.getY() > player.getY()) {
								player.addWalkSteps(player.getX(), player.getY() + 3, 3, false);
							} else {
								player.addWalkSteps(player.getX(), player.getY() - 3, 3, false);
							}
						} else if (trap.getY() == player.getY()){
							if(trap.getX() > player.getX()) {
								player.addWalkSteps(player.getX() + 3, player.getY(), 3, false);
							} else {
								player.addWalkSteps(player.getX() - 3, player.getY(), 3, false);
							}
						} else if(trap.getY() > player.getY()) {
							player.addWalkSteps(player.getX(), player.getY() + 3, 3, false);
						}
						player.getPackets().sendGameMessage("... and you manage to squeeze by undeteced.");
					} else {
						
						
						player.getPackets().sendGameMessage("You fail to get past the trap!");
					}
				} else if(loop == 2) {
					
					if(!success) {
						World.sendObjectAnimation(trap, new Animation(SPEARS));
						player.applyHit(new Hit(player, 50,
								HitLook.REGULAR_DAMAGE));
					}

					player.unlock();
					stop();
				}
				loop++;
			}
			
		}, 0, 2);
	}
	
	/**
	 * Checks if the player can continue to the next floor, and handles that interaction.
	 * @param player
	 */
	private void changeFloor(Player player)
	{
		PPFloors nextFloor = null;
		boolean canAdvance = false;
		switch(currentFloor.getFloorPlan())
		{
			case FLOOR_1:
				nextFloor = PPFloors.FLOOR_2;
				break;
			case FLOOR_2:
				nextFloor = PPFloors.FLOOR_3;
				break;
			case FLOOR_3:
				nextFloor = PPFloors.FLOOR_4;
				break;
			case FLOOR_4:
				nextFloor = PPFloors.FLOOR_5;
				break;
			case FLOOR_5:
				nextFloor = PPFloors.FLOOR_6;
				break;
			case FLOOR_6:
				nextFloor = PPFloors.FLOOR_7;
				break;
			case FLOOR_7:
				nextFloor = PPFloors.FLOOR_8;
				break;
			default:
				break;
		}
		
		if(nextFloor != null)
			if(player.getSkills().getLevel(Skills.THIEVING) >= nextFloor.getLevelRequired())
				canAdvance = true;
		
		if(nextFloor == null)
		{
			player.getPackets().sendGameMessage("You are on the highest floor of the Pyramid!");
			return;
		}
		
		if(!canAdvance)
			return;
			
		FadingScreen.fade(player, new Runnable(){
			
			
			@Override
			public void run() {
				
				currentFloor.destroyFloor(currentFloor.getFloorPlan());
				switch(currentFloor.getFloorPlan())
				{
					case FLOOR_1:
						currentFloor = new Floor(player, PPFloors.FLOOR_2);
						doorAttempts = 0;
						break;
					case FLOOR_2:
						currentFloor = new Floor(player, PPFloors.FLOOR_3);
						doorAttempts = 0;
						break;
					case FLOOR_3:
						currentFloor = new Floor(player, PPFloors.FLOOR_4);
						doorAttempts = 0;
						break;
					case FLOOR_4:
						currentFloor = new Floor(player, PPFloors.FLOOR_5);
						doorAttempts = 0;
						break;
					case FLOOR_5:
						currentFloor = new Floor(player, PPFloors.FLOOR_6);
						doorAttempts = 0;
						break;
					case FLOOR_6:
						currentFloor = new Floor(player, PPFloors.FLOOR_7);
						doorAttempts = 0;
						break;
					case FLOOR_7:
						currentFloor = new Floor(player, PPFloors.FLOOR_8);
						doorAttempts = 0;
						break;
					default:
						break;
				}
			}
			
		});
	}
	
	/**
	 * Activates a door attempt.<br><br>
	 * If the attempt fails, the door disappears. If the attempt succeeds,
	 * {@code changeFloor(player)} is called.
	 * @param player
	 * @param door {@code instanceof WorldObject}
	 */
	private void openDoor(Player player, WorldObject door)
	{
		if(Math.random() < 0.50 && doorAttempts < 2)
		{
			doorAttempts++;
			WorldTasksManager.schedule(new WorldTask(){

				int loop = 0;
				
				@Override
				public void run() {

					if(loop == 0) {
						player.lock();
						player.setNextAnimation(new Animation(PICK_LOCK));
						player.getPackets().sendGameMessage("You attempt to pick the lock...");
					} else if(loop == 1){
						player.unlock();
						player.getPackets().sendGameMessage("...and you realize that it leads to a dead end.");
						World.spawnObject(new WorldObject(DEAD_END, 0, door.getRotation(), 
								door.getX(), door.getY(), door.getPlane()));
						stop();
					}
					loop++;
				}
				
			}, 0, 1);
		}
		else 
		{
			player.getPackets().sendGameMessage("You move on to the next floor.");
			changeFloor(player);
		}
	}
	
	/**
	 * Searches a chest for loot and updates the map textures accordingly.
	 * @param player
	 * @param chest
	 */
	private void searchChest(Player player, WorldObject chest)
	{
		WorldTasksManager.schedule(new WorldTask() {
			
			int loop = 0;
			
			@Override
			public void run() {

				if (loop == 0) {
					player.lock();
					player.setNextAnimation(new Animation(SEARCH_CHEST));
					player.getPackets().sendGameMessage("You search the chest for treasures...");
					spawnNewChest(chest);
				} else if (loop == 1) {
					if(calculateSuccess(player)) {
						player.getPackets().sendGameMessage("You find some treasure in the chest!");
						Reward.giveReward(player, Reward.ChestRewards.values());
					} else {
						player.getPackets().sendGameMessage("...but you find nothing.");
					}
					player.unlock();
					stop();
				} 
				loop++;
			}
			
		}, 0, 1);	
	}
	
	/**
	 * Searches an urn for loot and updates the map textures accordingly.
	 * @param player
	 * @param urn
	 */
	private void searchUrn(Player player, WorldObject urn)
	{
		WorldTasksManager.schedule(new WorldTask() {
			
			int loop = 0;
			boolean success = false;
			
			@Override
			public void run() {

				if (loop == 0) {
					player.lock();
					player.setNextAnimation(new Animation(URNSEARCH_START));
					player.getPackets().sendGameMessage("You search the urn for treasures...");
				} else if (loop == 1) {
					if(calculateSuccess(player)) {
						success = true;
						spawnNewUrn(urn, false);
						player.setNextAnimation(new Animation(URNSEARCH_SUCCESS));
						player.getPackets().sendGameMessage("You find some treasure!");
					} else {
						spawnNewUrn(urn, true);
						player.setNextAnimation(new Animation(URNSEARCH_FAIL));
						player.getPackets().sendGameMessage("...and a "
								+ "snake leaps out from the urn and bites you!");
						player.applyHit(new Hit(player, 50,
								HitLook.REGULAR_DAMAGE));
					}
				} else if (loop == 2) {
					if(success)
						Reward.giveReward(player, Reward.UrnRewards.values());
					player.unlock();
					stop();
				}
				loop++;
			}
			
		}, 0, 2);
	}
	
	/**
	 * Determines whether or not a player will succeed in looting an object.
	 * @param player
	 * @return whether or not the loot is successful
	 */
	private boolean calculateSuccess(Player player)
	{
		double chance = 0.4;
		int levelReq = currentFloor.getFloorPlan().getLevelRequired();
		int expertise = player.getSkills().getLevel(Skills.THIEVING) - levelReq;
		if(expertise <= 3 && expertise > 0)
			chance+=0.1;
		if(expertise <=6 && expertise > 3)
			chance+=0.1;
		if(expertise <= 9 && expertise > 6)
			chance+=0.1;
		if(expertise > 9)
			chance+=0.2;
		if(Math.random() < chance)
			return true;
		return false;
	}
	
	/**
	 * Spawns a new chest according to the loot success or failure
	 * @param chest
	 */
	private void spawnNewChest(WorldObject chest)
	{
		World.spawnObject(new WorldObject(OPEN_CHEST, 
				10, chest.getRotation(), chest.getX(), 
				chest.getY(), chest.getPlane()));
	}
	
	/**
	 * Spawns a new urn according to the loot success or failure
	 * @param urn
	 */
	private void spawnNewUrn(WorldObject urn, boolean fail)
	{
		if(	urn.getId() == 16501 || 
			urn.getId() == 16518 || 
			urn.getId() == 16519 || 
			urn.getId() == 16520 || 
			urn.getId() == 16521 || 
			urn.getId() == 16522 )
		{
			World.spawnObject(new WorldObject(fail ? SNAKE_URN1 : SEARCHED_URN1, 
					10, urn.getRotation(), urn.getX(), 
					urn.getY(), urn.getPlane()));
		}
		else if(urn.getId() == 16502 || 
				urn.getId() == 16523 || 
				urn.getId() == 16524 || 
				urn.getId() == 16525 || 
				urn.getId() == 16526 || 
				urn.getId() == 16527 )
		{
			World.spawnObject(new WorldObject(fail ? SNAKE_URN2 : SEARCHED_URN2, 
					10, urn.getRotation(), urn.getX(), 
					urn.getY(), urn.getPlane()));
		}
		else if(urn.getId() == 16503 || 
				urn.getId() == 16528 || 
				urn.getId() == 16529 || 
				urn.getId() == 16530 || 
				urn.getId() == 16531 || 
				urn.getId() == 16532 )
		{
			World.spawnObject(new WorldObject(fail ? SNAKE_URN3 : SEARCHED_URN3, 
					10, urn.getRotation(), urn.getX(), 
					urn.getY(), urn.getPlane()));
		}
	}
}
