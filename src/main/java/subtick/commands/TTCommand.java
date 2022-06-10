package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import subtick.progress.TickActions;
import subtick.progress.TickProgress;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static subtick.progress.TickProgress.NUM_PHASES;

public class TTCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(literal("tt").then(
                literal("step").executes(
                        (c) -> commandStepTT(c, 1, 0)
                ).then(
                        argument("num", integer(1)).executes(
                                (c) -> commandStepTT(c, getInteger(c, "num"), 0)
                        ).then(
                                argument("ticks", integer(0)).executes(
                                        (c) -> commandStepTT(c, getInteger(c, "num"), getInteger(c, "ticks"))
                                )
                        )
                )
        ).then(
                literal("count").executes(TTCommand::commandCountTT)
        ));
    }

    private static int commandCountTT(CommandContext<ServerCommandSource> c){
        int progress = TickProgress.progressOf(tickPhase, c.getSource().getWorld().getRegistryKey());
        if(TickProgress.currentProgress != progress){
            Messenger.m(c.getSource(), "wi Must be stepping " + TickProgress.progressName(progress, true) + " to count");
            return 0;//TODO implement this entire system properly. Actually display what tile ticks are on what ticks
        }
        Messenger.m(c.getSource(), "gi " + c.getSource().getWorld().getBlockTickScheduler().getTickCount() +
                " Tile Ticks remaining");
        return 0;
    }

    private static final int tickPhase = TickProgress.TILE_TICKS;

    private static int commandStepTT(CommandContext<ServerCommandSource> c, int num, int ticks){
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
            Messenger.m(c.getSource(), "wi Must be in same dimension as current tick phase to step.");
            return 0;
        }

        if(progress < TickProgress.targetProgress){
            Messenger.m(c.getSource(), "wi " + TickProgress.tickPhaseNamesPlural[tickPhase] + " has already passed for this dimension. Use tick step to go to the beginning of the next tick");
            return 0;
        }

        TickProgress.setTarget(progress);
        TickActions.play(num, ticks, c.getSource());
        return 0;
    }

}
