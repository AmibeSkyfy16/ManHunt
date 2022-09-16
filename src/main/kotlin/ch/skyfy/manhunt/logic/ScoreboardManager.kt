package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.config.persistent.Persistent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.silkmc.silk.core.annotations.DelicateSilkApi
import net.silkmc.silk.core.task.silkCoroutineScope
import net.silkmc.silk.core.text.literal
import net.silkmc.silk.game.sideboard.Sideboard
import net.silkmc.silk.game.sideboard.SideboardLine
import net.silkmc.silk.game.sideboard.sideboard

class ScoreboardManager(private val minecraftServer: MinecraftServer) {

    private data class PlayerSideboard(
        val uuid: String,
        val sideboard: Sideboard,
        val roleLine: SideboardLine.Updatable,
        val gameStateLine: SideboardLine.Updatable
    )

    private val sideboards = mutableSetOf<PlayerSideboard>()

    init { initialize() }

    @OptIn(DelicateSilkApi::class)
    private fun initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> sideboards.removeIf { it.uuid == handler.player.uuidAsString } }

        ServerEntityEvents.ENTITY_LOAD.register{ entity, _ ->
            if(entity !is ServerPlayerEntity)return@register
            val playerUUID = entity.uuidAsString
            if(sideboards.none { it.uuid == playerUUID }){
                val roleLine = SideboardLine.Updatable("Role: ${GameUtils.getPlayerRole(entity.name.string)}".literal)
                val gameStateLine = SideboardLine.Updatable("Status: ${Persistent.MANHUNT_PERSISTENT.serializableData.gameState.displayName}".literal)
                val mySideboard = sideboard("<< Main Board >>".literal) {
                    line(Text.empty())
                    line(roleLine)
                    line(gameStateLine)
                }
                mySideboard.displayToPlayer(entity)
                sideboards.add(PlayerSideboard(playerUUID, mySideboard, roleLine, gameStateLine))
                silkCoroutineScope.launch {
                    delay(1000)
                    updateSideBoard(entity.uuidAsString, entity.name.string)
                }
            }
        }
    }

    fun updateSideboard(){
        minecraftServer.playerManager.playerList.forEach { serverPlayerEntity ->
            updateSideBoard(serverPlayerEntity.uuidAsString, serverPlayerEntity.name.string)
        }
    }

    private fun updateSideBoard(uuid: String, playerName: String){
        sideboards.find { it.uuid == uuid }?.let {
            it.roleLine.launchUpdate("Role: ${GameUtils.getPlayerRole(playerName).displayName}".literal.setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            it.gameStateLine.launchUpdate("Status: ${Persistent.MANHUNT_PERSISTENT.serializableData.gameState.displayName}".literal.setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        }
    }

}