package ch.skyfy.manhunt.command

import ch.skyfy.jsonconfiglib.update
import ch.skyfy.manhunt.config.persistent.GameState
import ch.skyfy.manhunt.config.persistent.ManHuntPersistent
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

class StartCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("start").executes(this))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        if (optGameRef.get().isEmpty) return Command.SINGLE_SUCCESS
        val game = optGameRef.get().get()

//        val player = context.source.player!!
//        val item = Items.COMPASS as CompassItem
//        val stack = player.getStackInHand(player.activeHand)

        val configData = Persistent.MANHUNT_PERSISTENT
        if (configData.serializableData.gameState == GameState.NOT_STARTED) {
            configData.update(ManHuntPersistent::gameState, GameState.STARTING)
            optGameRef.get().ifPresent(Game::cooldownStart)
        } else context.source.player?.sendMessage(Text.literal("The game is already started").setStyle(Style.EMPTY.withColor(Formatting.RED)))

        return Command.SINGLE_SUCCESS
    }
}