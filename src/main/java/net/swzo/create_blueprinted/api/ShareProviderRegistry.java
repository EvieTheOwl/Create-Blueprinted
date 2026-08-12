package net.swzo.create_blueprinted.api;

import net.minecraft.resources.ResourceLocation;
import net.swzo.create_blueprinted.impl.ShareProviderRegistryImpl;

import java.util.*;

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
    public static Optional<ShareProvider> getHighestPriorityProvider() {
        return ShareProviderRegistryImpl.getHighestPriorityProvider();
    }

    /**
     * Get the currently active share provider. The active share provider is either set in the schematic table
     * by scrolling through tooltips or by executing the command <code>/schematic share provider [id]</code>.
     * If an invalid or empty provider is set it will simply return the one with the highest priority.
     *
     * @return The active share provider if one exists or an empty optional if none are available
     */
    public static Optional<ShareProvider> getActiveShareProvider() {
        return ShareProviderRegistryImpl.getActiveShareProvider();
    }

    public static Optional<ShareProvider> getShareProvider(ResourceLocation providerId) {
        return ShareProviderRegistryImpl.getShareProvider(providerId);
    }

    public static Collection<ShareProvider> getAllProviders() {
        return ShareProviderRegistryImpl.getAllProviders();
    }

    public static boolean hasShareProvider() {
        return ShareProviderRegistryImpl.hasShareProvider();
    }

    /**
     * Alias for {@link ShareProviderRegistry#getHighestPriorityProvider()}. Renamed to make it more descriptive.
     */
    @Deprecated(since = "2.1", forRemoval = true)
    public static Optional<ShareProvider> getMainProvider() {
        return getHighestPriorityProvider();
    }
}
