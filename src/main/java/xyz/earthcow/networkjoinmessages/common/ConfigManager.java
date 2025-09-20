package xyz.earthcow.networkjoinmessages.common;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ConfigManager {

    private final CoreLogger logger;

    private YamlDocument pluginConfig;
    private YamlDocument discordConfig;

    private YamlDocument createConfig(CorePlugin plugin, String filename) throws IOException {
        YamlDocument doc = YamlDocument.create(
            new File(plugin.getDataFolder(), filename),
            Objects.requireNonNull(plugin.getClass().getResourceAsStream("/" + filename)),
            GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT,
            UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build()
        );

        doc.update();
        doc.save();

        return doc;
    }

    public ConfigManager(CorePlugin plugin) {
        this.logger = plugin.getCoreLogger();
        logger.info("Attempting to load config files...");
        try {
            pluginConfig = createConfig(plugin, "config.yml");
            discordConfig = createConfig(plugin, "discord.yml");
        } catch (IOException e){
            logger.severe("Could not create/load plugin config, disabling! Additional info: " + e);
            plugin.disable();
            return;
        }

        logger.info("Successfully loaded all plugin config files!");
    }

    public YamlDocument getPluginConfig() {
        return pluginConfig;
    }

    public YamlDocument getDiscordConfig() {
        return discordConfig;
    }

    public void reload() {
        try {
            pluginConfig.reload();
            discordConfig.reload();
        } catch (IOException e) {
            logger.severe("Could not reload config files! Additional info: " + e);
        }
    }

}