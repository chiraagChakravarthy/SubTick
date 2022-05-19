package subtick.commands;

import carpet.helpers.TickSpeed;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import subtick.progress.TickProgress;

public class NextCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        dispatcher.register(CommandManager.literal("next").executes(NextCommand::commandNext));
    }

    private static int commandNext(CommandContext<ServerCommandSource> c){
        if(TickSpeed.process_entities){
            c.getSource().sendFeedback(new LiteralText("Must be in tick freeze"), false);
            return 0;
        }
        if(TickProgress.targetProgress == TickProgress.POST_TICK){
            c.getSource().sendFeedback(new LiteralText("Already at the end of the tick"), false);
            return 0;
        }
        TickProgress.targetProgress++;
        c.getSource().sendFeedback(new LiteralText("Currently Running " + TickProgress.progressName(TickProgress.targetProgress)), false);
        return 0;
    }
}
