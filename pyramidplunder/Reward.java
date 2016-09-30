package com.rs.game.minigames.pyramidplunder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rs.game.World;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

/**
 * 
 * A Pyramid Plunder Rewards utility class.
 * <br><br>
 * The {@code Reward} class is responsible for giving rewards to players
 * when they successfully loot a Pyramid Plunder game object.
 * @author David
 *
 */
public final class Reward 
{
	
	/**
	 * 
	 * Simple interface for polymorphic calls.
	 * @author David
	 *
	 */
	interface Rewards
	{
		Item getItem();
		int getWeight();
	}
	
	/**
	 * The urn rewards.
	 * @author David
	 *
	 */
	enum UrnRewards implements Rewards
	{
		IVORY_COMB(new Item(9026, 1), 100),
		STONE_SEAL(new Item(9042,0), 50),
		GOLD_SEAL(new Item(9040,0), 15),
		POTTERY_SCARAB(new Item(9032,0), 75),
		GOLDEN_SCARAB(new Item(9028,0), 15),
		POTTERY_STATUETTE(new Item(9036,0), 75),
		STONE_STATUETTE(new Item(9038,0), 50),
		GOLDEN_STATUETTE(new Item(9034,0), 15),
		JEWELLED_GOLDEN_STATUETTE(new Item(20661,0), 3);

		Item item; 
		int weight;
		
		UrnRewards(Item item, int weight)
		{
			this.item = item;
			this.weight = weight;
		}
		
		@Override
		public Item getItem() {
			return item;
		}

		@Override
		public int getWeight() {
			return weight;
		}
		
	}
	
	/**
	 * The chest rewards.
	 * @author David
	 *
	 */
	enum ChestRewards implements Rewards
	{
		IVORY_COMB(new Item(9026, 1), 100),
		STONE_SEAL(new Item(9042,0), 50),
		GOLD_SEAL(new Item(9040,0), 15),
		POTTERY_SCARAB(new Item(9032,0), 75),
		GOLDEN_SCARAB(new Item(9028,0), 15),
		POTTERY_STATUETTE(new Item(9036,0), 75),
		STONE_STATUETTE(new Item(9038,0), 50),
		GOLDEN_STATUETTE(new Item(9034,0), 15),
		JEWELLED_GOLDEN_STATUETTE(new Item(20661,0), 3),
		COINS(new Item(995, 5000000), 1),
		JEWELED_DIAMOND_STATUETTE(new Item(21570, 1), 1),
		SCEPTRE(new Item(9050, 1), 1);
		
		Item item;
		int weight;

		ChestRewards(Item item, int weight)
		{
			this.item = item;
			this.weight = weight;
		}
		
		@Override
		public Item getItem() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getWeight() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	/**
	 * Selects a reward from a weighted list of rewards and gives it to the player.
	 * <br><br>Also sends a world message to all players if the reward is of high rarity.
	 * @param player
	 * @param values
	 */
	protected static void giveReward(Player player, Rewards[] values)
	{
		List<Item> filler = new ArrayList<>();
		
		Item reward = null;
		
		for(Rewards _reward : values)
		{
			for(int i = 0; i < _reward.getWeight(); i++)
			{
				filler.add(_reward.getItem());
			}
		}
		
		Collections.shuffle(filler);
		
		reward = filler.get(Utils.randomInt(0, filler.size() - 1));
		player.getInventory().addItem(reward);
		
		if(reward.getId() == UrnRewards.JEWELLED_GOLDEN_STATUETTE.getItem().getId())
			World.sendWorldMessage("<col=ffffff>[<col><col=bf00ff>Announcement</col><col=ffffff>]</col>: "
					+ "<col=ffffff>" + player.getDisplayName() +"<col=ffffff> "
							+ "just looted a Jewelled Golden Statuette from Pyramid Plunder!<col>", false);
		else if(reward.getId() == ChestRewards.SCEPTRE.getItem().getId())
			World.sendWorldMessage("<col=ffffff>[<col><col=bf00ff>Announcement</col><col=ffffff>]</col>: "
					+ "<col=ffffff>" + player.getDisplayName() +"<col=ffffff> "
							+ "just looted the Pharaoh's Sceptre from Pyramid Plunder!<col>", false);
		else if(reward.getId() == ChestRewards.COINS.getItem().getId())
			World.sendWorldMessage("<col=ffffff>[<col><col=bf00ff>Announcement</col><col=ffffff>]</col>: "
					+ "<col=ffffff>" + player.getDisplayName() +"<col=ffffff> "
							+ "just looted 5M coins from Pyramid Plunder!<col>", false);
		else if(reward.getId() == ChestRewards.JEWELED_DIAMOND_STATUETTE.getItem().getId())
			World.sendWorldMessage("<col=ffffff>[<col><col=bf00ff>Announcement</col><col=ffffff>]</col>: "
					+ "<col=ffffff>" + player.getDisplayName() +"<col=ffffff> "
							+ "just looted a Jewelled Diamond Statuette from Pyramid Plunder!<col>", false);
	}

}

