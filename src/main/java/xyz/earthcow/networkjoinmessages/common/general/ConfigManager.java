package xyz.earthcow.networkjoinmessages.common.general;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import xyz.earthcow.networkjoinmessages.common.abstraction.CoreLogger;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private static YamlDocument config;
    private static YamlDocument discordConfig;

    public static void setupConfig(CoreLogger logger, File dataFolder) {
        logger.info("Attempting to load config files...");
        try {
            config = YamlDocument.create(new File(dataFolder, "config.yml"),
                    NetworkJoinMessagesCore.getInstance().getClass().getClassLoader().getResourceAsStream("config.yml"), GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());

            config.update();
            config.save();

            discordConfig = YamlDocument.create(new File(dataFolder, "discord.yml"),
                    NetworkJoinMessagesCore.getInstance().getClass().getClassLoader().getResourceAsStream("discord.yml"), GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());

            discordConfig.update();
            discordConfig.save();
        } catch (IOException e){
            logger.severe(
                    "Could not create/load plugin config, disabling! Additional info: "
                            + e);
            // TODO ADD CORE DISABLE PLUGIN IMPORTANT!!!!!
            return;
        }

        logger.info("Successfully loaded all plugin config files!");
    }

    public static YamlDocument getPluginConfig() {
        return config;
    }

    public static YamlDocument getDiscordConfig() {
        return discordConfig;
    }

    public static void savePluginConfig() {
        try {
            config.save();
        } catch (IOException e) {
            // TODO LOG THIS BRUHHH DevPluginBukkit.getInstance().getLogger().severe("Failed to save configuration file!");
        }
    }

    public static void saveDiscordConfig() {
        try {
            discordConfig.save();
        } catch (IOException e) {
            // TODO LOG THIS BRUHHH DevPluginBukkit.getInstance().getLogger().severe("Failed to save configuration file!");
        }
    }

}