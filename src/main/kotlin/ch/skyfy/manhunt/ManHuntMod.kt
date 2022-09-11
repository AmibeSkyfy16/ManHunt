package ch.skyfy.manhunt


import ch.skyfy.manhunt.config.Configs
import ch.skyfy.betterenderman.utils.setupConfigDirectory
import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.command.CreateStarterKitCmd
import ch.skyfy.manhunt.command.ReloadConfigCmd
import ch.skyfy.manhunt.command.ReloadPersistentCmd
import ch.skyfy.manhunt.command.StartCmd
import ch.skyfy.manhunt.logic.Game
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class ManHuntMod : ModInitializer {

    companion object {
        private const val MOD_ID: String = "manhunt"
        val CONFIG_DIRECTORY: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)
        val PERSISTENT_DIRECTORY: Path = CONFIG_DIRECTORY.resolve("persistent")
        val LOGGER: Logger = LogManager.getLogger(ManHuntMod::class.java)
    }

    private val optGameRef: AtomicReference<Optional<Game>> = AtomicReference(Optional.empty())

    private val startCmd: StartCmd
    private val createStarterKitCmd: CreateStarterKitCmd
    private val reloadConfigCmd: ReloadConfigCmd
    private val reloadPersistentCmd: ReloadPersistentCmd

    init {
        setupConfigDirectory()
        ConfigManager.loadConfigs(arrayOf(Configs.javaClass))

        startCmd = StartCmd(optGameRef)
        createStarterKitCmd = CreateStarterKitCmd(optGameRef)
        reloadConfigCmd = ReloadConfigCmd()
        reloadPersistentCmd = ReloadPersistentCmd()
    }

    override fun onInitialize() {
        registerCommands()
        ServerLifecycleEvents.SERVER_STARTED.register{server -> optGameRef.set(Optional.of(Game(server)))}
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            startCmd.register(dispatcher)
            createStarterKitCmd.register(dispatcher)
            reloadConfigCmd.register(dispatcher)
            reloadPersistentCmd.register(dispatcher)
        }
    }

}