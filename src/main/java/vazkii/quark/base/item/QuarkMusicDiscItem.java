package vazkii.quark.base.item;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.module.QuarkModule;

public class QuarkMusicDiscItem extends RecordItem implements IQuarkItem {

	private final QuarkModule module;
	private final boolean isAmbient;
	private final Supplier<SoundEvent> soundSupplier;

	private BooleanSupplier enabledSupplier = () -> true;

	public QuarkMusicDiscItem(int comparatorValue, Supplier<SoundEvent> sound, String name, QuarkModule module, boolean isAmbient) {
		super(comparatorValue, sound, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_MISC).rarity(Rarity.RARE));

		RegistryHelper.registerItem(this, "music_disc_" + name);
		this.module = module;
		this.isAmbient = isAmbient;
		this.soundSupplier = sound;
	}

	@Override
	public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
		if(isEnabled() || group == CreativeModeTab.TAB_SEARCH)
			super.fillItemCategory(group, items);
	}

	@Override
	public QuarkMusicDiscItem setCondition(BooleanSupplier enabledSupplier) {
		this.enabledSupplier = enabledSupplier;
		return this;
	}

	@Override
	public QuarkModule getModule() {
		return module;
	}

	@Override
	public boolean doesConditionApply() {
		return enabledSupplier.getAsBoolean();
	}

	@OnlyIn(Dist.CLIENT)
	public boolean playAmbientSound(BlockPos pos) {
		if(isAmbient) {
			Minecraft mc = Minecraft.getInstance();
			SoundManager soundEngine = mc.getSoundManager();
			LevelRenderer render = mc.levelRenderer;

			SimpleSoundInstance simplesound = new SimpleSoundInstance(soundSupplier.get().getLocation(), SoundSource.RECORDS, 4.0F, 1.0F, SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.LINEAR, pos.getX(), pos.getY(), pos.getZ(), false);

			render.playingRecords.put(pos, simplesound);
			soundEngine.play(simplesound);

			if(mc.level != null)
				mc.level.addParticle(ParticleTypes.NOTE,pos.getX() + Math.random(), pos.getY() + 1.1, pos.getZ() + Math.random(), Math.random(), 0, 0);

			return true;
		}

		return false;
	}

}
