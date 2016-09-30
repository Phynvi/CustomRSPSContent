package com.rs.game.minigames.pyramidplunder;

/**
 * Enumeration of all the Pyramid Plunder floors
 * Contains chunk information for mapping, level requirement for entry,
 * 		and a reference number for telling which floor we are dealing with 
 * @author dlo
 *
 */
enum PPFloors 
{
	FLOOR_1(1, 21, new int[]{248, 555}, new int[]{245, 552}),
	FLOOR_2(2, 31, new int[]{245, 555}, new int[]{242, 552}),
	FLOOR_3(3, 41, new int[]{242, 555}, new int[]{239, 552}),
	FLOOR_4(4, 51, new int[]{243, 558}, new int[]{240, 555}),
	FLOOR_5(5, 61, new int[]{246, 560}, new int[]{243, 557}),
	FLOOR_6(6, 71, new int[]{244, 558}, new int[]{240, 554}),
	FLOOR_7(7, 81, new int[]{246, 558}, new int[]{243, 555}),
	FLOOR_8(8, 91, new int[]{243, 560}, new int[]{240, 557});
	
	private int floorNumber;
	private int levelRequired;
	private int[] northWestChunks;
	private int[] southEastChunks;
	
	private int dimension;
	
	private PPFloors(int floorNumber, int lvlReq, int[] northWestChunks, int[] southEastChunks)
	{
		this.floorNumber = floorNumber;
		this.levelRequired = lvlReq;
		this.northWestChunks = northWestChunks;
		this.southEastChunks = southEastChunks;
	}
	public int getFloorNumber()
	{
		return floorNumber;
	}
	public int getLevelRequired()
	{
		return levelRequired;
	}
	public int[] getNorthWestChunks()
	{
		return northWestChunks;
	}
	public int[] getSouthEastChunks()
	{
		return southEastChunks;
	}
	public int getDimension()
	{
		this.dimension = Math.abs(northWestChunks[0] - southEastChunks[0]);
		return dimension;
	}
}

