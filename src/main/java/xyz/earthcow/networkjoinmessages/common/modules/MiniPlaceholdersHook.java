package xyz.earthcow.networkjoinmessages.common.modules;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MiniPlaceholdersHook {

    public TagResolver getGlobalResolver() {
        return MiniPlaceholders.getGlobalPlaceholders();
    }

    public TagResolver getAudienceResolver(Audience audience) {
        return MiniPlaceholders.getAudiencePlaceholders(audience);
    }
}
