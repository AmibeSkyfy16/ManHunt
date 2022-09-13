package ch.skyfy.manhunt.command

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.config.Configs
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class DebugModeCmd : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("debug").then(argument("value", BoolArgumentType.bool()).executes(this)))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player ?: return Command.SINGLE_SUCCESS

        val value = BoolArgumentType.getBool(context, "value")
        val configValue = Configs.MANHUNT_CONFIG.`data`.debug

        if(configValue == value) player.sendMessage(Text.literal("Debug value is already false").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        else{
            player.sendMessage(Text.literal("Debug value has been set to $value").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
            ConfigManager.computeAndSave(Configs.MANHUNT_CONFIG, {manHuntConfig -> manHuntConfig.debug = value })
        }
        return Command.SINGLE_SUCCESS
    }
}