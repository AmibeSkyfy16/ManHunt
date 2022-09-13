package ch.skyfy.manhunt.utils

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

object MathUtils {

    /**
     * If a player is inside a cube, and tries to get out, he is teleported one step back
     */
    fun cancelPlayerFromLeavingAnArea(player: ServerPlayerEntity, cube: Cube, spawnLocation: SpawnLocation?): Boolean {

//        if(
//            player.blockX > cube.x + cube.size ||
//            player.blockX < cube.x - cube.size ||
//            player.blockZ > cube.z + cube.size ||
//            player.blockZ < cube.z - cube.size
//        ){
//            player.teleport(spawnLocation.x, spawnLocation.y, spawnLocation.x)
//        }

        var vec: Vec3d? = null
        if (player.blockX > cube.x + cube.size) {
            vec = Vec3d(player.x - 1, player.y, player.z)
        } else if (player.blockX < cube.x - cube.size) {
            vec = Vec3d(player.x + 1, player.y, player.z)
        } else if (player.blockZ > cube.z + cube.size) {
            vec = Vec3d(player.z - 1, player.y, player.z - 1)
        } else if (player.blockZ < cube.z - cube.size) {
            vec = Vec3d(player.x, player.y, player.z + 1)
        }
        if (vec != null) {
            if (spawnLocation != null) vec = Vec3d(spawnLocation.x, spawnLocation.y, spawnLocation.z)
            player.teleport(vec.x, vec.y, vec.z)
        }
        return vec != null
    }

}