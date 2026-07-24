package de.omegazirkel.risingworld.bosses;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.events.player.PlayerObjectInteractionEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Storage;
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.Vector3f;

/** Loads loot tables, creates loot sacks and delivers their contents. */
public final class BossLootHandler {
    private final Plugin plugin;
    private final BossDebugService debug;
    private final I18n i18n;
    private final Random random = new Random();
    private final Map<Long, Long> lootSacks = new LinkedHashMap<>();
    private final Map<Long, PendingLootSack> pendingLootSacks = new LinkedHashMap<>();
    private LootCatalog catalog = new LootCatalog();

    public BossLootHandler(Plugin plugin, BossDebugService debug, I18n i18n) {
        this.plugin = plugin;
        this.debug = debug;
        this.i18n = i18n;
    }

    public void load() {
        Path path = Path.of(plugin.getPath(), "loot.json");
        try {
            BossUtils.copyRuntimeConfig(plugin, path, "loot.default.json");
            catalog = new GsonBuilder().setPrettyPrinting().create()
                    .fromJson(Files.readString(path, StandardCharsets.UTF_8), LootCatalog.class);
            if (catalog == null || catalog.groups == null)
                catalog = new LootCatalog();
        } catch (Exception ex) {
            BossUtils.logger().error("Cannot load boss loot table: " + ex.getMessage());
            catalog = new LootCatalog();
        }
    }

    public void createLootSack(Vector3f position, String bossName, String groupType, int level,
            int participatingPlayers) {
        var definition = lootObjectDefinition();
        if (definition == null) {
            BossUtils.logger().error("No loot sack object definition found.");
            return;
        }
        var sack = World.createObject(definition.id, 0, definition.defaultcolor, position, new Quaternion(),
                new Vector3f(0.7f, 0.7f, 0.7f));
        if (sack == null)
            return;
        Storage storage = World.createStorage(Math.max(7, Math.max(1, level) * participatingPlayers + 3), false);
        sack.setInfo(storage.getID());
        debug.debugAdmins("TC_BOSSES_DEBUG_LOOT_SACK_CREATED", "PH_BOSS", bossName);
        int guaranteedDrops = Math.max(1, level) * participatingPlayers;
        lootSacks.put(sack.getGlobalID(), storage.getID());
        pendingLootSacks.put(sack.getGlobalID(), new PendingLootSack(bossName, roll(groupType, guaranteedDrops)));
    }

    public void openLootSack(PlayerObjectInteractionEvent event) {
        Long storageId = lootSacks.get(event.getGlobalID());
        if (storageId == null)
            return;
        event.setCancelled(true);
        PendingLootSack pending = pendingLootSacks.remove(event.getGlobalID());
        if (pending != null && !populateLootSack(storageId, pending.bossName(), pending.drops())) {
            grantLootFallback(event.getPlayer(), event.getObject().getWorldPosition(), pending.drops());
            lootSacks.remove(event.getGlobalID());
            event.getObject().destroy(true);
            return;
        }
        event.getPlayer().showStorage(storageId);
    }

    private List<LootDrop> roll(String groupType, int guaranteedDrops) {
        List<LootDrop> drops = new ArrayList<>();
        for (int guaranteed = 0; guaranteed < guaranteedDrops; guaranteed++)
            add(drops, groupType);
        for (int chance = 75; chance > 0 && random.nextInt(100) < chance; chance -= 25)
            add(drops, groupType);
        return drops;
    }

    private void add(List<LootDrop> drops, String groupType) {
        LootEntry entry = catalog.choose(groupType, random);
        if (entry == null)
            return;
        var definition = Definitions.getItemDefinition(entry.item);
        if (definition == null)
            return;
        int min = Math.max(1, entry.minStack);
        int max = Math.max(min, Math.min(definition.stacksize, entry.maxStack));
        int stack = min + random.nextInt(max - min + 1);
        debug.debugAdmins("TC_BOSSES_DEBUG_LOOT_ADDED", "PH_AMOUNT", Integer.toString(stack), "PH_ITEM",
                definition.name);
        drops.add(new LootDrop(definition.id, stack, definition.name));
    }

    private boolean populateLootSack(long storageId, String bossName, List<LootDrop> drops) {
        Storage storage = World.getStorage(storageId);
        if (storage == null) {
            BossUtils.logger().error("Loot sack storage was not available for " + storageId);
            return false;
        }
        storage.setName(BossUtils.replace(i18n.get("TC_BOSSES_LOOT_SACK_NAME", "de"), "PH_BOSS", bossName));
        storage.clear();
        for (LootDrop drop : drops) {
            if (storage.addItem(drop.itemId(), 0, drop.stack()) == null) {
                storage.clear();
                debug.debugAdmins("TC_BOSSES_DEBUG_LOOT_REJECTED", "PH_ITEM", drop.name());
                return false;
            }
        }
        return true;
    }

    private void grantLootFallback(Player player, Vector3f position, List<LootDrop> drops) {
        for (LootDrop drop : drops)
            if (player.getInventory().addItem(drop.itemId(), 0, drop.stack()) == null)
                World.spawnItem(drop.itemId(), 0, drop.stack(), position, new Quaternion(), false);
        player.sendTextMessage(BossUtils.message(i18n, "TC_BOSSES_LOOT_FALLBACK", player));
        debug.debug(player, "TC_BOSSES_DEBUG_LOOT_FALLBACK");
    }

    private net.risingworld.api.definitions.Objects.ObjectDefinition lootObjectDefinition() {
        for (String name : List.of("clothsack", "sack", "bag", "lootbag", "chest", "crate")) {
            var definition = Definitions.getObjectDefinition(name);
            if (definition != null)
                return definition;
        }
        return null;
    }

    public record LootDrop(short itemId, int stack, String name) {
    }

    private record PendingLootSack(String bossName, List<LootDrop> drops) {
    }

    private static final class LootEntry {
        String item;
        int weight;
        int minStack;
        int maxStack;
    }

    private static final class LootCatalog {
        Map<String, List<LootEntry>> groups = new LinkedHashMap<>();

        LootEntry choose(String groupType, Random random) {
            List<LootEntry> entries = groups.getOrDefault(groupType, groups.get("default"));
            if (entries == null || entries.isEmpty())
                return null;
            int total = entries.stream().filter(entry -> entry != null && entry.item != null && entry.weight > 0)
                    .mapToInt(entry -> entry.weight).sum();
            if (total <= 0)
                return null;
            int roll = random.nextInt(total);
            for (LootEntry entry : entries) {
                if (entry == null || entry.item == null || entry.weight <= 0)
                    continue;
                roll -= entry.weight;
                if (roll < 0)
                    return entry;
            }
            return null;
        }
    }
}
