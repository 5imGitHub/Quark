package vazkii.quark.content.mobs.module;

import com.google.common.collect.ImmutableSet;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.handler.EntityAttributeHandler;
import vazkii.quark.base.handler.advancement.QuarkAdvancementHandler;
import vazkii.quark.base.handler.advancement.mod.MonsterHunterModifier;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.world.EntitySpawnHandler;
import vazkii.quark.content.mobs.client.render.entity.ForgottenRenderer;
import vazkii.quark.content.mobs.entity.Forgotten;
import vazkii.quark.content.mobs.item.ForgottenHatItem;

@LoadModule(category = ModuleCategory.MOBS, hasSubscriptions = true)
public class ForgottenModule extends QuarkModule {

	public static EntityType<Forgotten> forgottenType;

	public static Item forgotten_hat;

	@Config(description = "1 in this many Skeletons that spawn under the threshold are replaced with Forgotten.")
	public double forgottenSpawnRate = 0.05;

	@Config public int maxHeightForSpawn = 0;

	@Override
	public void register() {
		forgotten_hat = new ForgottenHatItem(this);

		forgottenType = EntityType.Builder.of(Forgotten::new, MobCategory.MONSTER)
				.sized(0.7F, 2.4F)
				.clientTrackingRange(8)
				.setCustomClientFactory((spawnEntity, world) -> new Forgotten(forgottenType, world))
				.build("forgotten");

		RegistryHelper.register(forgottenType, "forgotten", Registry.ENTITY_TYPE_REGISTRY);
		EntitySpawnHandler.addEgg(forgottenType, 0x969487, 0x3a3330, this, () -> true);

		EntityAttributeHandler.put(forgottenType, Forgotten::registerAttributes);
		
		QuarkAdvancementHandler.addModifier(new MonsterHunterModifier(this, ImmutableSet.of(forgottenType)));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientSetup() {
		EntityRenderers.register(forgottenType, ForgottenRenderer::new);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onSkeletonSpawn(LivingSpawnEvent.CheckSpawn event) {
		if (event.getSpawnReason() == MobSpawnType.SPAWNER)
			return;

		LivingEntity entity = event.getEntity();
		Result result = event.getResult();
		LevelAccessor world = event.getLevel();

		if(entity.getType() == EntityType.SKELETON && entity instanceof Mob mob && result != Result.DENY && entity.getY() < maxHeightForSpawn && world.getRandom().nextDouble() < forgottenSpawnRate) {
			if(result == Result.ALLOW || (mob.checkSpawnRules(world, event.getSpawnReason()) && mob.checkSpawnObstruction(world))) {
				Forgotten forgotten = new Forgotten(forgottenType, entity.level);
				Vec3 epos = entity.position();
				
				forgotten.absMoveTo(epos.x, epos.y, epos.z, entity.getYRot(), entity.getXRot());
				forgotten.prepareEquipment();

				LivingSpawnEvent.CheckSpawn newEvent = new CheckSpawn(forgotten, world, event.getX(), event.getY(), event.getZ(), event.getSpawner(), event.getSpawnReason());
				MinecraftForge.EVENT_BUS.post(newEvent);
				
				if(newEvent.getResult() != Result.DENY) {
					world.addFreshEntity(forgotten);
					event.setResult(Result.DENY);
				}
			}
		}
	}

}
