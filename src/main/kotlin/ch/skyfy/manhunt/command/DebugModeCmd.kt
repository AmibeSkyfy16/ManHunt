package ch.skyfy.manhunt.command

import ch.skyfy.jsonconfiglib.update
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.ManHuntConfig
import ch.skyfy.manhunt.logic.Game
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
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class DebugModeCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("debug").then(argument("value", BoolArgumentType.bool()).executes(this)))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {

        if (!context.source.hasPermissionLevel(4)) {
            context.source.sendMessage(Text.literal("This command required permission level 4").setStyle(Style.EMPTY.withColor(Formatting.RED)))
            return Command.SINGLE_SUCCESS
        }

        if(optGameRef.get().isEmpty){
            context.source.sendMessage(Text.literal("The server is not yet fully ready (Game object is null)").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            return Command.SINGLE_SUCCESS
        }

        val value = BoolArgumentType.getBool(context, "value")
        val configValue = Configs.MANHUNT_CONFIG.serializableData.debug

        if (configValue == value) context.source.sendMessage(Text.literal("Debug mode value is already $value").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        else {
            context.source.sendMessage(Text.literal("Debug mode has been set to $value").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
            Configs.MANHUNT_CONFIG.update(ManHuntConfig::debug, value)
        }
        return Command.SINGLE_SUCCESS
    }
}