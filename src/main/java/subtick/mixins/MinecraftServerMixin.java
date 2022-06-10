package subtick.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.Highlights;
import subtick.progress.TickActions;
import subtick.progress.TickProgress;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
        extends ReentrantThreadExecutor<ServerTask>
        implements CommandOutput, AutoCloseable {

    public MinecraftServerMixin(String string) {
        super(string);
    }

    private boolean actuallyProcessEntities;

    @Inject(method = "tickWorlds", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/function/CommandFunctionManager;tick()V",
            shift= At.Shift.AFTER))
    public void preWorldsTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        actuallyProcessEntities = TickSpeed.process_entities;
        if(TickSpeed.process_entities){
            Highlights.clearHighlights((MinecraftServer) (Object)this);
            TickProgress.setTarget(TickProgress.POST_TICK);
        }
        TickActions.tick();
    }

    @Inject(method = "tickWorlds", at=@At(
                target = "Lnet/minecraft/server/MinecraftServer;getNetworkIo()Lnet/minecraft/server/ServerNetworkIo;",
                value = "INVOKE",
                shift = At.Shift.BEFORE))
    public void postWorldsTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        TickProgress.update(TickProgress.POST_TICK);

        TickSpeed.process_entities = actuallyProcessEntities;
        if(actuallyProcessEntities){
            TickProgress.setCurrent(TickProgress.PRE_TICK);
            TickProgress.setTarget(TickProgress.PRE_TICK);
        }
        TickActions.numActionsStep = 0;
    }
}