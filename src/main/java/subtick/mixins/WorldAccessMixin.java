package subtick.mixins;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.*;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.Highlights;
import subtick.progress.TickProgress;

import static subtick.Dimensions.*;

@Mixin(WorldAccess.class)
public interface WorldAccessMixin extends RegistryWorldView, LunarWorldView {
    @Shadow public abstract @Nullable MinecraftServer getServer();

    private ServerWorld getWorld(){
        if(OVERWORLD ==null){
            initDimensionTypes();
        }

        DimensionType type = getDimension();
        if(type == OVERWORLD){
            return getServer().getWorld(ServerWorld.OVERWORLD);
        }
        if(type == NETHER) {
            return getServer().getWorld(ServerWorld.NETHER);
        }
        if(type == END){
            return getServer().getWorld(ServerWorld.END);
        }
        return null;
    }

    private void initDimensionTypes(){
        OVERWORLD = getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.OVERWORLD_REGISTRY_KEY);
        NETHER = getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.THE_NETHER_REGISTRY_KEY);
        END = getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.THE_END_REGISTRY_KEY);
    }

    @Inject(method = "createAndScheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;I)V", at=@At("HEAD"))
    default void trackBlockTick(BlockPos pos, Block block, int delay, CallbackInfo ci){
        ServerWorld world = getWorld();
        Highlights.newHighlight(pos, TickProgress.TILE_TICKS, world);
    }

    @Inject(method = "createAndScheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;ILnet/minecraft/world/TickPriority;)V", at=@At("HEAD"))
    default void trackBlockTick(BlockPos pos, Block block, int delay, TickPriority priority, CallbackInfo ci){
        ServerWorld world = getWorld();
        Highlights.newHighlight(pos, TickProgress.TILE_TICKS, world);
    }

    @Inject(method = "createAndScheduleFluidTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/Fluid;I)V", at=@At("HEAD"))
    default void trackFluidTick(BlockPos pos, Fluid fluid, int delay, CallbackInfo ci){
        ServerWorld world = getWorld();
        Highlights.newHighlight(pos, TickProgress.LIQUID_TICKS, world);
    }

    @Inject(method = "createAndScheduleFluidTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/Fluid;ILnet/minecraft/world/TickPriority;)V", at=@At("HEAD"))
    default void trackFluidTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority, CallbackInfo ci){
        ServerWorld world = getWorld();
        Highlights.newHighlight(pos, TickProgress.LIQUID_TICKS, world);
    }
}
