package me.cmesh.SimpleAxe;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AxeListener implements Listener {
	
	public AxeListener() {
	}
	
	private boolean canBreakBlock(ItemStack tool, Block block) {
		return tool.getMaxStackSize() == 1 && tool.getType().name().contains("AXE") && !block.getDrops(tool).isEmpty();
	}
	
	private int breakAround(ItemStack tool, Location loc) {
		int total = 0;
		loc = loc.add(-1, -1, -1);
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 3; k++){
					if (i == 1 && k == 1 && j == 1) {
						continue;
					}
					Block target = loc.clone().add(i, j, k).getBlock();
					if (canBreakBlock(tool, target) && target.getType() != Material.BEDROCK && target.getType() != Material.PORTAL) {
						target.breakNaturally(tool);
						total++;
					}
				}
			}
		}
		return total;
	}
	
	private boolean breakTree(ItemStack tool, Block block) {
		Material type = block.getType();
		if (type == Material.LOG || type == Material.LOG_2) {
			Block up = block.getRelative(BlockFace.UP);
			BlockFace[] faces = {
					BlockFace.SELF,
					BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST,
					BlockFace.SOUTH, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
					BlockFace.EAST, BlockFace.WEST,
			};
			for (BlockFace face : faces) {
				Block curr = up.getRelative(face);
				if (!breakTree(tool, curr)) {
					return false;
				}
				curr = up.getRelative(face, 2);
				if (!breakTree(tool, curr)) {
					return false;
				}
			}
			if (tool.getDurability() < tool.getType().getMaxDurability()) {
				int damage = (tool.getDurability() + 1);
				tool.setDurability((short) damage);
				block.breakNaturally();
				return true;
			} else {
				return false;
			}
		}
		return true;
	}
	
	private static String Special = "Activated";
	
	private boolean isToolActivated(ItemStack item) {
		ItemMeta data = item.getItemMeta();
		List<String> lore = data.getLore();
		return lore != null && lore.contains(Special);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerRightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
				player.isSneaking() && player.getItemInHand().getType().name().contains("PICK")
			) {
			ItemMeta data = player.getItemInHand().getItemMeta();
			List<String> lore = data.getLore();
			
			if (lore == null) {
				lore = new ArrayList<String>();
			}
			
			if (lore.contains(Special)) {
				lore.remove(Special);
				player.sendMessage("This tool returns to normal");
			} else {
				lore.add(Special);
				player.sendMessage("This tool quivers with a strange power");
			}
			data.setLore(lore);
			player.getItemInHand().setItemMeta(data);
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block target = event.getClickedBlock().getRelative(event.getBlockFace());
			
			if (target.getType() == Material.AIR && player.getItemInHand().getType().name().contains("AXE")) {
				int index = player.getInventory().getHeldItemSlot() + 1;
				ItemStack items = player.getInventory().getItem(index);
				if (items != null && items.getType() == Material.TORCH) {
					target.setType(Material.TORCH);
					items.setAmount(items.getAmount() - 1);
				}
			}
		} 
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onBlockDamage(org.bukkit.event.block.BlockBreakEvent event)
	{
		Player player = event.getPlayer();
		Block block = event.getBlock();
		ItemStack tool = player.getItemInHand();
		if (canBreakBlock(tool, block)) {
			if (tool.getType().name().contains("PICK")) {
				if (isToolActivated(tool)) {
					int total = breakAround(tool, block.getLocation());
					if (total > 0) {
						int damage = (player.getItemInHand().getDurability() + total);
						
						int repair_per_ingot = player.getItemInHand().getType().getMaxDurability()/3;
						if (damage > repair_per_ingot) {
							Material mat = Material.AIR;
							switch (tool.getType()) {
								case IRON_PICKAXE:
									mat = Material.IRON_INGOT;
									break;
								case GOLD_PICKAXE:
									mat = Material.GOLD_INGOT;
									break;
								case DIAMOND_PICKAXE:
									mat = Material.DIAMOND;
									break;
								default:
									break;		
							}
							ItemStack ingot = new ItemStack(mat, 1);
							if (player.getInventory().containsAtLeast(ingot, 1)) {
								player.getInventory().removeItem(ingot);
								damage = damage - repair_per_ingot;
							}
						}
						
						if (damage <= player.getItemInHand().getType().getMaxDurability()) { 
							player.getItemInHand().setDurability((short) damage);
						} else {
							player.setItemInHand(null);
						}
					}
				}
			} else {
				if (!breakTree(tool, block)) {
					player.setItemInHand(null);
				}
			}
		}
	}
}