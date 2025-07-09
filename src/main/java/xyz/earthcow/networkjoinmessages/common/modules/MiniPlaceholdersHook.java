package xyz.earthcow.networkjoinmessages.common.modules;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MiniPlaceholdersHook {
    private final int startupExpansionCount;

    public MiniPlaceholdersHook() {
        startupExpansionCount = MiniPlaceholders.getExpansionCount();
    }

    public int getStartupExpansionCount() {
        return startupExpansionCount;
    }

    public TagResolver getGlobalResolver() {
        return MiniPlaceholders.getGlobalPlaceholders();
    }

    public TagResolver getAudienceResolver(Audience audience) {
        return MiniPlaceholders.getAudiencePlaceholders(audience);
    }
}
