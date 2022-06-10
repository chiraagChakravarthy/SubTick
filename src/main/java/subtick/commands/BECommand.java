package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import subtick.progress.TickActions;
import subtick.progress.TickProgress;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static net.minecraft.server.command.CommandManager.argument;
import static subtick.progress.TickProgress.NUM_PHASES;

public class BECommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("be").then(
                CommandManager.literal("step").executes(
                        (c) -> commandStepBE(c, 1, 0, 0)
                ).then(
                        argument("num", integer(1)).executes(
                                (c) -> commandStepBE(c, getInteger(c, "num"), 0, 0)
                        ).then(
                                argument("ticks", integer(0)).executes(
                                        (c) -> commandStepBE(c, getInteger(c, "num"), getInteger(c, "ticks"), 0)
                                )
                        )
                )
        ).then(
                CommandManager.literal("count").executes((c) -> commandCountBE(c, 0))
        ));

        dispatcher.register(CommandManager.literal("bed").then(
                CommandManager.literal("step").executes(
                        (c) -> commandStepBE(c, 1, 0, 1)
                ).then(
                        argument("num", integer(1)).executes(
                                (c) -> commandStepBE(c, getInteger(c, "num"), 0, 1)
                        ).then(
                                argument("ticks", integer(0)).executes(
                                        (c) -> commandStepBE(c, getInteger(c, "num"), getInteger(c, "ticks"), 1)
                                )
                        )
                )
        ).then(
                CommandManager.literal("count").executes((c) -> commandCountBE(c, 1))
        ));

    }

    private static int commandCountBE(CommandContext<ServerCommandSource> c, int action){
        if(action==0){
            Messenger.m(c.getSource(), "wi " + c.getSource().getWorld().syncedBlockEventQueue.size() + " Block Events remaining");
        } else {
            int bedCount;
            if(TickProgress.currentProgress == TickProgress.progressOf(tickPhase, c.getSource().getWorld().getRegistryKey())){
                bedCount = TickActions.bedCount[TickProgress.dim(c.getSource().getWorld().getRegistryKey())/NUM_PHASES];
            } else {
                bedCount = c.getSource().getWorld().syncedBlockEventQueue.size();
            }
            Messenger.m(c.getSource(), "wi " + bedCount
                                        + " Block Events remaining at the current depth");
        }
        return 0;
    }

    private static final int tickPhase = TickProgress.BLOCK_EVENTS;

    private static int commandStepBE(CommandContext<ServerCommandSource> c, int num, int ticks, int action){
        if(TickSpeed.process_entities){
            Messenger.m(c.getSource(), "wi Game is not frozen");
            return 0;
        }

        if(TickActions.stillPlaying()){
            Messenger.m(c.getSource(), "wi Already stepping " + TickProgress.progressName(tickPhase));
            return 0;
        }

        int progress = TickProgress.progressOf(tickPhase, c.getSource().getWorld().getRegistryKey());
        if(TickProgress.getDimension(progress) != TickProgress.dim(c.getSource().getWorld().getRegistryKey())){
            Messenger.m(c.getSource(), "wi Must be in same dimension as current tick phase to step.");//trust me, its better this way
            return 0;
        }

        if(progress < TickProgress.targetProgress){
            Messenger.m(c.getSource(), "wi " + TickProgress.tickPhaseNamesPlural[tickPhase] + " has already passed for this dimension. Use tick step to go to the beginning of the next tick");
            return 0;
        }

        TickProgress.setTarget(progress);
        TickActions.play(num, ticks, c.getSource(), action);
        return 0;
    }
}
