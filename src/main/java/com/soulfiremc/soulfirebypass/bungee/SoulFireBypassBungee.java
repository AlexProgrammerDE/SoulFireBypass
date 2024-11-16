package com.soulfiremc.soulfirebypass.bungee;

import lombok.SneakyThrows;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public class SoulFireBypassBungee extends Plugin implements Listener {
    private List<String> validKeys;

    @SneakyThrows
    @Override
    public void onEnable() {
        var logger = getLogger();
        logger.info("Loading SoulFireBypass!");

        var dataFolder = getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        var configPath = dataFolder.resolve("config.yml");
        if (!Files.exists(configPath)) {
            Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"), "config.yml missing"), configPath);
        }

        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(configPath.toFile());

        validKeys = config.getStringList("allowed-keys")
                .stream()
                .filter(s -> {
                    if (!s.equalsIgnoreCase("ConfigureMe")) {
                        logger.warning("Please configure the allowed-keys in the config.yml");
                        return false;
                    }
                    return true;
                })
                .toList();

        getProxy().getPluginManager().registerListener(this, this);

        logger.info("SoulFireBypass loaded!");
    }

    @SneakyThrows
    @EventHandler
    public void onHandshake(PlayerHandshakeEvent event) {
        Field field = event.getConnection().getClass().getDeclaredField("extraDataInHandshake");
        field.setAccessible(true);
        String extraData = (String) field.get(event.getConnection());

        if (isValidKey(extraData)) {
            var logger = getLogger();
            logger.info("Forcing offline mode for " + event.getConnection().getName());
            event.getConnection().setOnlineMode(false);
        }
    }

    private boolean isValidKey(String address) {
        return validKeys.stream().anyMatch(k -> address.contains("SF_" + k));
    }
}
