package ch.skyfy.manhunt

import ch.skyfy.manhunt.config.Configs
import ch.skyfy.betterenderman.utils.setupConfigDirectory
import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.command.*
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
        const val THE_HUNTERS = "the-hunters"
        const val THE_HUNTED_ONES = "the-hunted-ones"
        val CONFIG_DIRECTORY: Path = FabricLoader.getInstance().configDir.resolve(MOD_ID)
        val DATA_DIRECTORY: Path = CONFIG_DIRECTORY.resolve(arrayOf("d", "a", "t", "a").joinToString(""))
        val LOGGER: Logger = LogManager.getLogger(ManHuntMod::class.java)
    }

    private val optGameRef: AtomicReference<Optional<Game>> = AtomicReference(Optional.empty())

    private val startCmd: StartCmd
    private val createKitCmd: CreateKitCmd
    private val createBonusCmd: CreateBonusCmd
    private val getKitCmd: GetKitCmd
    private val reloadConfigCmd: ReloadConfigCmd
    private val reloadPersistentCmd: ReloadPersistentCmd
    private val debugModeCmd: DebugModeCmd

    init {
        setupConfigDirectory()
        ConfigManager.loadConfigs(arrayOf(Configs.javaClass))

        startCmd = StartCmd(optGameRef)
        createKitCmd = CreateKitCmd(optGameRef)
        createBonusCmd = CreateBonusCmd(optGameRef)
        getKitCmd = GetKitCmd(optGameRef)
        reloadConfigCmd = ReloadConfigCmd(optGameRef)
        reloadPersistentCmd = ReloadPersistentCmd(optGameRef)
        debugModeCmd = DebugModeCmd(optGameRef)
    }

    override fun onInitialize() {
        registerCommands()
        ServerLifecycleEvents.SERVER_STARTED.register{server -> optGameRef.set(Optional.of(Game(server)))}
    }

    private fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            startCmd.register(dispatcher)
            createKitCmd.register(dispatcher)
            createBonusCmd.register(dispatcher)
            getKitCmd.register(dispatcher)
            reloadConfigCmd.register(dispatcher)
            reloadPersistentCmd.register(dispatcher)
            debugModeCmd.register(dispatcher)
        }
    }

}