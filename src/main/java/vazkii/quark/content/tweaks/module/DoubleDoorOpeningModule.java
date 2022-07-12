package vazkii.quark.content.tweaks.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.quark.base.Quark;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.DoubleDoorMessage;

@LoadModule(category = ModuleCategory.TWEAKS, hasSubscriptions = true, subscribeOn = Dist.CLIENT)
public class DoubleDoorOpeningModule extends QuarkModule {

	public static TagKey<Block> nonDoubleDoorTag;
	
	@Override
	public void setup() {
		nonDoubleDoorTag = BlockTags.create(new ResourceLocation(Quark.MOD_ID, "non_double_door"));
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
		if(!event.getLevel().isClientSide || event.getEntity().isDiscrete() || event.isCanceled() || event.getResult() == Result.DENY || event.getUseBlock() == Result.DENY)
			return;

		Level world = event.getLevel();
		BlockPos pos = event.getPos();

		if(isDoor(world.getBlockState(pos))) {
			openDoor(world, event.getEntity(), pos);
			QuarkNetwork.sendToServer(new DoubleDoorMessage(pos));
		}
	}

	public static void openDoor(Level world, Player player, BlockPos pos) {
		if(!ModuleLoader.INSTANCE.isModuleEnabled(DoubleDoorOpeningModule.class) || world == null)
			return;

		BlockState state = world.getBlockState(pos);
		if(!isDoor(state))
			return;
		
		Direction direction = state.getValue(DoorBlock.FACING);
		boolean isOpen = state.getValue(DoorBlock.OPEN);
		DoorHingeSide isMirrored = state.getValue(DoorBlock.HINGE);

		BlockPos mirrorPos = pos.relative(isMirrored == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise());
		BlockPos doorPos = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? mirrorPos : mirrorPos.below();
		BlockState other = world.getBlockState(doorPos);

		if(state.getMaterial() != Material.METAL && other.getBlock() == state.getBlock() && other.getValue(DoorBlock.FACING) == direction && other.getValue(DoorBlock.OPEN) == isOpen && other.getValue(DoorBlock.HINGE) != isMirrored) {
			BlockHitResult res = new BlockHitResult(new Vec3(doorPos.getX() + 0.5, doorPos.getY() + 0.5, doorPos.getZ() + 0.5), direction, doorPos, false);
			if(res.getType() == HitResult.Type.BLOCK)
				other.use(world, player, InteractionHand.MAIN_HAND, res);
		}
	}
	
	private static boolean isDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock && !state.is(nonDoubleDoorTag);
	}

}
