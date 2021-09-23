package subtick.commands;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import subtick.variables.Phase;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static subtick.variables.Variables.*;

public class SubTickCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("be")
                .then(
                    literal("step")
                        .then(
                            argument("count",integer(1))
                                .executes(c -> beStep(c,getInteger(c,"count")))
                        )
                        .executes(SubTickCommands::beStep)
                )
                .then(
                    literal("play")
                        .then(
                            argument("interval",integer(1))
                                .then(
                                    argument("count",integer(1))
                                        .executes(c -> bePlay(c,getInteger(c,"interval"),getInteger(c,"count")))
                                )
                                .executes(c -> bePlay(c,getInteger(c,"interval"),Integer.MAX_VALUE))
                        )
                        .executes(SubTickCommands::bePlay)
                )
                .then(
                    literal("count")
                        .executes(SubTickCommands::beCount)
                )
        );

        dispatcher.register(
            literal("bed")
                .then(
                    literal("step")
                        .then(
                            argument("count",integer(1))
                                .executes(c -> bedStep(c,getInteger(c,"count")))
                        )
                        .executes(SubTickCommands::bedStep)
                )
                .then(
                    literal("play")
                        .then(
                            argument("interval",integer(1))
                                .then(
                                    argument("count",integer(1))
                                        .executes(c -> bedPlay(c,getInteger(c,"interval"),getInteger(c,"count")))
                                )
                                .executes(c -> bedPlay(c,getInteger(c,"interval"),Integer.MAX_VALUE))
                        )
                        .executes(SubTickCommands::bedPlay)
                )
        );

        dispatcher.register(
            literal("when")
                .executes(SubTickCommands::when)
        );
    }

    private static boolean cancelPlay(ServerCommandSource src) {
        if(bePlay != 0 || bedPlay != 0) {
            bePlay = 0;
            bedPlay = 0;
            Messenger.m(src,"w Cancelled playing block events");
            return true;
        }
        return false;
    }
    private static boolean assertTickFrozen(ServerCommandSource src) {
        if(TickSpeed.process_entities) {
            Messenger.m(src,"w must be in tick freeze");
            return false;
        }
        return true;
    }
    private static boolean assertMoreBlockEvents(ServerCommandSource src,RegistryKey<World> dim) {
        if(getData(dim).beCount == 0) {
            Messenger.m(src,"w no more block events in this dimension");
            return false;
        }
        return true;
    }
    private static void setTarget(RegistryKey<World> dim,ServerCommandSource src,int count,boolean setPos) {
        beStep = count;
        setTargetPhase(dim,Phase.BLOCK_EVENTS,src.getServer());
        if(setPos) commandSrcPos = src.getPosition();
    }
    private static void setTarget(RegistryKey<World> dim,ServerCommandSource src,int count,boolean setPos,int interval) {
        setTarget(dim,src,count,setPos);
        playInterval = interval;
        playStart = frozenTickCount - 1;
    }
    private static void assertNotPlaying(ServerCommandSource src,int count,int interval,boolean setPos) {
        if(bePlay == 0 && bedPlay == 0) {
            bePlay = count;
            playInterval = interval;
            playStart = frozenTickCount - 1;
            if(setPos) commandSrcPos = src.getPosition();
        } else
            Messenger.m(src,"w Already playing block events");
    }
    @FunctionalInterface interface SetTargetFunctor {void apply(RegistryKey<World> dim,ServerCommandSource src);}
    @FunctionalInterface interface MoreBlockEventsFunctor {void apply(ServerCommandSource src);}
    private static void tick(CommandContext<ServerCommandSource> c,boolean assertsPlay,
                             SetTargetFunctor setTarget,MoreBlockEventsFunctor moreBE) {
        final ServerCommandSource src = c.getSource();
        // '!(cancelPlay(src) && assertsPlay)' runs the function and enforces the result iff the flag is set.
        if(!(cancelPlay(src) && assertsPlay) && assertTickFrozen(src)) {
            final RegistryKey<World> dim = src.getWorld().getRegistryKey();
            if(targetBefore(dim,Phase.BLOCK_EVENTS))
                setTarget.apply(dim,src);
            else if(!isAtTarget(dim,Phase.BLOCK_EVENTS))
                Messenger.m(src,"w "+Phase.BLOCK_EVENTS.plural()+" has passed for this dimension");
            else if(assertMoreBlockEvents(src,dim))
                moreBE.apply(src);
        }
    }

    private static int beStep(CommandContext<ServerCommandSource> c) {return beStep(c,1);}
    private static int beStep(CommandContext<ServerCommandSource> c,int count) {
        tick(
            c,false,
            (dim,src) -> setTarget(dim,src,count,true),
            src -> {beStep = count; commandSrcPos = src.getPosition();}
        );
        return 0;
    }
    private static int bePlay(CommandContext<ServerCommandSource> c) {return bePlay(c,1,0);}
    private static int bePlay(CommandContext<ServerCommandSource> c,int interval,int count) {
        tick(
            c,true,
            (dim,src) -> setTarget(dim,src,count,true,interval),
            src -> assertNotPlaying(src,count,interval,true)
        );
        return 0;
    }

    private static int beCount(CommandContext<ServerCommandSource> c) {
        final ServerCommandSource src = c.getSource();
        Messenger.m(src,"w " + getData(src.getWorld().getRegistryKey()).beCount);
        return 0;
    }

    private static int bedStep(CommandContext<ServerCommandSource> c) {return bedStep(c,1);}
    private static int bedStep(CommandContext<ServerCommandSource> c,int count) {
        tick(
            c,false,
            (dim,src) -> setTarget(dim,src,count,false),
            src -> bedStep = count
        );
        return 0;
    }
    private static int bedPlay(CommandContext<ServerCommandSource> c) {return bedPlay(c,1,0);}
    private static int bedPlay(CommandContext<ServerCommandSource> c,int interval,int count) {
        tick(
            c,true,
            (dim,src) -> setTarget(dim,src,count,false,interval),
            src -> assertNotPlaying(src,count,interval,false)
        );
        return 0;
    }

    private static int when(CommandContext<ServerCommandSource> c) {
        final ServerCommandSource src = c.getSource();
        if(assertTickFrozen(src))
            Messenger.m(
                src,
                "w Currently running " + currentTickPhase.plural() +
                " in the " + getData(targetDimension).name
            );
        return 0;
    }
}