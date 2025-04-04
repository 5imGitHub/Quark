package vazkii.quark.base.handler.advancement.mod;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import vazkii.quark.base.handler.advancement.AdvancementModifier;
import vazkii.quark.base.handler.advancement.MutableAdvancement;
import vazkii.quark.base.module.QuarkModule;

public class AdventuringTimeModifier extends AdvancementModifier {

	private static final ResourceLocation TARGET = new ResourceLocation("adventure/adventuring_time");
	
	private final Set<ResourceKey<Biome>> locations;
	
	public AdventuringTimeModifier(QuarkModule module, Set<ResourceKey<Biome>> locations) {
		super(module);
		this.locations = locations;
	}

	@Override
	public Set<ResourceLocation> getTargets() {
		return ImmutableSet.of(TARGET);
	}

	@Override
	public boolean apply(ResourceLocation res, MutableAdvancement adv) {
		for(ResourceKey<Biome> key : locations) {
			String name = key.location().toString();
			
			Criterion criterion = new Criterion(PlayerTrigger.TriggerInstance.located(LocationPredicate.inBiome(key)));
			adv.addRequiredCriterion(name, criterion);
		}
		
		return true;
	}

}
