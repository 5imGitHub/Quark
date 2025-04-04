package vazkii.quark.integration.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.NonNullList;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import vazkii.arl.util.ItemNBTHelper;
import vazkii.quark.addons.oddities.block.be.MatrixEnchantingTableBlockEntity;
import vazkii.quark.addons.oddities.client.screen.BackpackInventoryScreen;
import vazkii.quark.addons.oddities.client.screen.CrateScreen;
import vazkii.quark.addons.oddities.module.MatrixEnchantingModule;
import vazkii.quark.addons.oddities.util.Influence;
import vazkii.quark.base.Quark;
import vazkii.quark.base.block.IQuarkBlock;
import vazkii.quark.base.client.handler.RequiredModTooltipHandler;
import vazkii.quark.base.handler.MiscUtil;
import vazkii.quark.base.item.IQuarkItem;
import vazkii.quark.base.item.QuarkItem;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.content.building.module.VariantFurnacesModule;
import vazkii.quark.content.client.module.ImprovedTooltipsModule;
import vazkii.quark.content.client.tooltip.EnchantedBookTooltips;
import vazkii.quark.content.tools.item.AncientTomeItem;
import vazkii.quark.content.tools.module.AncientTomesModule;
import vazkii.quark.content.tools.module.ColorRunesModule;
import vazkii.quark.content.tools.module.PickarangModule;
import vazkii.quark.content.tweaks.recipe.ElytraDuplicationRecipe;
import vazkii.quark.content.tweaks.recipe.SlabToBlockRecipe;

@JeiPlugin
public class QuarkJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID = new ResourceLocation(Quark.MOD_ID, Quark.MOD_ID);

	public static final RecipeType<InfluenceEntry> INFLUENCING =
		 RecipeType.create(Quark.MOD_ID, "influence", InfluenceEntry.class);

	@Nonnull
	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerItemSubtypes(@Nonnull ISubtypeRegistration registration) {
		registration.useNbtForSubtypes(AncientTomesModule.ancient_tome);
	}

	@Override
	public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {
		List<ItemStack> disabledItems = RequiredModTooltipHandler.disabledItems();
		if (!disabledItems.isEmpty())
			jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, disabledItems);

		ModuleLoader.INSTANCE.initJEICompat(() -> {
			NonNullList<ItemStack> stacks = NonNullList.create();
			for (Item item : ForgeRegistries.ITEMS.getValues()) {
				ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
				if (loc != null && loc.getNamespace().equals("quark")) {
					if ((item instanceof IQuarkItem quarkItem && !quarkItem.isEnabled()) ||
							(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof IQuarkBlock quarkBlock && !quarkBlock.isEnabled())) {
						item.fillItemCategory(CreativeModeTab.TAB_SEARCH, stacks);
					}
				}
			}

			if (!stacks.isEmpty())
				Minecraft.getInstance().submitAsync(() -> jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks));
		});
	}

	@Override
	public void registerVanillaCategoryExtensions(@Nonnull IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addCategoryExtension(ElytraDuplicationRecipe.class, ElytraDuplicationExtension::new);
		registration.getCraftingCategory().addCategoryExtension(SlabToBlockRecipe.class, SlabToBlockExtension::new);
	}

	private boolean matrix() {
		return ModuleLoader.INSTANCE.isModuleEnabled(MatrixEnchantingModule.class) && MatrixEnchantingModule.allowInfluencing && !MatrixEnchantingModule.candleInfluencingFailed;
	}

	@Override
	public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
		if (matrix())
			registration.addRecipeCategories(new InfluenceCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(@Nonnull IRecipeRegistration registration) {
		IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();

		if (ModuleLoader.INSTANCE.isModuleEnabled(AncientTomesModule.class))
			registerAncientTomeAnvilRecipes(registration, factory);

		if (ModuleLoader.INSTANCE.isModuleEnabled(PickarangModule.class)) {
			registerPickarangAnvilRepairs(PickarangModule.pickarang, Items.DIAMOND, registration, factory);
			registerPickarangAnvilRepairs(PickarangModule.flamerang, Items.NETHERITE_INGOT, registration, factory);
		}

		if (ModuleLoader.INSTANCE.isModuleEnabled(ColorRunesModule.class))
			registerRuneAnvilRecipes(registration, factory);

		if (matrix())
			registerInfluenceRecipes(registration);
	}

	@Override
	public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
		if(ModuleLoader.INSTANCE.isModuleEnabled(VariantFurnacesModule.class)) {
			registration.addRecipeCatalyst(new ItemStack(VariantFurnacesModule.deepslateFurnace), RecipeTypes.FUELING, RecipeTypes.SMELTING);
			registration.addRecipeCatalyst(new ItemStack(VariantFurnacesModule.blackstoneFurnace), RecipeTypes.FUELING, RecipeTypes.SMELTING);
		}

		if (matrix()) {
			if (MatrixEnchantingModule.automaticallyConvert)
				registration.addRecipeCatalyst(new ItemStack(Blocks.ENCHANTING_TABLE), INFLUENCING);
			else
				registration.addRecipeCatalyst(new ItemStack(MatrixEnchantingModule.matrixEnchanter), INFLUENCING);
		}
	}

	@Override
	public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(CrateScreen.class, new CrateGuiHandler());
		registration.addRecipeClickArea(BackpackInventoryScreen.class, 137, 29, 10, 13, RecipeTypes.CRAFTING);
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new BackpackRecipeTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
	}

	private void registerAncientTomeAnvilRecipes(@Nonnull IRecipeRegistration registration, @Nonnull IVanillaRecipeFactory factory) {
		List<IJeiAnvilRecipe> recipes = new ArrayList<>();
		for (Enchantment enchant : AncientTomesModule.validEnchants) {
			EnchantmentInstance data = new EnchantmentInstance(enchant, enchant.getMaxLevel());
			recipes.add(factory.createAnvilRecipe(EnchantedBookItem.createForEnchantment(data),
					Collections.singletonList(AncientTomeItem.getEnchantedItemStack(enchant)),
					Collections.singletonList(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(data.enchantment, data.level + 1)))));
		}
		registration.addRecipes(RecipeTypes.ANVIL, recipes);
	}

	private void registerRuneAnvilRecipes(@Nonnull IRecipeRegistration registration, @Nonnull IVanillaRecipeFactory factory) {
		RandomSource random = RandomSource.create();
		Stream<ItemStack> displayItems;
		if (ModuleLoader.INSTANCE.isModuleEnabled(ImprovedTooltipsModule.class) && ImprovedTooltipsModule.enchantingTooltips) {
			displayItems = EnchantedBookTooltips.getTestItems().stream();
		} else {
			displayItems = Stream.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
					Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
					Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.ELYTRA, Items.SHIELD, Items.BOW, Items.CROSSBOW,
					Items.TRIDENT, Items.FISHING_ROD, Items.SHEARS, PickarangModule.pickarang).map(ItemStack::new);
		}

		List<ItemStack> used = displayItems
				.filter(it -> !(it.getItem() instanceof QuarkItem qItem) || qItem.isEnabled())
				.map(item -> makeEnchantedDisplayItem(item, random))
				.collect(Collectors.toList());

		List<IJeiAnvilRecipe> recipes = new ArrayList<>();
		for (Item rune : MiscUtil.getTagValues(BuiltinRegistries.ACCESS, ColorRunesModule.runesTag)) {
			ItemStack runeStack = new ItemStack(rune);
			recipes.add(factory.createAnvilRecipe(used, Collections.singletonList(runeStack),
					used.stream().map(stack -> {
						ItemStack output = stack.copy();
						ItemNBTHelper.setBoolean(output, ColorRunesModule.TAG_RUNE_ATTACHED, true);
						ItemNBTHelper.setCompound(output, ColorRunesModule.TAG_RUNE_COLOR, runeStack.serializeNBT());
						return output;
					}).collect(Collectors.toList())));
		}
		registration.addRecipes(RecipeTypes.ANVIL, recipes);
	}

	// Runes only show up and can be only anvilled on enchanted items, so make some random enchanted items
	@Nonnull
	private static ItemStack makeEnchantedDisplayItem(ItemStack input, RandomSource random) {
		ItemStack stack = input.copy();
		stack.setHoverName(Component.translatable("quark.jei.any_enchanted"));
		if (stack.getEnchantmentValue() <= 0) { // If it can't take anything in ench. tables...
			stack.enchant(Enchantments.UNBREAKING, 3); // it probably accepts unbreaking anyways
			return stack;
		}
		return EnchantmentHelper.enchantItem(random, stack, 25, false);
	}

	private void registerPickarangAnvilRepairs(Item pickarang, Item repairMaterial, @Nonnull IRecipeRegistration registration, @Nonnull IVanillaRecipeFactory factory) {
		//Repair ratios taken from JEI anvil maker
		ItemStack nearlyBroken = new ItemStack(pickarang);
		nearlyBroken.setDamageValue(nearlyBroken.getMaxDamage());
		ItemStack veryDamaged = nearlyBroken.copy();
		veryDamaged.setDamageValue(veryDamaged.getMaxDamage() * 3 / 4);
		ItemStack damaged = nearlyBroken.copy();
		damaged.setDamageValue(damaged.getMaxDamage() * 2 / 4);

		IJeiAnvilRecipe materialRepair = factory.createAnvilRecipe(nearlyBroken,
				Collections.singletonList(new ItemStack(repairMaterial)), Collections.singletonList(veryDamaged));
		IJeiAnvilRecipe toolRepair = factory.createAnvilRecipe(veryDamaged,
				Collections.singletonList(veryDamaged), Collections.singletonList(damaged));

		registration.addRecipes(RecipeTypes.ANVIL, Arrays.asList(materialRepair, toolRepair));
	}

	private void registerInfluenceRecipes(@Nonnull IRecipeRegistration registration) {
		registration.addRecipes(INFLUENCING,
			 Arrays.stream(DyeColor.values()).map(color -> {
				 Block candle = MatrixEnchantingTableBlockEntity.CANDLES.get(color.getId());
				 Influence influence = MatrixEnchantingModule.candleInfluences.get(color);

				 return new InfluenceEntry(candle, influence);
			 }).filter(InfluenceEntry::hasAny).collect(Collectors.toList()));

		registration.addRecipes(INFLUENCING,
			 MatrixEnchantingModule.customInfluences.entrySet().stream().map(entry -> {
				 Block block = entry.getKey().getBlock();
				 Influence influence = entry.getValue().influence();

				 return new InfluenceEntry(block, influence);
			 }).filter(InfluenceEntry::hasAny).collect(Collectors.toList()));
	}

	private static class CrateGuiHandler implements IGuiContainerHandler<CrateScreen> {

		@Nonnull
		@Override
		public List<Rect2i> getGuiExtraAreas(@Nonnull CrateScreen containerScreen) {
			return containerScreen.getExtraAreas();
		}

	}
}

