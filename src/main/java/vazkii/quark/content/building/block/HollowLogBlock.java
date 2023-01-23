package vazkii.quark.content.building.block;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vazkii.quark.base.block.IQuarkBlock;
import vazkii.quark.base.block.QuarkPillarBlock;
import vazkii.quark.base.handler.RenderLayerHandler;
import vazkii.quark.base.handler.RenderLayerHandler.RenderTypeSkeleton;
import vazkii.quark.base.module.QuarkModule;

public class HollowLogBlock extends QuarkPillarBlock implements SimpleWaterloggedBlock {

	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	private static final VoxelShape SHAPE_BOTTOM = Block.box(0F, 0F, 0F, 16F, 2F, 16F);
	private static final VoxelShape SHAPE_TOP = Block.box(0F, 14F, 0F, 16F, 16F, 16F);
	private static final VoxelShape SHAPE_NORTH = Block.box(0F, 0F, 0F, 2F, 16F, 16F);
	private static final VoxelShape SHAPE_SOUTH = Block.box(14F, 0F, 0F, 16F, 16F, 16F);
	private static final VoxelShape SHAPE_EAST = Block.box(0F, 0F, 0F, 16F, 16F, 2F);
	private static final VoxelShape SHAPE_WEST = Block.box(0F, 0F, 14F, 16F, 16F, 16F);

	private static final VoxelShape SHAPE_X = Shapes.or(SHAPE_BOTTOM, SHAPE_TOP, SHAPE_EAST, SHAPE_WEST);
	private static final VoxelShape SHAPE_Y = Shapes.or(SHAPE_NORTH, SHAPE_SOUTH, SHAPE_EAST, SHAPE_WEST);
	private static final VoxelShape SHAPE_Z = Shapes.or(SHAPE_BOTTOM, SHAPE_TOP, SHAPE_NORTH, SHAPE_SOUTH);

	private final boolean flammable;
	
	public HollowLogBlock(Block sourceLog, QuarkModule module, boolean flammable) {
		super(IQuarkBlock.inherit(sourceLog, "hollow_%s"), module, CreativeModeTab.TAB_DECORATIONS, 
				Properties.copy(sourceLog)
				.noOcclusion()
				.isSuffocating((s,g,p) -> false));

		this.flammable = flammable;
		RenderLayerHandler.setRenderType(this, RenderTypeSkeleton.CUTOUT_MIPPED);
		
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
	}
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
		if(ctx instanceof EntityCollisionContext ectx && ectx.getEntity() instanceof Player player) {
			BlockPos bpos = player.blockPosition();
			if(player.getEyeHeight() > 1F)
				bpos = bpos.above();
			
			BlockState bstate = world.getBlockState(bpos);
			if(bstate.getBlock() instanceof HollowLogBlock)
				return getCollisionShape(state, world, bpos, ctx);
		}
		
		return super.getShape(state, world, pos, ctx);
	}
	
	@Override
	public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
		if(state.getValue(AXIS) != Axis.Y)
			return false;
		
		Vec3 eyePos = entity.getEyePosition();
		double pad = 2.0 / 16.0;
		if(eyePos.x > (pos.getX() + pad) && eyePos.z > (pos.getZ() + pad) && eyePos.x < (pos.getX() + 1 - pad) && eyePos.z < (pos.getZ() + 1 - pad))
			return true;
		
		return super.isLadder(state, level, pos, entity);
	}
	
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return super.getStateForPlacement(ctx).setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
	}
	
	@Override
	public boolean propagatesSkylightDown(BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
		return !state.getValue(WATERLOGGED);
	}

	@Nonnull
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	
	@Nonnull
	@Override
	public BlockState updateShape(BlockState state, @Nonnull Direction facing, @Nonnull BlockState facingState, @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(state, facing, facingState, level, pos, facingPos);
	}
	
	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return flammable;
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState p_56967_) {
		return false;
	}
	
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> def) {
		super.createBlockStateDefinition(def);
		
		def.add(WATERLOGGED);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
		return switch(state.getValue(AXIS)) {
		case X -> SHAPE_X;
		case Y -> SHAPE_Y;
		case Z -> SHAPE_Z;
		};
	}
	
}

