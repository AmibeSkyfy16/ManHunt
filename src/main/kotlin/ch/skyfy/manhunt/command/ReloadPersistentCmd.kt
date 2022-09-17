package ch.skyfy.manhunt.command

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.config.persistent.Persistent
import ch.skyfy.manhunt.logic.Game
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class ReloadPersistentCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("reload").then(literal("persistent").executes(this)))
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

        ConfigManager.reloadConfig(Persistent.MANHUNT_PERSISTENT)
        ConfigManager.reloadConfig(Persistent.TIMELINE_PERSISTENT)
        context.source.sendMessage(Text.literal("Persistent files have been reloaded").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))

        return Command.SINGLE_SUCCESS
    }
}