package subtick.mixins.world_tickphases;

import carpet.helpers.TickSpeed;
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

import static subtick.TickProgress.NO_RUN;
import static subtick.TickProgress.RAIDS;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class RaidsWorldMixin extends World implements StructureWorldAccess {
    protected RaidsWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    private boolean actualProcessEntities;

    @Inject(method="tick", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/village/raid/RaidManager;tick()V"))
    public void preRaids(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(RAIDS, this.getRegistryKey());
        int runStatus = TickProgress.runStatus();
        actualProcessEntities = TickSpeed.process_entities;
        TickSpeed.process_entities = runStatus != NO_RUN;
    }

    @Inject(method="tick", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/village/raid/RaidManager;tick()V",
            shift = At.Shift.AFTER))
    public void postRaids(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickSpeed.process_entities = actualProcessEntities;
    }
}
