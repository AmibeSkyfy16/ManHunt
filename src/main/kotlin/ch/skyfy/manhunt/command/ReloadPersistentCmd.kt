package ch.skyfy.manhunt.command

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.config.persistent.Persistent
import ch.skyfy.manhunt.logic.Game
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class ReloadPersistentCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("reload").then(literal("persistent").executes(this)))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        ConfigManager.reloadConfig(Persistent.MANHUNT_PERSISTENT)
        ConfigManager.reloadConfig(Persistent.TIMELINE_PERSISTENT)
        return Command.SINGLE_SUCCESS
    }
}