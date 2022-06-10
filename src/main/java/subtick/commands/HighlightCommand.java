package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import subtick.Highlights;
import subtick.progress.TickProgress;

import static net.minecraft.server.command.CommandManager.literal;


public class HighlightCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(
                literal("highlight").then(
                        literal("executed").executes((c) -> commandHighlight(c, Highlights.EXECUTED, 0))
                ).then(
                        literal("new").executes((c) -> commandHighlight(c, Highlights.NEW_EVENTS, TickProgress.NUM_PHASES)).then(
                                literal("tt").executes((c)->commandHighlight(c, Highlights.NEW_EVENTS, TickProgress.TILE_TICKS))
                        ).then(
                                literal("be").executes((c)->commandHighlight(c, Highlights.NEW_EVENTS, TickProgress.BLOCK_EVENTS))
                        )
                ).then(
                        literal("none").executes((c)->commandHighlight(c, Highlights.NONE, 0))
                )
        );
    }

    private static int commandHighlight(CommandContext<ServerCommandSource> c, int type, int phase){

        if(type == Highlights.EXECUTED){
            Highlights.toggleShowExecuted(c.getSource().getWorld());
            Messenger.m(c.getSource(), "gi " + (Highlights.showingExecuted?"Executed events highlighted" : "Executed events no longer highlighted"));
        } else if(type == Highlights.NEW_EVENTS){
            Highlights.toggleShowNew(c.getSource().getWorld(), phase);
            if(phase==TickProgress.NUM_PHASES){
                Messenger.m(c.getSource(), "gi " + (Highlights.showingNew[0]?"All New events highlighted": "All New events no longer highlighted"));
            } else {
                Messenger.m(c.getSource(), "gi New " + TickProgress.tickPhaseNamesPlural[phase] + (Highlights.showingNew[phase]?" highlighted":" no longer highlighted"));
            }
        } else if(type==Highlights.NONE){
            Highlights.showNone(c.getSource().getWorld());
            Messenger.m(c.getSource(), "gi All highlights cleared");
        }
        return 0;
    }
}
