package com.punchster2.subtick.subtick;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.helpers.TickSpeed;
import carpet.network.ServerNetworkHandler;
import carpet.settings.SettingsManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import subtick.commands.SubTickCommands;

public class Subtick implements CarpetExtension, ModInitializer
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