package vazkii.quark.content.building.module;

import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.content.building.block.BambooMatBlock;

@LoadModule(category = ModuleCategory.BUILDING)
public class BambooMatModule extends QuarkModule {

	@Override
	public void construct() {
		new BambooMatBlock(this);
	}
	
}
