package subtick.mixins;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.EntityList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.progress.TickProgress;

import java.util.function.Consumer;

import static subtick.progress.TickProgress.*;

@Mixin(EntityList.class)
public class EntityListMixin {
    @Shadow private @Nullable Int2ObjectMap<Entity> iterating;

    @Shadow private Int2ObjectMap<Entity> entities;

    @Inject(method = "forEach", at=@At("HEAD"), cancellable = true)
    public void entityStep(Consumer<Entity> action, CallbackInfo ci){
        if(entities.isEmpty() || entities.values().iterator().next().world.isClient){
            iterating = null;
            return;
        }

        int runStatus = TickProgress.runStatus();
        if(runStatus==RUN_COMPLETELY || runStatus==NO_RUN){
            iterating = null;
            return;
        }
        ci.cancel();
        if(runStatus==STEP_FROM_START){
            this.iterating = entities;
        }

        //probably not possible for iterating to be null here
        try {
            for (Entity entity : this.iterating.values()) {
                action.accept(entity);
            }
        } catch (Throwable t){
            if(iterating == null){
                throw t;
            }
            if(targetProgress != STEP_TO_FINISH){
                targetProgress++;
                TickProgress.update(ENTITIES, getDimension(targetProgress));
                System.err.println("Stepped past " + progressName(targetProgress-1) + " due to entity crash");
            }
        }

        if(runStatus==STEP_TO_FINISH){
            iterating = null;
        }
    }
}
