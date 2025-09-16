package xyz.earthcow.networkjoinmessages.common.modules;

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MiniPlaceholdersHook {

    public TagResolver getGlobalResolver() {
        return MiniPlaceholders.globalPlaceholders();
    }

    public TagResolver getAudienceGlobalResolver() {
        return MiniPlaceholders.audienceGlobalPlaceholders();
    }
}
