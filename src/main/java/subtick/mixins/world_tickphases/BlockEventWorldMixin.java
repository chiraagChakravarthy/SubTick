package subtick.mixins.world_tickphases;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.Highlights;
import subtick.SubTickSettings;
import subtick.progress.TickActions;
import subtick.progress.TickProgress;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static subtick.progress.TickProgress.*;

@Mixin(value = ServerWorld.class, priority = 999)
public abstract class BlockEventWorldMixin extends World implements StructureWorldAccess {
    @Shadow @Final public ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;

    @Shadow @Final private List<BlockEvent> blockEventQueue;

    @Shadow public abstract boolean shouldTickBlocksInChunk(long chunkPos);

    @Shadow protected abstract boolean processBlockEvent(BlockEvent event);

    @Shadow @Final private MinecraftServer server;

    protected BlockEventWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Inject(method="tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V"))
    public void preBlockEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        int runStatus = TickProgress.update(BLOCK_EVENTS, this.getRegistryKey());
        TickSpeed.process_entities = runStatus != NO_RUN;
    }

    private int bedCount = 0;

    @Inject(method = "processSyncedBlockEvents", at=@At("HEAD"), cancellable = true)
    public void onProcessBlockEvents(CallbackInfo ci){
        int runStatus = TickProgress.runStatus();
        if(runStatus == RUN_COMPLETELY){
            return;
        }
        ci.cancel();

        if(runStatus == STEP_FROM_START){
            this.blockEventQueue.clear();
            bedCount = syncedBlockEventQueue.size();
        }

        if(runStatus == STEP_FROM_START || runStatus == STEP){
            int total;
            if(TickActions.action==0) {
                total = stepBlockEvents();
            } else {
                total = stepBlockEventDelay();
            }

            if(TickActions.numActionsStep != 0 && !TickActions.stillPlaying()){
                int totalStepped;
                if(syncedBlockEventQueue.isEmpty()){
                    if(TickActions.ticksPerStep == 0){
                        totalStepped = total;
                    } else {
                        totalStepped = (TickActions.tickCount-1)/TickActions.ticksPerStep + total;
                    }
                } else {
                    totalStepped = TickActions.numActionsPlay;
                }
                if(totalStepped != 0)
                    Messenger.m(TickActions.actor, "gi Stepped " + totalStepped + " block event" + (TickActions.action==1 ?" delay" : (totalStepped==1?"":"s")));
            }
            TickActions.bedCount[TickProgress.dim(getRegistryKey())/NUM_PHASES] = bedCount;
        }

        if(runStatus == STEP_TO_FINISH){
            while(!this.syncedBlockEventQueue.isEmpty()) {
                BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();
                if (this.shouldTickBlocksInChunk(ChunkPos.toLong(blockEvent.pos()))) {
                    if (this.processBlockEvent(blockEvent)) {
                        sendBlockEvent(blockEvent);
                    }
                } else {
                    this.blockEventQueue.add(blockEvent);
                }
            }
            syncedBlockEventQueue.addAll(blockEventQueue);
        }
    }

    private int stepBlockEventDelay() {
        int total = 0;
        for (int i = 0; i < TickActions.numActionsStep; i++) {
            if(syncedBlockEventQueue.isEmpty()){
                Messenger.m(TickActions.actor, "gi No more Block Events in this dimension");
                TickActions.tickCount = TickActions.ticksPerStep * TickActions.numActionsPlay + 1;
                return total;
            }
            total++;
            int bedCount = this.bedCount;
            for (int j = 0; j < bedCount; j++) {
                if(syncedBlockEventQueue.isEmpty()){//just in case, shouldn't happen under 'normal' circumstances
                    Messenger.m(TickActions.actor, "gi No more Block Events in this dimension");
                    TickActions.tickCount = TickActions.ticksPerStep * TickActions.numActionsPlay + 1;
                    TickActions.action = 0;
                    return total;
                }
                BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();

                if(this.shouldTickBlocksInChunk(ChunkPos.toLong(blockEvent.pos()))){
                    if (this.processBlockEvent(blockEvent)) {
                        sendBlockEvent(blockEvent);
                    }
                } else {
                    this.blockEventQueue.add(blockEvent);
                }
            }
            this.bedCount = syncedBlockEventQueue.size();
        }
        return total;
    }

    private int stepBlockEvents(){
        int total = 0;
        for (int i = 0; i < TickActions.numActionsStep; i++) {
            if(syncedBlockEventQueue.isEmpty()){
                Messenger.m(TickActions.actor, "gi No more Block Events in this dimension");
                TickActions.tickCount = TickActions.ticksPerStep * TickActions.numActionsPlay + 1;
                return total;
            }
            BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();
            bedCount--;

            if(this.shouldTickBlocksInChunk(ChunkPos.toLong(blockEvent.pos()))){
                if(!SubTickSettings.skipInvalidEvents || this.getBlockState(blockEvent.pos()).isOf(blockEvent.block())){
                    total++;
                    Highlights.executedHighlight(blockEvent.pos(), (ServerWorld)(Object)this);
                    if (this.processBlockEvent(blockEvent)) {
                        sendBlockEvent(blockEvent);
                    }
                }
            } else {
                this.blockEventQueue.add(blockEvent);
                i--;
            }

            if(bedCount==0){
                bedCount = syncedBlockEventQueue.size();
            }
        }
        return total;
    }

    private void sendBlockEvent(BlockEvent blockEvent){
        this.server
                .getPlayerManager()
                .sendToAround(
                        null,
                        (double)blockEvent.pos().getX(),
                        (double)blockEvent.pos().getY(),
                        (double)blockEvent.pos().getZ(),
                        64.0,
                        this.getRegistryKey(),
                        new BlockEventS2CPacket(blockEvent.pos(), blockEvent.block(), blockEvent.type(), blockEvent.data())
                );
    }
}
