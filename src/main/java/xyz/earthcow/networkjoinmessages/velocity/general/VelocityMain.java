package xyz.earthcow.networkjoinmessages.velocity.general;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import xyz.earthcow.networkjoinmessages.common.abstraction.*;
import xyz.earthcow.networkjoinmessages.common.general.NetworkJoinMessagesCore;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityLogger;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityPlayer;
import xyz.earthcow.networkjoinmessages.velocity.abstraction.VelocityServer;
import xyz.earthcow.networkjoinmessages.velocity.commands.FakeCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ReloadCommand;
import xyz.earthcow.networkjoinmessages.velocity.commands.ToggleJoinCommand;
import xyz.earthcow.networkjoinmessages.velocity.listeners.PlayerListener;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(
    id = "networkjoinmessages",
    name = "NetworkJoinMessages",
    version = "2.1.0-SNAPSHOT",
    url = "https://github.com/RagingTech/NetworkJoinMessages",
    description = "A plugin handling join, leave and switch messages for proxy servers.",
    authors = { "EarthCow" }
)
public class VelocityMain implements CorePlugin {

    private static VelocityMain instance;
    public static VelocityMain getInstance() {
        return instance;
    }

    private final ProxyServer proxy;
    public ProxyServer getProxy() {
        return proxy;
    }

    private final VelocityLogger velocityLogger;
    @Override
    public CoreLogger getCoreLogger() {
        return velocityLogger;
    }

    @Override
    public List<CorePlayer> getAllPlayers() {
        return proxy.getAllPlayers().stream().map(VelocityPlayer::new).collect(Collectors.toList());
    }

    @Override
    public CoreBackendServer getServer(String serverName) {
        RegisteredServer registeredServer = proxy.getServer(serverName).orElse(null);
        if (registeredServer == null) return null;
        return new VelocityServer(registeredServer);
    }

    @Override
    public void fireEvent(Object event) {
        proxy.getEventManager().fireAndForget(event);
    }

    @Override
    public boolean getVanishAPI() {
        return false;
    }

    @Override
    public void runTaskLater(Runnable task, int timeInSecondsLater) {
        proxy.getScheduler().buildTask(this, task).delay(timeInSecondsLater, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void runTaskAsync(Runnable task) {
        proxy.getScheduler().buildTask(this, task).schedule();
    }

    private final File dataFolder;
    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    private NetworkJoinMessagesCore core;
    @Override
    public NetworkJoinMessagesCore getCore() {
        return core;
    }

    @Override
    public ServerType getServerType() {
        return ServerType.VELOCITY;
    }

    @Inject
    public VelocityMain(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.velocityLogger = new VelocityLogger(logger);
        this.dataFolder = dataDirectory.toFile();

        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.core = new NetworkJoinMessagesCore(this);

        proxy.getEventManager().register(this, new PlayerListener());

        CommandManager commandManager = proxy.getCommandManager();
        commandManager.register(
            commandManager
                .metaBuilder("fakemessage")
                .aliases("fm")
                .plugin(this)
                .build(),
            new FakeCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("networkjoinreload")
                .aliases("njoinreload")
                .plugin(this)
                .build(),
            new ReloadCommand()
        );
        commandManager.register(
            commandManager
                .metaBuilder("togglejoinmessage")
                .aliases("njointoggle")
                .plugin(this)
                .build(),
            new ToggleJoinCommand()
        );

        // TODO Add vanish support
    }
}
