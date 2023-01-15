package vazkii.quark.content.building.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.quark.base.handler.advancement.QuarkAdvancementHandler;
import vazkii.quark.base.handler.advancement.QuarkGenericTrigger;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.util.VanillaWoods;
import vazkii.quark.base.util.VanillaWoods.Wood;
import vazkii.quark.content.building.block.HollowLogBlock;

@LoadModule(category = ModuleCategory.BUILDING, hasSubscriptions = true)
public class HollowLogsModule extends QuarkModule {

	private static final String TAG_TRYING_TO_CRAWL = "quark:trying_crawl";

	public static QuarkGenericTrigger crawlTrigger;
	
	@Config
	public static boolean enableAutoCrawl = true;

	@Override
	public void register() {
		for(Wood wood : VanillaWoods.ALL)
			new HollowLogBlock(wood.log(), this, !wood.nether());
		
		crawlTrigger = QuarkAdvancementHandler.registerGenericTrigger("hollow_log_crawl");
	}

	@SubscribeEvent
	public void playerTick(PlayerTickEvent event) {
		if(enableAutoCrawl && event.phase == Phase.START) {
			Player player = event.player;
			boolean isTrying = player.isCrouching() && !player.isSwimming();
			boolean wasTrying = player.getPersistentData().getBoolean(TAG_TRYING_TO_CRAWL);
			
			if(isTrying && !wasTrying) {
				Direction dir = player.getDirection();
				
				BlockPos pos = player.blockPosition().relative(dir);
				BlockState state = player.level.getBlockState(pos);
				Block block = state.getBlock();
				
				if(block instanceof HollowLogBlock) {
					Axis axis = state.getValue(HollowLogBlock.AXIS);
					if(axis != Axis.Y && axis == dir.getAxis()) {
						player.setPose(Pose.SWIMMING);
						player.setSwimming(true);
						
						double x = pos.getX() + 0.5 - ((double) dir.getStepX() * 0.4);
						double y = pos.getY() + 0.13;
						double z = pos.getZ() + 0.5 - ((double) dir.getStepZ() * 0.4);
						
						player.setPos(x, y, z);
					}
				}
			}
			
			if(isTrying != wasTrying)
				player.getPersistentData().putBoolean(TAG_TRYING_TO_CRAWL, isTrying);
		}
	}

}
