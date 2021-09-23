package subtick.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.variables.Phase;

import java.util.function.BooleanSupplier;

import static subtick.variables.Variables.*;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
    extends ReentrantThreadExecutor<ServerTask>
    implements SnooperListener, CommandOutput, AutoCloseable {

    @Shadow private PlayerManager playerManager;

    public MinecraftServerMixin(String string) {super(string);}

    @Inject(method = "tickWorlds",at = @At("HEAD"))
    public void preWorldsTick(BooleanSupplier shouldKeepTicking,CallbackInfo ci) {
        frozenTickCount++;//used for playing
        actuallyProcessEntities = TickSpeed.process_entities;//lets target tick phase continue seamlessly

        if(TickSpeed.process_entities)
            setTargetPhase(World.END,Phase.POST_TICK,playerManager.getServer());

        inWorldTick = true;
    }

    @Inject(
        method = "tickWorlds",
        at = @At(
            target = "Lnet/minecraft/server/MinecraftServer;getNetworkIo()Lnet/minecraft/server/ServerNetworkIo;",
            value = "INVOKE"
        )
    )
    public void postWorldsTick(BooleanSupplier shouldKeepTicking,CallbackInfo ci) {
        inWorldTick = false;
        if(actuallyProcessEntities) {
            setTargetPhase(World.OVERWORLD,Phase.TICK_FREEZE,playerManager.getServer());
            currentTickPhase = Phase.TICK_FREEZE;
            currentDimension = World.OVERWORLD;
        }
    }
}