package net.swzo.create_blueprinted.impl;

import net.swzo.create_blueprinted.api.ShareProvider;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import static net.swzo.create_blueprinted.api.ShareProvider.MAX_URL_CHAR_LENGTH;
import static net.swzo.create_blueprinted.CreateBlueprinted.LOGGER;

public class ShareProviderRegistryImpl {

    public static final SortedSet<ShareProvider> PROVIDERS = new TreeSet<>(Comparator.comparingInt(ShareProvider::priority));

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

        PROVIDERS.add(provider);
        LOGGER.info("Registered schematic image sharing provider. {}", createContext(provider));
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
}
