package vazkii.quark.content.tools.module;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.saveddata.maps.MapDecoration.Type;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.arl.util.ItemNBTHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.advancement.QuarkAdvancementHandler;
import vazkii.quark.base.handler.advancement.QuarkGenericTrigger;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.module.config.type.AbstractConfigType;
import vazkii.quark.content.tools.loot.InBiomeCondition;
import vazkii.quark.content.tools.loot.PathfinderMapFunction;

@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class PathfinderMapsModule extends QuarkModule {
	
	private static final String TAG_IS_PATHFINDER = "quark:is_pathfinder";

	private static final Object mutex = new Object();

	public static List<TradeInfo> builtinTrades = new LinkedList<>();
	public static List<TradeInfo> customTrades = new LinkedList<>();
	public static List<TradeInfo> tradeList = new LinkedList<>();

	@Config(description = """
			In this section you can add custom Pathfinder Maps. This works for both vanilla and modded biomes.
			Each custom map must be on its own line.
			The format for a custom map is as follows:
			<id>,<level>,<min_price>,<max_price>,<color>,<name>

			With the following descriptions:
			 - <id> being the biome's ID NAME. You can find vanilla names here - https://minecraft.gamepedia.com/Biome#Biome_IDs
			 - <level> being the Cartographer villager level required for the map to be unlockable
			 - <min_price> being the cheapest (in Emeralds) the map can be
			 - <max_price> being the most expensive (in Emeralds) the map can be
			 - <color> being a hex color (without the #) for the map to display. You can generate one here - https://htmlcolorcodes.com/

			Here's an example of a map to locate Ice Mountains:
			minecraft:ice_mountains,2,8,14,7FE4FF""")
	private List<String> customs = new LinkedList<>();

	public static LootItemFunctionType pathfinderMapType;
	public static LootItemConditionType inBiomeConditionType;

	public static QuarkGenericTrigger pathfinderMapTrigger;
	
	@Config public static int searchRadius = 6400;
	@Config public static int xpFromTrade = 5;

	@Override
	public void register() {
		loadTradeInfo(Biomes.SNOWY_PLAINS, true, 4, 8, 14, 0x7FE4FF);
		loadTradeInfo(Biomes.WINDSWEPT_HILLS, true, 4, 8, 14, 0x8A8A8A);
		loadTradeInfo(Biomes.DARK_FOREST, true, 4, 8, 14, 0x00590A);
		loadTradeInfo(Biomes.DESERT, true, 4, 8, 14, 0xCCB94E);
		loadTradeInfo(Biomes.SAVANNA, true, 4, 8, 14, 0x9BA562);
		loadTradeInfo(Biomes.SWAMP, true, 4, 12, 18, 0x22370F);
		loadTradeInfo(Biomes.OLD_GROWTH_PINE_TAIGA, true, 4, 12, 18, 0x5B421F);

		loadTradeInfo(Biomes.FLOWER_FOREST, true, 5, 12, 18, 0xCE46E2);
		loadTradeInfo(Biomes.JUNGLE, true, 5, 16, 22, 0x22B600);
		loadTradeInfo(Biomes.BAMBOO_JUNGLE, true, 5, 16, 22, 0x3DE217);
		loadTradeInfo(Biomes.BADLANDS, true, 5, 16, 22, 0xC67F22);
		loadTradeInfo(Biomes.MUSHROOM_FIELDS, true, 5, 20, 26, 0x4D4273);
		loadTradeInfo(Biomes.ICE_SPIKES, true, 5, 20, 26, 0x1EC0C9);

		pathfinderMapType = new LootItemFunctionType(new PathfinderMapFunction.Serializer());
		Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(Quark.MOD_ID, "pathfinder_map"), pathfinderMapType);
		inBiomeConditionType = new LootItemConditionType(new InBiomeCondition.InBiomeSerializer());
		Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(Quark.MOD_ID, "in_biome"), inBiomeConditionType);
		
		pathfinderMapTrigger = QuarkAdvancementHandler.registerGenericTrigger("pathfinder_map_center");
	}

	@SubscribeEvent
	public void onTradesLoaded(VillagerTradesEvent event) {
		if(event.getType() == VillagerProfession.CARTOGRAPHER)
			synchronized (mutex) {
				Int2ObjectMap<List<ItemListing>> trades = event.getTrades();
				for(TradeInfo info : tradeList)
					if(info != null)
						trades.get(info.level).add(new PathfinderMapTrade(info));
			}
	}
	
	@SubscribeEvent
	public void playerTick(PlayerTickEvent event) {
		Player player = event.player;
		if(!(player instanceof ServerPlayer))
			return;

		if(!tryCheckCenter(player, InteractionHand.MAIN_HAND))
			tryCheckCenter(player, InteractionHand.OFF_HAND);
	}
	
	private boolean tryCheckCenter(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		
		if(stack.getItem() == Items.FILLED_MAP && stack.hasTag() && ItemNBTHelper.getBoolean(stack, TAG_IS_PATHFINDER, false)) {
			 ListTag decorations = stack.getTag().getList("Decorations", stack.getTag().getId());
			 
			 for(int i = 0; i < decorations.size(); i++) {
				 Tag tag = decorations.get(i);
				 if(tag instanceof CompoundTag cmp) {
					 String id = cmp.getString("id");
					 
					 if(id.equals("+")) {
						 double x = cmp.getDouble("x");
						 double z = cmp.getDouble("z");
						 
						 Vec3 pp = player.position();
						 double px = pp.x;
						 double pz = pp.z;
						 
						 double distSq = (px - x) * (px - x) + (pz - z) * (pz - z); 
						 if(distSq < 200) {
							 pathfinderMapTrigger.trigger((ServerPlayer) player);
							 return true;
						 }
					 }
				 }
			 }
		}
		
		return false;
	}

	@Override
	public void configChanged() {
		synchronized (mutex) {
			tradeList.clear();
			customTrades.clear();

			loadCustomMaps(customs);

			tradeList.addAll(builtinTrades);
			tradeList.addAll(customTrades);
		}
	}

	private void loadTradeInfo(ResourceKey<Biome> biome, boolean enabled, int level, int minPrice, int maxPrice, int color) {
		builtinTrades.add(new TradeInfo(biome.location(), enabled, level, minPrice, maxPrice, color));
	}

	private void loadCustomTradeInfo(ResourceLocation biome, boolean enabled, int level, int minPrice, int maxPrice, int color) {
		customTrades.add(new TradeInfo(biome, enabled, level, minPrice, maxPrice, color));
	}

	private void loadCustomTradeInfo(String line) throws IllegalArgumentException {
		String[] tokens = line.split(",");
		if(tokens.length != 5 && tokens.length != 6) // Silently ignore old name format
			throw new IllegalArgumentException("Wrong number of parameters " + tokens.length + " (expected 5)");

		ResourceLocation biomeName = new ResourceLocation(tokens[0]);
		int level = Integer.parseInt(tokens[1]);
		int minPrice = Integer.parseInt(tokens[2]);
		int maxPrice = Integer.parseInt(tokens[3]);
		int color = Integer.parseInt(tokens[4], 16);

		loadCustomTradeInfo(biomeName, true, level, minPrice, maxPrice, color);
	}

	private void loadCustomMaps(Iterable<String> lines) {
		for(String s : lines)
			try {
				loadCustomTradeInfo(s);
			} catch(IllegalArgumentException e) {
				Quark.LOG.warn("[Custom Pathfinder Maps] Error while reading custom map string \"{}\"", s);
				Quark.LOG.warn("[Custom Pathfinder Maps] - {}", e.getMessage());
			}
	}

	public static ItemStack createMap(Level world, BlockPos pos, Predicate<Holder<Biome>> predicate, int color) {
		if(!(world instanceof ServerLevel serverLevel))
			return ItemStack.EMPTY;

		// from LocateCommand
		Pair<BlockPos, Holder<Biome>> biomeInfo = serverLevel.findClosestBiome3d(predicate, pos, searchRadius, 32, 64);

		if(biomeInfo == null)
			return ItemStack.EMPTY;

		BlockPos biomePos = biomeInfo.getFirst();
		Either<ResourceKey<Biome>, Biome> biome = biomeInfo.getSecond().unwrap();
		Optional<ResourceKey<Biome>> key = biome.map(Optional::of, ForgeRegistries.BIOMES::getResourceKey);

		Component biomeComponent = key
				.map(ResourceKey::location)
				.<MutableComponent>map((it) -> Component.translatable("biome." + it.getNamespace() + "." + it.getPath()))
				.orElse(Component.translatable("item.quark.biome_map.unknown").withStyle(ChatFormatting.RED));

		ItemStack stack = MapItem.create(world, biomePos.getX(), biomePos.getZ(), (byte) 2, true, true);
		// fillExplorationMap
		MapItem.renderBiomePreviewMap(serverLevel, stack);
		MapItemSavedData.addTargetDecoration(stack, biomePos, "+", Type.RED_X);
		stack.setHoverName(Component.translatable("item.quark.biome_map", biomeComponent));

		stack.getOrCreateTagElement("display").putInt("MapColor", color);
		ItemNBTHelper.setBoolean(stack, TAG_IS_PATHFINDER, true);

		return stack;
	}

	private record PathfinderMapTrade(TradeInfo info) implements ItemListing {

		@Override
		public MerchantOffer getOffer(@Nonnull Entity entity, @Nonnull RandomSource random) {
			if (!info.enabled)
				return null;

			int i = random.nextInt(info.maxPrice - info.minPrice + 1) + info.minPrice;

			ItemStack itemstack = createMap(entity.level, entity.blockPosition(), info, info.color);
			if (itemstack.isEmpty())
				return null;

			return new MerchantOffer(new ItemStack(Items.EMERALD, i), new ItemStack(Items.COMPASS), itemstack, 12, xpFromTrade * Math.max(1, (info.level - 1)), 0.2F);
		}
	}

	public static class TradeInfo extends AbstractConfigType implements Predicate<Holder<Biome>> {

		public final ResourceLocation biome;
		public final int color;

		@Config public boolean enabled;
		@Config public final int level;
		@Config public final int minPrice;
		@Config public final int maxPrice;

		TradeInfo(ResourceLocation biome, boolean enabled, int level, int minPrice, int maxPrice, int color) {
			this.biome = biome;

			this.enabled = enabled;
			this.level = level;
			this.minPrice = minPrice;
			this.maxPrice = maxPrice;
			this.color = color;
		}

		@Override
		public boolean test(Holder<Biome> biomeHolder) {
			return biomeHolder.is(biome);
		}
	}

}
