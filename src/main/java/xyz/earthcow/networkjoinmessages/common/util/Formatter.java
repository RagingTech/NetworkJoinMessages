package xyz.earthcow.networkjoinmessages.common.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.earthcow.networkjoinmessages.common.abstraction.CorePlayer;
import xyz.earthcow.networkjoinmessages.common.modules.MiniPlaceholdersHook;

/**
 * Stateless facade for Adventure Component serialization/deserialization.
 *
 * <p>Handles legacy-to-MiniMessage translation and MiniPlaceholders context. All live
 * placeholder resolution (LuckPerms, PAPI, built-ins) is delegated to {@link PlaceholderResolver}.
 */
public final class Formatter {

    public static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private Formatter() {}

    /**
     * Deserializes a string to a Component, applying legacy color translation.
     * Does not resolve MiniPlaceholders.
     */
    public static Component deserialize(@NotNull String str) {
        return deserialize(str, null);
    }

    /**
     * Deserializes a string to a Component, applying legacy color translation and optionally
     * MiniPlaceholders for the given player audience.
     *
     * @param str         the raw string to deserialize
     * @param parseTarget optional player to resolve audience-scoped MiniPlaceholders against
     */
    public static Component deserialize(@NotNull String str, @Nullable CorePlayer parseTarget) {
        String translated = LegacyColorTranslator.translate(str);
        MiniPlaceholdersHook miniPlaceholders = PlaceholderResolver.getMiniPlaceholders();

        if (miniPlaceholders != null) {
            if (parseTarget == null) {
                return miniMessage.deserialize(translated, miniPlaceholders.getGlobalResolver());
            } else {
                return miniMessage.deserialize(translated, parseTarget.getAudience(), miniPlaceholders.getAudienceGlobalResolver());
            }
        }

        return miniMessage.deserialize(translated);
    }

    /**
     * Serializes a Component back to a MiniMessage string.
     */
    public static String serialize(@NotNull Component component) {
        return miniMessage.serialize(component);
    }

    /**
     * Strips all formatting from a string, returning plain text.
     */
    public static String sanitize(@NotNull String str) {
        return sanitize(deserialize(str));
    }

    /**
     * Strips all formatting from a Component, returning plain text.
     */
    public static String sanitize(@NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
