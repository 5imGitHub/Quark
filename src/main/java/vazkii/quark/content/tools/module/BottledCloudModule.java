package vazkii.quark.content.tools.module;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.content.tools.block.CloudBlock;
import vazkii.quark.content.tools.client.render.CloudTileEntityRenderer;
import vazkii.quark.content.tools.item.BottledCloudItem;
import vazkii.quark.content.tools.tile.CloudTileEntity;

@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class BottledCloudModule extends QuarkModule {

    public static BlockEntityType<CloudTileEntity> tileEntityType;
    public static Block cloud;
    public static Item bottled_cloud;
    
    @Config 
    public static int cloudLevelBottom = 127;
    
    @Config 
    public static int cloudLevelTop = 132;

	@Override
	public void construct() {
		cloud = new CloudBlock(this);
		bottled_cloud = new BottledCloudItem(this);
		
    	tileEntityType = BlockEntityType.Builder.of(CloudTileEntity::new, cloud).build(null);
		RegistryHelper.register(tileEntityType, "cloud");
	} 
	
	@Override
	public void clientSetup() {
		ClientRegistry.bindTileEntityRenderer(tileEntityType, CloudTileEntityRenderer::new);
	}
	
	@SubscribeEvent
	public void onRightClick(PlayerInteractEvent.RightClickItem event) {
		ItemStack stack = event.getItemStack();
		Player player = event.getPlayer();
		if(stack.getItem() == Items.GLASS_BOTTLE && player.getY() > cloudLevelBottom && player.getY() < cloudLevelTop) {
			stack.shrink(1);
			
			ItemStack returnStack = new ItemStack(bottled_cloud);
			if(!player.addItem(returnStack))
				player.drop(returnStack, false);
			
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}
	
}
