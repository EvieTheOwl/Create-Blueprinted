package net.swzo.create_blueprinted.api;

import net.swzo.create_blueprinted.impl.ShareProviderRegistryImpl;

import java.util.*;

import static net.swzo.create_blueprinted.impl.ShareProviderRegistryImpl.PROVIDERS;

public final class ShareProviderRegistry {

    private ShareProviderRegistry() {}

    /**
     * Register a new share provider.
     *
     * @param provider An object that inherits the {@link net.swzo.create_blueprinted.api.ShareProvider} interface
     */
    public static void register(ShareProvider provider) {
        ShareProviderRegistryImpl.register(provider);
    }

    /**
     * Get the share provider with the highest priority.
     *
     * @return The highest priority share provider or an empty optional if none are available
     */
    public static Optional<ShareProvider> getMainProvider() {
        return hasShareProvider() ? Optional.of(PROVIDERS.first()) : Optional.empty();
    }

    public static boolean hasShareProvider() {
        return !PROVIDERS.isEmpty();
    }
}
