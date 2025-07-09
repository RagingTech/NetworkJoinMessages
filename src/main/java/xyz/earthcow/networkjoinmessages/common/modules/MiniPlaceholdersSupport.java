package xyz.earthcow.networkjoinmessages.common.modules;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MiniPlaceholdersSupport {

    private final TagResolver miniPlaceholdersResolver;

    public MiniPlaceholdersSupport() {
        miniPlaceholdersResolver = MiniPlaceholders.getGlobalPlaceholders();
    }

    public TagResolver getGlobalResolver() {
        return miniPlaceholdersResolver;
    }

    public TagResolver getAudienceResolver(Audience audience) {
        return MiniPlaceholders.getAudiencePlaceholders(audience);
    }
}
