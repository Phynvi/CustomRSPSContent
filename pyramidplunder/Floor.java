package com.rs.game.minigames.pyramidplunder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.rs.game.RegionBuilder;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;
/**
 * 
 * A Pyramid Plunder {@code Floor} object.<br><br>
 * The {@code Floor} is responsible for mapping physical information about
 * the current floor the player is on, as well as destroying old map
 * information when a player transitions between floors.
 * 
 * @author David
 *
 */
public class Floor 
{
	/** The {@link Player} running this floor. {@code transient} so as to avoid serializing issues. */
	private transient Player player;
	/** An {@code int} array containing map chunk information about this floor. */
	private int[] boundChunks;
	/** The {@link PPFloors} enumerator encapsulating relevant information about this floor. */
	private PPFloors floorPlan;
	/** The start location of this floor. */
	private WorldTile startTile;
	
	/**
	 * Instantiates a {@code Floor} object, generates the map, and sends the player tot he start tile.
	 * @param player
	 * @param floorPlan
	 */
	public Floor(Player player, PPFloors floorPlan)
	{
		this.player = player;
		this.floorPlan = floorPlan;
		generateFloor(this.floorPlan);
	}
	
	/**
	 * Accessor method to retrieve the player running the floor.
	 * @return {@code player}
	 */
	public Player getPlayer()
	{
		return player;
	}
	
	/**
	 * Accessor method to retrieve the floor plan enumerator
	 * @return {@code floorPlan}
	 */
	public PPFloors getFloorPlan()
	{
		return floorPlan;
	}
	
	/**
	 * Destroy the supplied floor plan chunks
	 * @param floorPlan
	 */
	public void destroyFloor(final PPFloors floorPlan)
	{
		try
		{
			Executors.newSingleThreadScheduledExecutor().schedule(
					() -> 
					{
						RegionBuilder.destroyMap(boundChunks[0], boundChunks[1],
								floorPlan.getDimension(), floorPlan.getDimension());
					}, 1200, TimeUnit.MILLISECONDS
			).get();
		}
		catch(InterruptedException | ExecutionException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Generate the floor based on the supplied floor plan
	 * @param floorPlan
	 */
	private void generateFloor(final PPFloors floorPlan)
	{
		Executors.newSingleThreadScheduledExecutor().execute(() -> 
		{
			
			boundChunks = RegionBuilder.findEmptyChunkBound(floorPlan.getDimension(), floorPlan.getDimension());
			
			RegionBuilder.copyAllPlanesMap(floorPlan.getNorthWestChunks()[0], floorPlan.getNorthWestChunks()[1], 
					boundChunks[0], boundChunks[1], floorPlan.getDimension());
			RegionBuilder.copyAllPlanesMap(floorPlan.getSouthEastChunks()[0], floorPlan.getSouthEastChunks()[1], 
					boundChunks[0], boundChunks[1], floorPlan.getDimension());
			
			initializeStartTile(floorPlan);
			
			player.setNextWorldTile(startTile);
			
		});
	}
	
	/**
	 * Initializes the player start tile based on the floor plan
	 * @param floorPlan
	 */
	private void initializeStartTile(PPFloors floorPlan)
	{
		switch(floorPlan)
		{
			case FLOOR_1:
				startTile = new WorldTile(boundChunks[0] * 8 + 15, boundChunks[1] * 8 + 4, 0);
				break;
			case FLOOR_2:
				startTile = new WorldTile(boundChunks[0] * 8 + 7, boundChunks[1] * 8 + 5, 0);
				break;
			case FLOOR_3:
				startTile = new WorldTile(boundChunks[0] * 8 + 16, boundChunks[1] * 8 + 9, 0);
				break;
			case FLOOR_4:
				startTile = new WorldTile(boundChunks[0] * 8 + 6, boundChunks[1] * 8 + 12, 0);
				break;
			case FLOOR_5:
				startTile = new WorldTile(boundChunks[0] * 8 + 10, boundChunks[1] * 8 + 20, 0);
				break;
			case FLOOR_6:
				startTile = new WorldTile(boundChunks[0] * 8 + 5, boundChunks[1] * 8 + 20, 0);
				break;
			case FLOOR_7:
				startTile = new WorldTile(boundChunks[0] * 8 + 21, boundChunks[1] * 8 + 4, 0);
				break;
			case FLOOR_8:
				startTile = new WorldTile(boundChunks[0] * 8 + 7, boundChunks[1] * 8 + 20, 0);
				break;
		}
	}
}
