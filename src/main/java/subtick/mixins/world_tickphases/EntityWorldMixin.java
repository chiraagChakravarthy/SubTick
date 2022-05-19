package subtick.mixins.world_tickphases;

import carpet.helpers.TickSpeed;
import net.minecraft.entity.Entity;
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

import static subtick.TickProgress.*;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class EntityWorldMixin extends World implements StructureWorldAccess {
    protected EntityWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Inject(method = "tick",
            at=@At(value = "INVOKE",
                target = "Lnet/minecraft/world/EntityList;forEach(Ljava/util/function/Consumer;)V"))
    public void preEntities(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(ENTITIES, this.getRegistryKey());
    }

    @Inject(method="method_31420",
            at=@At(value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;tickEntity(Ljava/util/function/Consumer;Lnet/minecraft/entity/Entity;)V"))
    public void preEntity(Profiler profiler, Entity entity, CallbackInfo ci){
        int runStatus = TickProgress.runStatus();
        TickSpeed.process_entities = runStatus == RUN_COMPLETELY || runStatus == STEP_TO_FINISH;
    }
}
