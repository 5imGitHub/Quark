package vazkii.quark.content.tools.module;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.content.tools.client.render.entity.PickarangRenderer;
import vazkii.quark.content.tools.entity.Pickarang;
import vazkii.quark.content.tools.item.PickarangItem;

@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class PickarangModule extends QuarkModule {

	public static EntityType<Pickarang> pickarangType;

	@Config(description = "How long it takes before the Pickarang starts returning to the player if it doesn't hit anything.")
	public static int timeout = 20;
	@Config(description = "How long it takes before the Flamarang starts returning to the player if it doesn't hit anything.")
	public static int netheriteTimeout = 20;

	@Config(description = "Pickarang harvest level. 2 is Iron, 3 is Diamond, 4 is Netherite.")
	public static int harvestLevel = 3;
	@Config(description = "Flamarang harvest level. 2 is Iron, 3 is Diamond, 4 is Netherite.")
	public static int netheriteHarvestLevel = 4;

	@Config(description = "Pickarang durability. Set to -1 to have the Pickarang be unbreakable.")
	public static int durability = 800;

	@Config(description = "Flamarang durability. Set to -1 to have the Flamarang be unbreakable.")
	public static int netheriteDurability = 1040;

	@Config(description = "Pickarang max hardness breakable. 22.5 is ender chests, 25.0 is monster boxes, 50 is obsidian. Most things are below 5.")
	public static double maxHardness = 20.0;

	@Config(description = "Flamarang max hardness breakable. 22.5 is ender chests, 25.0 is monster boxes, 50 is obsidian. Most things are below 5.")
	public static double netheriteMaxHardness = 20.0;

	@Config(description = "Set this to true to use the recipe without the Heart of Diamond, even if the Heart of Diamond is enabled.", flag = "pickarang_never_uses_heart")
	public static boolean neverUseHeartOfDiamond = false;

	@Config(description = "Set this to true to disable the short cooldown between throwing Pickarangs.")
	public static boolean noCooldown = false;
	@Config(description = "Set this to true to disable the short cooldown between throwing Flamarangs.")
	public static boolean netheriteNoCooldown = false;

	public static Item pickarang;
	public static Item flamarang;

	private static boolean isEnabled;

	@Override
	public void register() {
		pickarangType = EntityType.Builder.<Pickarang>of(Pickarang::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10) // update interval
				.setCustomClientFactory((spawnEntity, world) -> new Pickarang(pickarangType, world))
				.build("pickarang");
		RegistryHelper.register(pickarangType, "pickarang", Registry.ENTITY_TYPE_REGISTRY);

		pickarang = new PickarangItem("pickarang", this, propertiesFor(durability, false), false);
		flamarang = new PickarangItem("flamerang", this, propertiesFor(netheriteDurability, true), true);
	}

	private static Item.Properties propertiesFor(int durability, boolean netherite) {
		Item.Properties properties = new Item.Properties()
				.stacksTo(1)
				.tab(CreativeModeTab.TAB_TOOLS);

		if (durability > 0)
			properties.durability(durability);

		if(netherite)
			properties.fireResistant();

		return properties;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientSetup() {
		EntityRenderers.register(pickarangType, PickarangRenderer::new);
	}

	@Override
	public void configChanged() {
		// Pass over to a static reference for easier computing the coremod hook
		isEnabled = this.enabled;
	}

	private static final ThreadLocal<Pickarang> ACTIVE_PICKARANG = new ThreadLocal<>();

	public static void setActivePickarang(Pickarang pickarang) {
		ACTIVE_PICKARANG.set(pickarang);
	}

	public static DamageSource createDamageSource(Player player) {
		Pickarang pickarang = ACTIVE_PICKARANG.get();

		if (pickarang == null)
			return null;

		return new IndirectEntityDamageSource("player", pickarang, player).setProjectile();
	}

	public static boolean getIsFireResistant(boolean vanillaVal, Entity entity) {
		if(!isEnabled || vanillaVal)
			return vanillaVal;

		Entity riding = entity.getVehicle();
		if(riding instanceof Pickarang)
			return ((Pickarang) riding).netherite;

		return false;
	}

}
