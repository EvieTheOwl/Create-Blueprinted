package net.swzo.create_blueprinted.impl;

import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.CreateBlueprinted;
import net.swzo.create_blueprinted.api.ShareProvider;
import net.swzo.create_blueprinted.api.ShareProviderRegistry;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static net.swzo.create_blueprinted.api.ShareProvider.MAX_URL_CHAR_LENGTH;
import static net.swzo.create_blueprinted.CreateBlueprinted.LOGGER;

public class ShareProviderRegistryImpl {

    private static final Map<ResourceLocation, ShareProvider> PROVIDERS = new HashMap<>();
    private static final String HIGHEST_PRIORITY_WARN = "Selecting the highest priority share provider instead.";

    private static final String ERROR_START = "Failed to register a provider that implements schematic image sharing " +
            "functionality for Create Blueprinted.";

    public static void register(ShareProvider provider) {
        if (provider.id() == null)
            throw new NullPointerException(ERROR_START + " Provider ID cannot be null.");
        if (provider.id() == null || provider.destinationName() == null || provider.destinationUrl() == null)
            throw new NullPointerException(makeErrorMessage("Destination name or url are null.", provider));

        URL destinationUrl;
        String urlString = provider.destinationUrl();
        try {
            destinationUrl = URI.create(urlString).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new RuntimeException(makeErrorMessage("Destination URL could not be parsed.", provider));
        }

        if (!destinationUrl.getProtocol().equals("https"))
            throw new RuntimeException(makeErrorMessage("Destination URL must use the HTTPS protocol.", provider));
        if (urlString.length() > MAX_URL_CHAR_LENGTH)
            throw new RuntimeException(makeErrorMessage("Destination URL cannot be longer than " +
                    MAX_URL_CHAR_LENGTH + " characters.", provider));

        PROVIDERS.put(provider.id(), provider);
        LOGGER.info("Registered schematic image sharing provider. {}", createContext(provider));
    }

    public static Optional<ShareProvider> getHighestPriorityProvider() {
        int highestPriority = Integer.MIN_VALUE;
        ShareProvider highestPriorityProvider = null;

        for (var provider : PROVIDERS.values()) {
            if (provider.priority() > highestPriority) {
                highestPriority = provider.priority();
                highestPriorityProvider = provider;
            }
        }
        return Optional.ofNullable(highestPriorityProvider);
    }

    public static Optional<ShareProvider> getActiveShareProvider() {
        if (hasShareProvider()) {
            ResourceLocation providerId = ResourceLocation.tryParse(CreateBlueprinted.State.activeShareProvider.get());

            if (providerId != null) {
                String providerIdString = providerId.toString();

                if (!providerIdString.equals("minecraft:") && providerIdString.contains("minecraft:")) {
                    LOGGER.warn("Share provider can't be empty or use the default namespace. " + HIGHEST_PRIORITY_WARN);
                } else if (!PROVIDERS.containsKey(providerId)) {
                    LOGGER.warn("Active share provider: {} isn't currently registered. " + HIGHEST_PRIORITY_WARN,
                            providerIdString);
                } else
                    return Optional.ofNullable(PROVIDERS.get(providerId));

                return ShareProviderRegistry.getHighestPriorityProvider();
            }
        }
        return Optional.empty();
    }

    private static String makeErrorMessage(String message, ShareProvider provider) {
        return ERROR_START + " " + message + " " + createContext(provider);
    }

    private static String createContext(ShareProvider provider) {
        StringBuilder context = new StringBuilder("(");

        if (provider.id() != null)
            context.append("id: ").append(provider.id().toString());
        if (provider.priority() != 0)
            context.append(", priority: ").append(provider.priority());
        if (provider.destinationUrl() != null)
            context.append(", destination url: ").append(provider.destinationUrl());

        context.append(")");
        return context.toString();
    }

    public static Optional<ShareProvider> getShareProvider(ResourceLocation providerId) {
        return Optional.ofNullable(PROVIDERS.get(providerId));
    }

    public static Collection<ShareProvider> getAllProviders() {
        return PROVIDERS.values();
    }

    public static boolean hasShareProvider() {
        return !PROVIDERS.isEmpty();
    }
}
