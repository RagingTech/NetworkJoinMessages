package xyz.earthcow.networkjoinmessages.common.modules;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

public class MiniPlaceholdersHook {

    public TagResolver getGlobalResolver() {
        return MiniPlaceholders.getGlobalPlaceholders();
    }

    public TagResolver getAudienceGlobalResolver(@NotNull Audience audience) {
        return MiniPlaceholders.getAudienceGlobalPlaceholders(audience);
    }
}
