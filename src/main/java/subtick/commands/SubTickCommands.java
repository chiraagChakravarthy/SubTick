package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import subtick.variables.Variables;
import subtick.variables.WorldData;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SubTickCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("be")
                .then(literal("step")
                        .then(argument("count", integer(1)).executes((c) -> beStep(c, getInteger(c, "count"))))
                        .executes((c) -> beStep(c, 1))
                )

                .then(literal("play")
                    .then(argument("interval", integer(1))
                        .then(argument("count", integer(1))
                            .executes((c)-> bePlay(c, getInteger(c, "interval"), getInteger(c, "count"))))
                        .executes((c)->bePlay(c, getInteger(c, "interval"), Integer.MAX_VALUE))
                    )
                    .executes((c)-> bePlay(c, 1, 0))
                )
                .then(literal("count").executes(SubTickCommands::beCount)));

        dispatcher.register(literal("bed")
                .then(literal("step")
                        .then(argument("count", integer(1)).executes((c) -> bedStep(c, getInteger(c, "count"))))
                        .executes(c -> bedStep(c, 1))
                )

                .then(literal("play")
                    .then(argument("interval", integer(1))
                        .then(argument("count", integer(1))
                            .executes((c)-> bedPlay(c, getInteger(c, "interval"), getInteger(c, "count"))))
                        .executes((c)->bedPlay(c, getInteger(c, "interval"), Integer.MAX_VALUE))
                    )
                    .executes((c)-> bedPlay(c, 1, 0))
                ));

        dispatcher.register(literal("when").executes(SubTickCommands::when));
    }

     private static int beStep(CommandContext<ServerCommandSource> c, int count){
         if(Variables.bePlay!=0||Variables.bedPlay!=0){
             //cancelled
             Variables.bePlay = 0;
             Variables.bedPlay = 0;
             Messenger.m(c.getSource(), "w Cancelled playing block events");
         }
         RegistryKey<World> dimension = c.getSource().getWorld().getRegistryKey();

         if(TickSpeed.process_entities){
             Messenger.m(c.getSource(), "w must be in tick freeze");
         } else if(Variables.targetBefore(dimension, Variables.BLOCK_EVENTS)){
             Variables.beStep = count;
             Variables.setTargetPhase(dimension, Variables.BLOCK_EVENTS, c.getSource().getServer());
             Variables.commandSource = c.getSource();
         } else if(Variables.isAtTarget(dimension, Variables.BLOCK_EVENTS)){
             if(c.getSource().getWorld().syncedBlockEventQueue.size()==0){
                 Messenger.m(c.getSource(), "w no more block events in this dimension");
             } else {
                 Variables.beStep = count;
                 Variables.commandSource = c.getSource();
             }
         } else {
             Messenger.m(c.getSource(), "w Block Events has passed for this dimension");
         }
         return 0;
    }

    private static int bePlay(CommandContext<ServerCommandSource> c, int interval, int count){
        if(Variables.bePlay!=0||Variables.bedPlay!=0){
            //cancelled
            Variables.bePlay = 0;
            Variables.bedPlay = 0;
            Messenger.m(c.getSource(), "w Cancelled playing block events");
            return 0;
        }

        int beCount = c.getSource().getWorld().syncedBlockEventQueue.size();

        RegistryKey<World> dimension = c.getSource().getWorld().getRegistryKey();

        if(TickSpeed.process_entities){
            Messenger.m(c.getSource(), "w Must be in tick freeze");
        } else if(Variables.targetBefore(dimension, Variables.BLOCK_EVENTS)){
            Variables.setTargetPhase(dimension, Variables.BLOCK_EVENTS, c.getSource().getServer());
            Variables.bePlay = count;
            Variables.playInterval = interval;
            Variables.playStart = Variables.frozenTickCount-1;
            Variables.commandSource = c.getSource();
        } else if(Variables.isAtTarget(dimension, Variables.BLOCK_EVENTS)){
            if(beCount==0){
                Messenger.m(c.getSource(), "w No more block events in this dimension");
            } else {
                if(Variables.bePlay==0&&Variables.bedPlay==0){
                    Variables.bePlay = count;
                    Variables.playInterval = interval;
                    Variables.playStart = Variables.frozenTickCount-1;
                    Variables.commandSource = c.getSource();
                } else {
                    Messenger.m(c.getSource(), "w Already playing block events");
                }
            }
        } else {
            Messenger.m(c.getSource(), "w Block events has passed for this dimension");
        }
        return 0;
    }

    private static int beCount(CommandContext<ServerCommandSource> c){
        Messenger.m(c.getSource(), "w " + c.getSource().getWorld().syncedBlockEventQueue.size());
        return 0;
    }

    private static int bedStep(CommandContext<ServerCommandSource> c, int count){
        if(Variables.bePlay!=0||Variables.bedPlay!=0){
            //cancelled
            Variables.bePlay = 0;
            Variables.bedPlay = 0;
            Messenger.m(c.getSource(), "w Cancelled playing block events");
        }
        RegistryKey<World> dimension = c.getSource().getWorld().getRegistryKey();
        WorldData data = Variables.getData(dimension);
        if(TickSpeed.process_entities){
            Messenger.m(c.getSource(), "w Must be in tick freeze");
        } else if(Variables.targetBefore(dimension, Variables.BLOCK_EVENTS)){
            Variables.bedStep = count;
            Variables.setTargetPhase(dimension, Variables.BLOCK_EVENTS, c.getSource().getServer());
        } else if(Variables.isAtTarget(dimension, Variables.BLOCK_EVENTS)){
            if(c.getSource().getWorld().syncedBlockEventQueue.size()==0){
                Messenger.m(c.getSource(), "w No more block events in this dimension");
            } else {
                Variables.bedStep = count;
            }
        } else {
            Messenger.m(c.getSource(), "w Block events has passed for this dimension");
        }
        return 0;
    }

    public static int bedPlay(CommandContext<ServerCommandSource> c, int interval, int count){
        if(Variables.bePlay!=0||Variables.bedPlay!=0){
            //cancelled
            Variables.bePlay = 0;
            Variables.bedPlay = 0;
            Messenger.m(c.getSource(), "w Cancelled playing block events");
            return 0;
        }
        RegistryKey<World> dimension = c.getSource().getWorld().getRegistryKey();
        WorldData data = Variables.getData(dimension);
        if(TickSpeed.process_entities){
            Messenger.m(c.getSource(), "w Must be in tick freeze");
        } else if(Variables.targetBefore(dimension, Variables.BLOCK_EVENTS)){
            Variables.setTargetPhase(dimension, Variables.BLOCK_EVENTS, c.getSource().getServer());
            Variables.bedPlay = count;
            Variables.playInterval = interval;
            Variables.playStart = Variables.frozenTickCount-1;
        } else if(Variables.isAtTarget(dimension, Variables.BLOCK_EVENTS)){
            if(c.getSource().getWorld().syncedBlockEventQueue.size()==0){
                Messenger.m(c.getSource(), "w No more block events in this dimension");
            } else {
                if(Variables.bePlay==0&&Variables.bedPlay==0){
                    Variables.bedPlay = count;
                    Variables.playInterval = interval;
                    Variables.playStart = Variables.frozenTickCount-1;
                } else {
                    Messenger.m(c.getSource(), "w Already playing block events");
                }
            }
        } else {
            Messenger.m(c.getSource(), "w Block events has passed for this dimension");
        }
        return 0;
    }

    private static int when(CommandContext<ServerCommandSource> c){
        Messenger.m(c.getSource(), "w " + (TickSpeed.process_entities ? "Game is not frozen"
                :"Currently running " + Variables.tickPhasePluralNames[Variables.currentTickPhase]
                + " in the " + Variables.getData(Variables.targetDimension).name));
        return 0;
    }
}