package subtick.mixins.world_tickphases;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.TickProgress;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class BlockEntityWorldMixin extends World implements StructureWorldAccess {

    protected BlockEntityWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Inject(method="tick", at=@At(value="INVOKE",
    target = "Lnet/minecraft/server/world/ServerWorld;tickBlockEntities()V"))
    public void preBlockEntities(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(TickProgress.TILE_ENTITIES, this.getRegistryKey());
    }
}
