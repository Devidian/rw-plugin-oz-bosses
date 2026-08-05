package de.omegazirkel.risingworld.bosses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector2i;
import net.risingworld.api.utils.Vector3f;

/** Stateless utilities shared by the feature-owned boss services. */
public final class BossUtils {
    public static final int CHUNKS_PER_SECTOR_AXIS = 256;
    public static final float SECTOR_SIZE = 8192f;

    private BossUtils() {
    }

    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.Bosses");
    }

    public static void copyRuntimeConfig(Plugin plugin, Path target, String defaultResource) throws IOException {
        if (Files.exists(target))
            return;
        try (InputStream input = plugin.getClass().getClassLoader().getResourceAsStream(defaultResource)) {
            if (input == null)
                throw new IOException("Missing packaged default " + defaultResource);
            Files.copy(input, target);
        }
    }

    public static String message(I18n i18n, String key, Player player, String... replacements) {
        return replace(i18n.get(key, player), replacements);
    }

    public static String replace(String message, String... replacements) {
        String result = message == null ? "" : message;
        if (replacements == null)
            return result;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];
            if (placeholder == null || placeholder.isEmpty())
                continue;
            String replacement = replacements[i + 1] == null ? "-" : replacements[i + 1];
            result = result.replace(placeholder, replacement);
        }
        return result;
    }

    public static String enemyKey(Npc npc, BossGroupCatalog groups) {
        if (npc == null || npc.getDefinition() == null)
            return "default";
        String name = npc.getDefinition().name == null ? "" : npc.getDefinition().name.toLowerCase(Locale.ROOT);
        String configured = groups.nameType(npc);
        if (configured != null)
            return configured;
        if (name.contains("lion") || name.contains("löw"))
            return "lion";
        if (name.contains("ghoul") || name.contains("ghul"))
            return "ghoul";
        if (name.contains("skeleton") || name.contains("skelet"))
            return "skeleton";
        if (name.contains("firewolf"))
            return "firewolf";
        if (name.contains("wolf"))
            return "wolf";
        if (name.contains("boar") || name.contains("keiler") || name.contains("schwein"))
            return "wildboar";
        return npc.getDefinition().type == net.risingworld.api.definitions.Npcs.Type.Animal ? "animal" : "dummy";
    }

    public static String genderKey(Npc npc) {
        if (npc != null && npc.getSkin() != null)
            return npc.getSkin().getGender() == net.risingworld.api.objects.Skin.Gender.Female ? "female" : "male";
        if (npc == null || npc.getDefinition() == null)
            return "any";
        return switch (npc.getDefinition().gender) {
            case Female -> "female";
            case Male -> "male";
            default -> "any";
        };
    }

    public static Vector2i sectorPosition(Vector3f position) {
        return new Vector2i((int) Math.floor(position.x / SECTOR_SIZE),
                (int) Math.floor(position.z / SECTOR_SIZE));
    }
}
