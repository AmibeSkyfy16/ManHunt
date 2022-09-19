package ch.skyfy.manhunt.command

import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTED_ONES
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTERS
import ch.skyfy.manhunt.logic.Game
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.item.ItemStack
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

class CreateBonusCmd(private val optGameRef: AtomicReference<Optional<Game>>) : Command<ServerCommandSource> {

    companion object{
        const val KILL_HUNTERS_BONUS = "kill-hunters-bonus"
        val KILL_HUNTERS_BONUS_PATH = CONFIG_DIRECTORY.resolve("$THE_HUNTED_ONES-$KILL_HUNTERS_BONUS.dat")
    }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val command = literal("create-bonus")
            .then(literal(THE_HUNTED_ONES).then(literal(KILL_HUNTERS_BONUS).executes(this)))

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

        val roleName = context.nodes[1].node.name
        val bonusName = context.nodes[2].node.name

//        val filename: String
//        if(roleName == THE_HUNTED_ONES && bonusName == KILL_HUNTERS_BONUS){
//            filename = KILL_HUNTERS_BONUS_PATH.fileName.toString()
//        }

        val filename = "$roleName-$bonusName.dat"

        val nbtList = player.inventory.writeNbt(NbtList())
        val nbtCompound = NbtCompound()
        nbtCompound.put("inventory", nbtList)
        NbtIo.write(nbtCompound, CONFIG_DIRECTORY.resolve(filename).toFile())

        player.sendMessage(Text.literal("You have successfully save your inventory acting as bonus ($bonusName)").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))

        //test
//        (if (KILL_HUNTERS_BONUS_PATH.toFile().exists()) NbtIo.read(KILL_HUNTERS_BONUS_PATH.toFile())?.let { it.get("inventory") as NbtList } else null)?.let{
//            it.forEach {nbtElement ->
//                if(nbtElement is NbtCompound){
//                    val itemStack = ItemStack.fromNbt(nbtElement)
//                    player.dropItem(itemStack.copy(), false, false)
//                }
//            }
//            println()
//        }

        return Command.SINGLE_SUCCESS
    }
}