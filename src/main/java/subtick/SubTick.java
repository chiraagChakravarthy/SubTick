package subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.helpers.TickSpeed;
import carpet.network.ServerNetworkHandler;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import subtick.commands.SubTickCommands;

import java.util.logging.Level;

public class SubTick implements CarpetExtension, ModInitializer
{
    @Override
    public String version() {
        return "subtick";
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new SubTick());
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(SubTickSettings.class);
        SettingsManager.addGlobalRuleObserver((c, rule, state)->{
            if(rule.name.equals("carpetClientFreeze")){
                ServerNetworkHandler.updateFrozenStateToConnectedPlayers();
            }
        });
    }

    public static void say(ServerWorld world, String msg){
        world.getServer().getPlayerManager().broadcast(new LiteralText(msg), MessageType.CHAT, null);
    }
    
    @Override
    public void onServerLoadedWorlds(MinecraftServer minecraftServer){
        //A better way to check if carpetExtra is loaded
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        SubTickCommands.register(dispatcher);
    }

    @Override
    public void onTick(MinecraftServer server) {

    }
}