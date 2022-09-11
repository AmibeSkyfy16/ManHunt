package ch.skyfy.manhunt.command

import ch.skyfy.manhunt.ManHuntMod
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
        val command = literal("manhunt")
            .then(literal("hunters").then(literal("createStarterKit").executes(this)))
            .then(literal("theHuntedOnes").then(literal("createStarterKit").executes(this)))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        if (optGameRef.get().isEmpty) return Command.SINGLE_SUCCESS
        val player = context.source.player ?: return Command.SINGLE_SUCCESS

        val type = context.nodes[1]
        val name = type.node.name
        val filename = "starter-kit-$name.dat"

        val nbtList = player.inventory.writeNbt(NbtList())
        val nbtCompound = NbtCompound()
        nbtCompound.put("inventory", nbtList)
        NbtIo.write(nbtCompound, ManHuntMod.CONFIG_DIRECTORY.resolve(filename).toFile())

        player.sendMessage(Text.literal("You have successfully save your inventory acting as starter kit for $name").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))

        return Command.SINGLE_SUCCESS
    }
}