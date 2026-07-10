package net.swzo.create_blueprinted.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

/**
 * <p>A provider used whenever the share button or command is fired by the player.</p>
 * <p>If you want to provide an implementation that sends an image to a remote server you must
 *     implement some form of serverside image sanitization.</p>
 *
 * Register the provider within {@link net.swzo.create_blueprinted.api.ShareProviderRegistry}
 */
public interface ShareProvider {

    int MAX_URL_CHAR_LENGTH = 50;

    /**
     * Unique ID for the provider.
     *
     * @return Provider ID resource location
     */
    ResourceLocation id();

    /**
     * Describes where the file will be sent. For example: "Brassworks SMP Discord Server".
     *
     * @return A text component representing the schematic image files destination
     */
    Component destinationName();

    /**
     * URL representing where the file will be sent.
     *
     * @return Destination URL. Must use the HTTPS protocol and be shorter than {@link #MAX_URL_CHAR_LENGTH}
     *         characters long.
     */
    String destinationUrl();

    /**
     * Determine what happens to the schematic image after it is rendered.
     * Exceptions should be thrown instead of handled. Wrap checked exceptions in a {@code CompletionException}.
     * This method runs on a separate async thread independent of the main client or render threads.
     *
     * @param fileName Name of the image file without its extension
     * @param imageByteArray The rendered PNG schematic image in a byte arrØay format
     * @return A URL representing the location the image has been shared or null if the operation failed
     */
    @Nullable URL handle(String fileName, byte[] imageByteArray);

    /**
     * Gets the priority of the current provider. Used to compare against other mods that implement a provider.
     * Only the highest priority provider is used by Create Configured.
     *
     * @return Priority of the current provider (Default value = 1)
     */
    default int priority() { return 1; }
}
