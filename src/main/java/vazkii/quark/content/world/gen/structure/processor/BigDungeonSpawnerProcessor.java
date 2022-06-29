package vazkii.quark.content.world.gen.structure.processor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import vazkii.quark.content.world.module.BigDungeonModule;

import java.util.Optional;
import java.util.Random;

public class BigDungeonSpawnerProcessor extends StructureProcessor {

	public BigDungeonSpawnerProcessor() {
		// NO-OP
	}

	@Override
	public StructureBlockInfo process(LevelReader worldReaderIn, BlockPos pos, BlockPos otherposidk, StructureBlockInfo otherinfoidk, StructureBlockInfo blockInfo, StructurePlaceSettings placementSettingsIn, StructureTemplate template) {
		if(blockInfo.state.getBlock() instanceof SpawnerBlock) {
			RandomSource rand = placementSettingsIn.getRandom(blockInfo.pos);
			BlockEntity tile = BlockEntity.loadStatic(blockInfo.pos, blockInfo.state, blockInfo.nbt);

			if(tile instanceof SpawnerBlockEntity spawner) {
				BaseSpawner logic = spawner.getSpawner();

				EntityType<?> typeToSet = null;
				double val = rand.nextDouble();
				if(val > 0.95)
					typeToSet = EntityType.CREEPER;
				else if(val > 0.5)
					typeToSet = EntityType.SKELETON;
				else typeToSet = EntityType.ZOMBIE;

				CompoundTag tag = new CompoundTag();
				tag.putString("id", typeToSet.getRegistryName().toString());
				logic.spawnPotentials = SimpleWeightedRandomList.single(new SpawnData(tag, Optional.empty()));

				CompoundTag nbt = spawner.saveWithFullMetadata();
				return new StructureBlockInfo(blockInfo.pos, blockInfo.state, nbt);
			}
		}

		return blockInfo;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return BigDungeonModule.SPAWN_PROCESSOR_TYPE;
	}

}
