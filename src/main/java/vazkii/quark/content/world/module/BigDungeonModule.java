package vazkii.quark.content.world.module;

import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;

// Mostly all taken from https://github.com/TelepathicGrunt/StructureTutorialMod/blob/1.18.2-Forge-Jigsaw/
// thank u <3

@LoadModule(category = ModuleCategory.WORLD, hasSubscriptions = true)
public class BigDungeonModule extends QuarkModule {
//
//	@Config(description = "The chance that a big dungeon spawn candidate will be allowed to spawn. 0.2 is 20%, which is the same as the Pillager Outpost.")
//	public static double spawnChance = 0.1;
//
//	@Config
//	public static String lootTable = "minecraft:chests/simple_dungeon";
//
//	@Config public static int maxRooms = 10;
//	@Config public static int minStartY = -40;
//	@Config public static int maxStartY = -20;
//	@Config public static double chestChance = 0.5;
//
//	@Config
//	public static CompoundBiomeConfig biomeConfig = CompoundBiomeConfig.fromBiomeTypes(true, BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.BEACH, BiomeDictionary.Type.NETHER, BiomeDictionary.Type.END);
//
//	private static BigDungeonChestProcessor CHEST_PROCESSOR = new BigDungeonChestProcessor();
//	private static BigDungeonSpawnerProcessor SPAWN_PROCESSOR = new BigDungeonSpawnerProcessor();
//	private static BigDungeonWaterProcessor WATER_PROCESSOR = new BigDungeonWaterProcessor();
//
//	public static StructureProcessorType<BigDungeonChestProcessor> CHEST_PROCESSOR_TYPE = () -> Codec.unit(CHEST_PROCESSOR);
//	public static StructureProcessorType<BigDungeonSpawnerProcessor> SPAWN_PROCESSOR_TYPE = () -> Codec.unit(SPAWN_PROCESSOR);
//	public static StructureProcessorType<BigDungeonWaterProcessor> WATER_PROCESSOR_TYPE = () -> Codec.unit(WATER_PROCESSOR);
//
//	public static final DeferredRegister<StructureFeature<?>> DEFERRED_REGISTRY_STRUCTURE = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, Quark.MOD_ID);
//	public static final RegistryObject<StructureFeature<JigsawConfiguration>> STRUCTURE = DEFERRED_REGISTRY_STRUCTURE.register("mega_dungeon", () -> (new BigDungeonStructure(JigsawConfiguration.CODEC)));
//
//	@Override
//	public void construct() {
//		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
//		DEFERRED_REGISTRY_STRUCTURE.register(modEventBus);
//	}
//
//	@Override
//	public void setup() {
//		enqueue(this::setupStructures);
//	}
//
//	private void setupStructures() {
//		registerProcessor("big_dungeon_chest", CHEST_PROCESSOR, CHEST_PROCESSOR_TYPE);
//		registerProcessor("big_dungeon_spawner", SPAWN_PROCESSOR, SPAWN_PROCESSOR_TYPE);
//		registerProcessor("big_dungeon_water", WATER_PROCESSOR, WATER_PROCESSOR_TYPE);
//	}
//
//	private static <T extends StructureProcessor> void registerProcessor(String name, T processor, StructureProcessorType<T> type) {
//		ResourceLocation res = new ResourceLocation(Quark.MOD_ID, name);
//		Registry.register(Registry.STRUCTURE_PROCESSOR, res, type);
//		Registry.register(BuiltinRegistries.PROCESSOR_LIST, res, new StructureProcessorList(Lists.newArrayList(processor)));
//	}

}
