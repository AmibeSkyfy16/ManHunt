package ch.skyfy.manhunt.command

import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTED_ONES
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTERS
import ch.skyfy.manhunt.logic.Game
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class CreateStarterKitCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val literal = literal("create-kit")
            .then(literal("starter").executes(this))
            .then(literal("respawn").executes(this))

        val command = literal("manhunt")
            .then(
                literal(THE_HUNTERS)
                    .then(literal)
            )
            .then(
                literal(THE_HUNTED_ONES)
                    .then(literal)
            )
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {

        if (!context.source.hasPermissionLevel(4)) {
            context.source.sendMessage(Text.literal("This command required permission level 4").setStyle(Style.EMPTY.withColor(Formatting.RED)))
            return Command.SINGLE_SUCCESS
        }

        val player = context.source.player

        if(player == null){
            context.source.sendMessage(Text.literal("This command must be executed by a player").setStyle(Style.EMPTY.withColor(Formatting.RED)))
            return Command.SINGLE_SUCCESS
        }

        if(optGameRef.get().isEmpty){
            context.source.sendMessage(Text.literal("The server is not yet fully ready (Game object is null)").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            return Command.SINGLE_SUCCESS
        }

        val name = context.nodes[1].node.name
        val kitName = context.nodes[3].node.name

        val filename = "$kitName-kit-$name.dat"

        val nbtList = player.inventory.writeNbt(NbtList())
        val nbtCompound = NbtCompound()
        nbtCompound.put("inventory", nbtList)
        NbtIo.write(nbtCompound, ManHuntMod.CONFIG_DIRECTORY.resolve(filename).toFile())

        player.sendMessage(Text.literal("You have successfully save your inventory acting as $kitName kit for $name").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))

        return Command.SINGLE_SUCCESS
    }
}