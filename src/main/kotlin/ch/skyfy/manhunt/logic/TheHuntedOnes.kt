package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY
import ch.skyfy.manhunt.command.CreateBonusCmd.Companion.KILL_HUNTERS_BONUS_PATH
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class TheHuntedOnes(game: Game) : BaseRole(game, CONFIG_DIRECTORY.resolve("starter-kit-${ManHuntMod.THE_HUNTED_ONES}.dat").toFile(), CONFIG_DIRECTORY.resolve("respawn-kit-${ManHuntMod.THE_HUNTED_ONES}.dat").toFile()) {

    fun giveBonus(attacker: ServerPlayerEntity) {
        getKitAsNbtList(KILL_HUNTERS_BONUS_PATH.toFile())?.let {
            attacker.sendMessage(Text.literal("You have killed an hunter, some bonus item will be drop on the ground"))
            it.forEach {nbtElement ->
                if(nbtElement is NbtCompound)
                    attacker.dropItem(ItemStack.fromNbt(nbtElement).copy(), false, false)
            }
        }
    }

}