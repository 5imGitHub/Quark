package vazkii.quark.content.tools.module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.advancement.QuarkAdvancementHandler;
import vazkii.quark.base.handler.advancement.QuarkGenericTrigger;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.content.tools.client.render.entity.PickarangRenderer;
import vazkii.quark.content.tools.config.PickarangType;
import vazkii.quark.content.tools.entity.rang.AbstractPickarang;
import vazkii.quark.content.tools.entity.rang.Echorang;
import vazkii.quark.content.tools.entity.rang.Flamerang;
import vazkii.quark.content.tools.entity.rang.Pickarang;
import vazkii.quark.content.tools.item.PickarangItem;

@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class PickarangModule extends QuarkModule {

	@Config(name = "pickarang")
	public static PickarangType<Pickarang> pickarangType = new PickarangType<>(Items.DIAMOND, Items.DIAMOND_PICKAXE, 20, 3, 800, 20.0, 2, 10);

	@Config(name = "flamerang")
	public static PickarangType<Flamerang> flamerangType = new PickarangType<>(Items.NETHERITE_INGOT, Items.NETHERITE_PICKAXE, 20, 4, 1040, 20.0, 3, 10);

	@Config(name = "echorang")
	public static PickarangType<Echorang> echorangType = new PickarangType<>(Items.ECHO_SHARD, Items.DIAMOND_PICKAXE, 40, 3, 2000, 20.0, 2, 10);

	@Config(flag = "flamerang")
	public static boolean enableFlamerang = true;
	
	@Config(flag = "echorang")
	public static boolean enableEchorang = true;
	
	@Config(description = "Set this to true to use the recipe without the Heart of Diamond, even if the Heart of Diamond is enabled.", flag = "pickarang_never_uses_heart")
	public static boolean neverUseHeartOfDiamond = false;

	public static Item pickarang;
	public static Item flamerang;
	public static Item echorang;

	private static List<PickarangType<?>> knownTypes = new ArrayList<>();
	private static boolean isEnabled;

	public static TagKey<Block> pickarangImmuneTag;
	public static TagKey<GameEvent> echorangCanListenTag;
	
	public static QuarkGenericTrigger throwPickarangTrigger;
	public static QuarkGenericTrigger useFlamerangTrigger;

	@Override
	public void register() {
		pickarang = makePickarang(pickarangType, "pickarang", Pickarang::new, Pickarang::new, () -> true);
		flamerang = makePickarang(flamerangType, "flamerang", Flamerang::new, Flamerang::new, () -> enableFlamerang);
		echorang = makePickarang(echorangType, "echorang", Echorang::new, Echorang::new, () -> enableEchorang);
		
		throwPickarangTrigger = QuarkAdvancementHandler.registerGenericTrigger("throw_pickarang");
		useFlamerangTrigger = QuarkAdvancementHandler.registerGenericTrigger("use_flamerang");
	}

	private <T extends AbstractPickarang<T>> Item makePickarang(PickarangType<T> type, String name, 
			EntityType.EntityFactory<T> entityFactory,
			PickarangType.PickarangConstructor<T> thrownFactory,
			BooleanSupplier condition) {

		EntityType<T> entityType = EntityType.Builder.<T>of(entityFactory, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.setCustomClientFactory((t, l) -> entityFactory.create(type.getEntityType(), l))
				.build(name);
		RegistryHelper.register(entityType, name, Registry.ENTITY_TYPE_REGISTRY);

		knownTypes.add(type);
		type.setEntityType(entityType, thrownFactory);
		return new PickarangItem(name, this, propertiesFor(type.durability, type.isFireResistant()), type).setCondition(condition);
	}

	private Item.Properties propertiesFor(int durability, boolean fireResist) {
		Item.Properties properties = new Item.Properties()
				.stacksTo(1)
				.tab(CreativeModeTab.TAB_TOOLS);

		if (durability > 0)
			properties.durability(durability);

		if(fireResist)
			properties.fireResistant();

		return properties;
	}

	@Override
	public void setup() {
		pickarangImmuneTag = BlockTags.create(new ResourceLocation(Quark.MOD_ID, "pickarang_immune"));
		echorangCanListenTag = TagKey.create(Registry.GAME_EVENT_REGISTRY, new ResourceLocation(Quark.MOD_ID, "echorang_can_listen"));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientSetup() {
		knownTypes.forEach(t -> EntityRenderers.register(t.getEntityType(), PickarangRenderer::new));
	}

	@Override
	public void configChanged() {
		// Pass over to a static reference for easier computing the coremod hook
		isEnabled = this.enabled;
	}

	private static final ThreadLocal<AbstractPickarang<?>> ACTIVE_PICKARANG = new ThreadLocal<>();

	public static void setActivePickarang(AbstractPickarang<?> pickarang) {
		ACTIVE_PICKARANG.set(pickarang);
	}

	public static DamageSource createDamageSource(Player player) {
		AbstractPickarang<?> pickarang = ACTIVE_PICKARANG.get();

		if (pickarang == null)
			return null;

		return new IndirectEntityDamageSource("player", pickarang, player).setProjectile();
	}

	public static boolean getIsFireResistant(boolean vanillaVal, Entity entity) {
		if(!isEnabled || vanillaVal)
			return vanillaVal;

		Entity riding = entity.getVehicle();
		if(riding instanceof AbstractPickarang<?> pick)
			return pick.getPickarangType().isFireResistant();

		return false;
	}

}
