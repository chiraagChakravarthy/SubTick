package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import subtick.Highlights;
import subtick.progress.TickProgress;

import static net.minecraft.server.command.CommandManager.argument;
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
        if(!TickSpeed.isPaused()){
            Messenger.m(c.getSource(), "wi Must be in tick freeze to highlight");
            return 0;
        }

        if(type == Highlights.EXECUTED){
            Messenger.m(c.getSource(), "gi Highlighting Executed Events");
            Highlights.showExecuted(c.getSource().getWorld());
        } else if(type == Highlights.NEW_EVENTS){
            Messenger.m(c.getSource(), "gi Highlighting New Events");
            Highlights.showNew(c.getSource().getWorld(), phase);
        } else if(type==Highlights.NONE){
            Messenger.m(c.getSource(), "gi Removing all highlights");
            Highlights.showNone(c.getSource().getWorld());
        }
        return 0;
    }
}
