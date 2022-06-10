package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import subtick.progress.TickProgress;

import static net.minecraft.server.command.CommandManager.literal;

public class WhenCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("when").executes((c) -> commandWhen(c, false)).then(
                    literal("verbose").executes((c) -> commandWhen(c, true))
                )
        );
    }

    private static int commandWhen(CommandContext<ServerCommandSource> c, boolean verbose){
        if(TickSpeed.process_entities){
            Messenger.m(c.getSource(), "wi Game is not fr" +
                    "ozen");
        } else {
            if(verbose){
                int tickPhase = TickProgress.getTickPhase(TickProgress.targetProgress);

                for (int i = 0; i < TickProgress.NUM_PHASES; i++) {
                    Messenger.m(c.getSource(), "wi " + (i==tickPhase?"> ":"* ") + TickProgress.tickPhaseNamesPlural[i]);
                }
            } else {
                Messenger.m(c.getSource(), "gi Currently running " + TickProgress.progressName(TickProgress.targetProgress, true));
            }
        }
        return 0;
    }
}
