package net.swzo.create_blueprinted.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.swzo.create_blueprinted.util.UIHelpers;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static net.swzo.create_blueprinted.CreateBlueprinted.translatable;

@EventBusSubscriber(Dist.CLIENT)
public enum ImageActionProgress {

    EXPORTED(1.0f, Type.FINAL, UIHelpers.LIGHT_GREEN_TEXT_COLOR),
    SHARED(1.0f, Type.FINAL, UIHelpers.LIGHT_GREEN_TEXT_COLOR),
    SHARE_FAILED(1.0f, Type.FAIL, UIHelpers.ERROR_PRIMARY),
    EXPORT_FAILED(1.0f, Type.FAIL, UIHelpers.ERROR_PRIMARY),
    RENDER_FAILED(1.0f, Type.FAIL, UIHelpers.ERROR_PRIMARY),
    EXPORTING(0.8f),
    SHARING(0.8f),
    RENDERING(0.4f),
    BAKING(0.0f),
    INACTIVE(0.0f);

    private static final int BAR_SEGMENTS = 10;
    private static final long LINGER_MILLIS = 1500L;

    private static final ProgressState INACTIVE_STATE = new ProgressState(INACTIVE, "");
    private static final AtomicReference<ProgressState> PROGRESS_STATE = new AtomicReference<>(INACTIVE_STATE);

    public final float fraction;
    public final int barColor;
    public final Type type;
    public final String messageKey;

    public enum Type { FINAL, FAIL, INTERMEDIATE }

    record ProgressState(ImageActionProgress progress, String fileName, long clearAt) {
        ProgressState(ImageActionProgress progress, String fileName) {
            this(progress, fileName, 0);
        }
    }

    ImageActionProgress(float fraction) {
        this(fraction, Type.INTERMEDIATE, UIHelpers.LIGHT_BLUE_TEXT_COLOR);
    }

    ImageActionProgress(float fraction, Type type, int barColor) {
        this.fraction = Math.clamp(fraction, 0f, 1f);
        this.barColor = barColor;
        this.type = type;
        this.messageKey = "message.action_progress." + name().toLowerCase(Locale.ENGLISH);
    }

    public static void start(@NotNull String fileName) {
        PROGRESS_STATE.set(new ProgressState(BAKING, fileName));
    }

    public static void setState(ImageActionProgress progress) {
        ProgressState progressState = PROGRESS_STATE.get();
        String fileName = progressState.fileName;;

        if (fileName == null)
            throw new NullPointerException("Failed to set intermediary progress state. Image file name cannot be null.");
        if (progress.type == Type.INTERMEDIATE)
            PROGRESS_STATE.set(new ProgressState(progress, fileName));
        else
            PROGRESS_STATE.set(new ProgressState(progress, fileName, now() + LINGER_MILLIS));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ProgressState state = PROGRESS_STATE.get();
        ImageActionProgress progress = state.progress;
        LocalPlayer player = Minecraft.getInstance().player;

        if (progress == INACTIVE) return;
        if (progress.type != Type.INTERMEDIATE && now() >= state.clearAt) {
            PROGRESS_STATE.compareAndSet(state, INACTIVE_STATE);
            return;
        }
        if (player != null)
            player.displayClientMessage(buildMessage(state), true);
    }

    private static Component buildMessage(ProgressState state) {
        ImageActionProgress progress = state.progress;
        float clamped = Math.clamp(progress.fraction, 0f, 1f);
        int filled = Math.round(clamped * BAR_SEGMENTS);
        var bar = new StringBuilder();

        for (int i = 0; i < BAR_SEGMENTS; i++)
            bar.append(i < filled ? '█' : '▒');

        boolean failed = progress.type == Type.FAIL;
        MutableComponent message = translatable(progress.messageKey, state.fileName)
                .withColor(failed ? UIHelpers.ERROR_DARKER : UIHelpers.DARK_BLUE_TEXT_COLOR)
                .append(Component.literal(" " + bar)
                        .withStyle(Style.EMPTY.withColor(progress.barColor)));

        if (progress.type != Type.FAIL)
            message.append(Component.literal(" " + Math.round(clamped * 100f) + "%")
                    .withColor(UIHelpers.LIGHT_BLUE_TEXT_COLOR));

        return message;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
