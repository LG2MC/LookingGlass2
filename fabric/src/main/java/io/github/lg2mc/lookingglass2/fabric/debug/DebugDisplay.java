package io.github.lg2mc.lookingglass2.fabric.debug;

import io.github.lg2mc.lookingglass2.LookingGlass2;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.Direction;

/** Temporary Fabric-only test content. Not part of the public API. */
public final class DebugDisplay {
    public static final ResourceLocation ID = new ResourceLocation(LookingGlass2.MOD_ID, "debug_display");
    public static final DisplayBlock BLOCK = new DisplayBlock();
    public static final BlockEntityType<DisplayEntity> ENTITY_TYPE =
            FabricBlockEntityTypeBuilder.create(DisplayEntity::new, BLOCK).build();

    private DebugDisplay() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, ID, BLOCK);
        Registry.register(BuiltInRegistries.ITEM, ID, new BlockItem(BLOCK, new Item.Properties()));
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ID, ENTITY_TYPE);
    }

    public static final class DisplayBlock extends BaseEntityBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

        private DisplayBlock() {
            super(BlockBehaviour.Properties.of().strength(1.0F));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new DisplayEntity(pos, state);
        }
    }

    public static final class DisplayEntity extends BlockEntity {
        public DisplayEntity(BlockPos pos, BlockState state) {
            super(ENTITY_TYPE, pos, state);
        }
    }
}
