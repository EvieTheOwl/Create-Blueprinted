package net.swzo.create_blueprinted.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ThreadUtils {

    public static <T> CompletableFuture<T> onRenderThread(Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        RenderSystem.recordRenderCall(() -> {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static <T> CompletableFuture<T> onClientThread(Supplier<T> work, Minecraft client) {
        CompletableFuture<T> future = new CompletableFuture<>();
        client.execute(() -> {
            try {
                future.complete(work.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
