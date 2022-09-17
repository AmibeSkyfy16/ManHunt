package ch.skyfy.manhunt.command

import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.logic.Game
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandSource.suggestMatching
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class GetKitCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("manhunt").then(literal("GetKit").then(argument("file", StringArgumentType.greedyString()).suggests { _, b ->
            suggestMatching(ManHuntMod.CONFIG_DIRECTORY.toFile().list { _, fileName -> fileName.substringAfterLast(".") == "dat" }?.toList(), b)
        }.executes(this)))
        dispatcher.register(command)
    }

    override fun run(context: CommandContext<ServerCommandSource>): Int {
        val player = context.source.player

        if (player == null) {
            context.source.sendMessage(Text.literal("This command must be executed by a player !").setStyle(Style.EMPTY.withColor(Formatting.RED)))
            return Command.SINGLE_SUCCESS
        }

        if(optGameRef.get().isEmpty){
            context.source.sendMessage(Text.literal("The server is not yet fully ready (Game object is null)").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            return Command.SINGLE_SUCCESS
        }

        if (!player.hasPermissionLevel(4) || !Configs.MANHUNT_CONFIG.serializableData.debug) {
            val message = Text.empty()
            if (!player.hasPermissionLevel(4) && !Configs.MANHUNT_CONFIG.serializableData.debug)
                message.append(Text.literal("This command required permission level 4 and debug mode must be set to true !"))
            else if (!player.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.serializableData.debug)
                message.append(Text.literal("This command required permission level 4"))
            else if (player.hasPermissionLevel(4) && !Configs.MANHUNT_CONFIG.serializableData.debug)
                message.append(Text.literal("This command required debug mode to true"))
            player.sendMessage(message.setStyle(Style.EMPTY.withColor(Formatting.RED)))
            return Command.SINGLE_SUCCESS
        }

        val nbtElement = NbtIo.read(ManHuntMod.CONFIG_DIRECTORY.resolve(StringArgumentType.getString(context, "file")).toFile())?.get("inventory")
        if (nbtElement is NbtList) {
            // TODO Also clear trinket slots https://github.com/emilyploszaj/trinkets
            player.inventory.clear()
            player.inventory.readNbt(nbtElement)
        }

        return Command.SINGLE_SUCCESS
    }
}