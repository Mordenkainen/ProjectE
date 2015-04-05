package moze_intel.projecte.gameObjs.items.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moze_intel.projecte.gameObjs.entity.EntityLootBall;
import moze_intel.projecte.network.PacketHandler;
import moze_intel.projecte.network.packets.SwingItemPKT;
import moze_intel.projecte.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

public class DarkShears extends PEToolBase
{
	public DarkShears()
	{
		super("dm_shears", (byte)2, new String[]{});
		this.setNoRepair();
		this.peToolMaterial = "dm_tools";
		this.pePrimaryToolClass = "shears";
		this.harvestMaterials.add(Material.web);
		this.harvestMaterials.add(Material.cloth);
		this.harvestMaterials.add(Material.plants);
		this.harvestMaterials.add(Material.leaves);
		this.harvestMaterials.add(Material.vine);
	}

	// Only for RedShears
	protected DarkShears(String name, byte numCharges, String[] modeDesc)
	{
		super(name, numCharges, modeDesc);
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase ent)
	{
		if (block.getMaterial() != Material.leaves && block != Blocks.web && block != Blocks.tallgrass && block != Blocks.vine && block != Blocks.tripwire && !(block instanceof IShearable))
		{
			return super.onBlockDestroyed(stack, world, block, x, y, z, ent);
		}
		else
		{
			return true;
		}
	}
	
	@Override
	public boolean canHarvestBlock(Block block, ItemStack stack) 
	{
		return super.canHarvestBlock(block, stack) || block == Blocks.redstone_wire || block == Blocks.tripwire;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (!world.isRemote)
		{
			byte charge = this.getCharge(stack);

			int offset = ((int) Math.pow(2, 2 + charge));
			
			AxisAlignedBB bBox = player.boundingBox.expand(offset, offset / 2, offset);
			List<Entity> list = world.getEntitiesWithinAABB(IShearable.class, bBox);
			
			if (list.isEmpty())
			{
				return stack;
			}
			
			List<ItemStack> drops = new ArrayList();
			
			for (Entity ent : list)
			{
				IShearable target = (IShearable) ent;
				
				if (target.isShearable(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ))
				{
					ArrayList<ItemStack> entDrops = target.onSheared(stack, ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, stack));
					
					if (!entDrops.isEmpty())
					{
						for (ItemStack drop : entDrops)
						{
							drop.stackSize = MathHelper.clamp_int(drop.stackSize + Utils.randomIntInRange(6, 3), 0, 64);
						}
						
						drops.addAll(entDrops);
					}
				}
			}
			
			if (!drops.isEmpty())
			{
				world.spawnEntityInWorld(new EntityLootBall(world, drops, player.posX, player.posY, player.posZ));
				PacketHandler.sendTo(new SwingItemPKT(), (EntityPlayerMP) player);
			}
		}
		
		return stack;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack itemstack, int x, int y, int z, EntityPlayer player)
	{
		if (player.worldObj.isRemote)
		{
			return false;
		}
		
		Block block = player.worldObj.getBlock(x, y, z);
		
		if (block instanceof IShearable)
		{
			IShearable target = (IShearable) block;
			
			if (target.isShearable(itemstack, player.worldObj, x, y, z))
			{
				ArrayList<ItemStack> drops = target.onSheared(itemstack, player.worldObj, x, y, z, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));
				Random rand = new Random();

				for(ItemStack stack : drops)
				{
					float f = 0.7F;
					double d = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d1 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					double d2 = (double)(rand.nextFloat() * f) + (double)(1.0F - f) * 0.5D;
					EntityItem entityitem = new EntityItem(player.worldObj, (double)x + d, (double)y + d1, (double)z + d2, stack);
					entityitem.delayBeforeCanPickup = 10;
					player.worldObj.spawnEntityInWorld(entityitem);
				}

				itemstack.damageItem(1, player);
				player.addStat(StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
			}
		}
	
		return false;
	}
}
