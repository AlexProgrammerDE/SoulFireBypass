package com.soulfiremc.soulfirebypass.velocity;

import com.google.inject.Inject;
import com.soulfiremc.soulfirebypass.BuildConstants;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Plugin(id = "soulfirebypass", name = "SoulFireBypass", version = BuildConstants.VERSION, description = "Allow SoulFire to bypass online mode auth on your server.")
public class SoulFireBypassVelocity {

    @Inject
    private Logger logger;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private List<String> validKeys;

    @SneakyThrows
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading SoulFireBypass!");

        Files.createDirectories(dataDirectory);
        var configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"), "config.yml missing"), configPath);
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(dataDirectory.resolve("config.yml"))
                .build();

        try {
            var node = loader.load();
            validKeys = Objects.requireNonNull(node.node("allowed-keys").getList(String.class), "allowed-keys missing")
                    .stream()
                    .filter(s -> {
                        if (!s.equalsIgnoreCase("ConfigureMe")) {
                            logger.warn("Please configure the allowed-keys in the config.yml");
                            return false;
                        }
                        return true;
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to load config", e);
            return;
        }

        logger.info("SoulFireBypass loaded!");
    }

    @SneakyThrows
    @Subscribe
    public void onProxyInitialization(PreLoginEvent event) {
        // LoginInboundConnection
        var delegateField = event.getConnection().getClass().getDeclaredField("delegate");
        var delegate = delegateField.get(event.getConnection());
        // InitialInboundConnection
        var handshakeField = delegate.getClass().getDeclaredField("handshake");
        var handshake = handshakeField.get(delegate);
        // HandshakePacket
        var serverAddressField = handshake.getClass().getDeclaredField("serverAddress");
        var serverAddress = (String) serverAddressField.get(handshake);

        if (isValidKey(serverAddress)) {
            logger.info("Forcing offline mode for {}", event.getUsername());
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
        }
    }

    private boolean isValidKey(String address) {
        return validKeys.stream().anyMatch(k -> address.contains("SF_" + k));
    }
}
