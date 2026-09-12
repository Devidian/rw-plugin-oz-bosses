package de.omegazirkel.risingworld.bosses;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.bridge.MailBridge;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Clothing.ClothingDefinition;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Skin;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.Vector3f;

/** Creates and serves fixed NPC endpoints without taking ownership of Wallet or Mail state. */
public final class BossInformantService {
    private static final String PLUGIN_NAME = "OZ - Bosses";
    private static final List<String> MALE_NAMES = List.of("Aldric", "Bastian", "Cedric", "Darian", "Eamon", "Falk", "Garrick", "Hadrian", "Ivar", "Jorek", "Kael", "Leif", "Marek", "Nolan", "Orin", "Perrin", "Quentin", "Roderic", "Soren", "Tavian", "Ulric", "Vaughn", "Wulfric", "Xander", "Yorik", "Zarek", "Ansel", "Bram", "Corvin", "Dieter", "Erik", "Finnian", "Gideon", "Haldor", "Isen", "Jannik", "Kellan", "Lorcan", "Milo", "Nestor", "Oswin", "Rurik", "Stellan", "Torben", "Valen", "Waldo", "Yngvar", "Zoran", "Arvid", "Berengar");
    private static final List<String> FEMALE_NAMES = List.of("Aurelia", "Brunhild", "Celia", "Dagmar", "Elowen", "Fenna", "Gisla", "Hedwig", "Isolde", "Juna", "Kestrel", "Liora", "Mirela", "Nerys", "Odette", "Petra", "Quilla", "Runa", "Sigrid", "Tamsin", "Una", "Vesper", "Wynne", "Xanthe", "Yrsa", "Zelda", "Anouk", "Brynja", "Coralie", "Dagna", "Eira", "Freydis", "Gudrun", "Halla", "Ilva", "Jorunn", "Kaia", "Liv", "Mavis", "Nadia", "Orla", "Rhiannon", "Solveig", "Thyra", "Vala", "Wenche", "Ylva", "Ziva", "Adelina", "Brisa");
    private static final List<String> OUTFIT = List.of("medievalshirt", "medievalpants", "medievalshoes");
    private final Plugin plugin;
    private final BossState state;
    private final Supplier<PluginSettings> settings;
    private final BossInformantRepository repository;
    private final I18n i18n;
    private final Random random = new Random();

    public BossInformantService(Plugin plugin, BossState state, Supplier<PluginSettings> settings,
            BossInformantRepository repository, I18n i18n) {
        this.plugin = plugin; this.state = state; this.settings = settings; this.repository = repository; this.i18n = i18n;
    }

    public void rehydrate() {
        try {
            for (BossInformant informant : repository.all()) {
                Npc npc = World.getNpc(informant.npcId());
                if (npc != null && !npc.isDead()) { initialize(npc, informant); continue; }
                var dummy = Definitions.getNpcDefinition("dummy");
                if (dummy == null) { BossUtils.logger().warn("Cannot rehydrate Headhunter Informant: dummy NPC is unavailable."); continue; }
                npc = World.spawnNpc(dummy.id, informant.male() ? 0 : 1, new Vector3f(informant.x(), informant.y(), informant.z()), new Quaternion(informant.rx(), informant.ry(), informant.rz(), informant.rw()));
                if (npc == null) { BossUtils.logger().warn("Cannot rehydrate Headhunter Informant " + informant.name()); continue; }
                BossInformant replacement = fromNpc(npc, informant.name(), informant.male(), informant.accountId());
                repository.replaceId(informant.npcId(), replacement);
                initialize(npc, replacement);
            }
        } catch (SQLException ex) { BossUtils.logger().error("Cannot restore Headhunter Informants: " + ex.getMessage()); }
    }

    public void create(Player player, boolean male) {
        if (player == null || !player.isAdmin()) { if (player != null) player.sendTextMessage(text(player, "tc.bosses.admin.required")); return; }
        var dummy = Definitions.getNpcDefinition("dummy");
        if (dummy == null) { player.sendTextMessage(text(player, "tc.bosses.informant.create.failed")); return; }
        Npc npc = World.spawnNpc(dummy.id, male ? 0 : 1, player.getPosition(), player.getRotation());
        if (npc == null) { player.sendTextMessage(text(player, "tc.bosses.informant.create.failed")); return; }
        String name = text(player, male ? "tc.bosses.informant.name.male" : "tc.bosses.informant.name.female") + " " + (male ? MALE_NAMES : FEMALE_NAMES).get(random.nextInt(50));
        try {
            BossInformant informant = fromNpc(npc, name, male, newAccountId());
            repository.save(informant); initialize(npc, informant);
            player.sendTextMessage(text(player, "tc.bosses.informant.create.success", "PH_NAME", name));
        } catch (SQLException ex) { npc.delete(); BossUtils.logger().error("Cannot save Headhunter Informant: " + ex.getMessage()); player.sendTextMessage(text(player, "tc.bosses.informant.create.failed")); }
    }

    public List<AdminRow> adminRows() {
        try {
            WalletBridge wallet = new WalletBridge(plugin);
            return repository.all().stream().map(informant -> {
                var sector = BossUtils.sectorPosition(new Vector3f(informant.x(), informant.y(), informant.z()));
                long balance = wallet.isAvailable() && !informant.accountId().isBlank()
                        ? wallet.systemAccountBalances(informant.accountId()).stream()
                                .filter(value -> value.currencyIdentifier().equals(wallet.defaultCurrencyIdentifier()))
                                .mapToLong(WalletBridge.SystemBalanceInfo::balance).findFirst().orElse(0L)
                        : 0L;
                return new AdminRow(informant.npcId(), informant.name(), sector.x + "," + sector.y, balance);
            }).sorted(java.util.Comparator.comparing(AdminRow::name, String.CASE_INSENSITIVE_ORDER)).toList();
        } catch (SQLException ex) { BossUtils.logger().error("Cannot list Headhunter Informants: " + ex.getMessage()); return List.of(); }
    }

    public boolean rename(long npcId, String name) {
        String safeName = name == null ? "" : name.trim();
        if (safeName.isBlank() || safeName.length() > 120) return false;
        try {
            BossInformant current = repository.find(npcId).orElse(null);
            if (current == null) return false;
            BossInformant renamed = new BossInformant(current.npcId(), safeName, current.male(), current.x(), current.y(), current.z(), current.rx(), current.ry(), current.rz(), current.rw(), current.accountId());
            repository.save(renamed);
            Npc npc = World.getNpc(npcId);
            if (npc != null && !npc.isDead()) npc.setName(safeName);
            WalletBridge wallet = new WalletBridge(plugin);
            if (wallet.isAvailable() && !renamed.accountId().isBlank()) wallet.updateSystemAccountDisplayName(renamed.accountId(), safeName, PLUGIN_NAME);
            return true;
        } catch (SQLException ex) { BossUtils.logger().error("Cannot rename Headhunter Informant: " + ex.getMessage()); return false; }
    }

    public boolean dissolve(long npcId) {
        try {
            BossInformant informant = repository.find(npcId).orElse(null);
            if (informant == null) return false;
            if (!settleAndArchiveAccount(informant)) return false;
            repository.delete(npcId);
            Npc npc = World.getNpc(npcId);
            if (npc != null && !npc.isDead()) npc.delete();
            return true;
        } catch (SQLException ex) { BossUtils.logger().error("Cannot dissolve Headhunter Informant: " + ex.getMessage()); return false; }
    }

    private boolean settleAndArchiveAccount(BossInformant informant) {
        if (informant.accountId().isBlank()) return true;
        WalletBridge wallet = new WalletBridge(plugin);
        if (!wallet.isAvailable() || !wallet.hasSystemAccountApi()) return false;
        WalletBridge.SystemAccountCallResult account = wallet.systemAccount(informant.accountId());
        if (!account.success()) return "ACCOUNT_NOT_FOUND".equals(account.errorCode());
        String worldAccountId = wallet.worldSystemAccountId();
        if (worldAccountId.isBlank()) return false;
        for (WalletBridge.SystemBalanceInfo balance : wallet.systemAccountBalances(informant.accountId())) {
            if (balance.balance() == 0L) continue;
            if (balance.balance() < 0L) return false;
            String correlation = "informant-dissolve-" + informant.npcId() + "-" + balance.currencyIdentifier();
            if (!wallet.transferSystemToSystemIdempotent(informant.accountId(), worldAccountId, balance.balance(),
                    "Dissolved Headhunter Informant", balance.currencyIdentifier(), PLUGIN_NAME, correlation).success()) return false;
        }
        return wallet.archiveSystemAccount(informant.accountId(), PLUGIN_NAME).success();
    }

    public void interact(PlayerNpcInteractionEvent event) {
        if (event.getNpc() == null) return;
        try {
            BossInformant informant = repository.find(event.getNpc().getGlobalID()).orElse(null);
            if (informant == null) return;
            event.setCancelled(true);
            Player player = event.getPlayer();
            WalletBridge wallet = new WalletBridge(plugin);
            if (!wallet.isAvailable()) { player.sendTextMessage(text(player, "tc.bosses.informant.wallet.unavailable")); return; }
            Quote quote = randomQuote(player, ensureAccount(informant));
            if (quote == null) { player.sendTextMessage(text(player, "tc.bosses.informant.wallet.unavailable")); return; }
            MailBridge mail = new MailBridge(plugin);
            if (!mail.canReceiveMail(player.getDbID())) { player.sendTextMessage(text(player, "tc.bosses.informant.mailbox.unavailable")); return; }
            showOffer(player, quote);
        } catch (SQLException ex) { BossUtils.logger().error("Cannot resolve Headhunter Informant interaction: " + ex.getMessage()); }
    }

    private Quote randomQuote(Player player, BossInformant informant) {
        if (player == null || informant == null) return null;
        String sector = state.sector(player).key;
        List<Quote> choices = new ArrayList<>();
        for (BossGroup group : state.activeGroups().values()) {
            if (group.finished || !sector.equals(group.sector.key)) continue;
            List<Npc> members = group.members.stream().map(World::getNpc).filter(npc -> npc != null && !npc.isDead()).toList();
            if (!members.isEmpty()) choices.add(new Quote(group, members, price(members.size()), informant));
        }
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    private long price(int members) { try { return Math.multiplyExact(Math.max(0L, settings.get().informantBasePrice), Math.max(0, members)); } catch (ArithmeticException ex) { return Long.MAX_VALUE; } }

    private void showOffer(Player player, Quote quote) {
        UIElement dialog = new UIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(480, 245, false); dialog.setBackgroundColor(0, 0, 0, .94f); dialog.setBorder(1); dialog.setBorderColor(.85f, .65f, .2f, .8f);
        UILabel title = new UILabel(text(player, "tc.bosses.informant.offer.title")); title.setFont(Font.DefaultBold); title.setFontSize(22); title.setTextAlign(TextAnchor.MiddleCenter); title.setPivot(Pivot.UpperCenter); title.setPosition(50, 14, true); title.setSize(440, 32, false); dialog.addChild(title);
        UILabel body = new UILabel(text(player, "tc.bosses.informant.offer.body", "PH_BOSS", quote.group.name, "PH_LEVEL", Integer.toString(quote.group.level), "PH_COUNT", Integer.toString(quote.members.size()), "PH_PRICE", Long.toString(quote.price))); body.setTextWrap(true); body.setFontSize(16); body.setPivot(Pivot.UpperLeft); body.setPosition(24, 66, false); body.setSize(432, 105, false); dialog.addChild(body);
        AdvancedButton decline = AdvancedButtonFactory.cancel(text(player, "tc.bosses.informant.decline"), ignored -> { player.removeUIElement(dialog); player.closeAllActiveUIWindows(); }); decline.setPivot(Pivot.LowerLeft); decline.setPosition(24, 220, false); decline.setSize(160, 34, false); dialog.addChild(decline);
        AdvancedButton accept = AdvancedButtonFactory.ok(text(player, "tc.bosses.informant.accept"), ignored -> { player.removeUIElement(dialog); player.closeAllActiveUIWindows(); purchase(player, quote); }); accept.setPivot(Pivot.LowerRight); accept.setPosition(456, 220, false); accept.setSize(160, 34, false); dialog.addChild(accept);
        player.addUIElement(dialog, UITarget.Modal);
    }

    private void purchase(Player player, Quote quote) {
        Quote current = quoteStillActive(player, quote.group.id, quote.informant);
        if (current == null) { player.sendTextMessage(text(player, "tc.bosses.informant.expired")); return; }
        WalletBridge wallet = new WalletBridge(plugin);
        String correlation = "informant-" + UUID.randomUUID();
        BossInformant informant = ensureAccount(current.informant);
        if (informant == null || !wallet.transferPlayerToSystemIdempotent(player.getDbID(), informant.accountId(), current.price, "Headhunter intelligence", wallet.defaultCurrencyIdentifier(), PLUGIN_NAME, correlation).success()) { player.sendTextMessage(text(player, "tc.bosses.informant.insufficient")); return; }
        MailBridge.BridgeResult delivery = new MailBridge(plugin).sendTextMail(new MailBridge.PluginMailRequest(PLUGIN_NAME, player.getDbID(), player.getName(), text(player, "tc.bosses.informant.mail.subject", "PH_BOSS", current.group.name), mailBody(player, current), correlation));
        if (delivery.success()) { player.sendTextMessage(text(player, "tc.bosses.informant.delivered")); return; }
        boolean refunded = wallet.reverseAccountTransferIdempotent(correlation, correlation + "-refund", "Headhunter intelligence refund", PLUGIN_NAME).success();
        BossUtils.logger().warn("Headhunter Informant mail delivery failed (" + delivery.code() + "); refund=" + refunded + ".");
        player.sendTextMessage(text(player, refunded ? "tc.bosses.informant.refunded" : "tc.bosses.informant.refund.pending"));
    }

    private Quote quoteStillActive(Player player, int groupId, BossInformant informant) { for (Quote quote : quotesInSector(player, informant)) if (quote.group.id == groupId) return quote; return null; }
    private List<Quote> quotesInSector(Player player, BossInformant informant) { String sector = state.sector(player).key; List<Quote> values = new ArrayList<>(); for (BossGroup group : state.activeGroups().values()) { if (group.finished || !sector.equals(group.sector.key)) continue; List<Npc> members = group.members.stream().map(World::getNpc).filter(npc -> npc != null && !npc.isDead()).toList(); if (!members.isEmpty()) values.add(new Quote(group, members, price(members.size()), informant)); } return values; }
    private String mailBody(Player player, Quote quote) { StringBuilder body = new StringBuilder(text(player, "tc.bosses.informant.mail.intro", "PH_BOSS", quote.group.name, "PH_LEVEL", Integer.toString(quote.group.level))).append('\n'); for (Npc member : quote.members) { Vector3f position = member.getPosition(); body.append(text(player, "tc.bosses.informant.mail.member", "PH_NAME", member.getName(), "PH_X", coordinate(position.x), "PH_Y", coordinate(position.y), "PH_Z", coordinate(position.z))).append('\n'); } return body.append(text(player, "tc.bosses.informant.mail.snapshot")).toString(); }
    private String coordinate(float value) { return Integer.toString(Math.round(value)); }
    private BossInformant fromNpc(Npc npc, String name, boolean male, String accountId) { Vector3f p = npc.getPosition(); Quaternion r = npc.getRotation(); return new BossInformant(npc.getGlobalID(), name, male, p.x, p.y, p.z, r.x, r.y, r.z, r.w, accountId); }
    private BossInformant ensureAccount(BossInformant informant) {
        WalletBridge wallet = new WalletBridge(plugin);
        if (!wallet.isAvailable() || !wallet.hasSystemAccountApi()) return null;
        BossInformant resolved = informant;
        if (resolved.accountId().isBlank()) {
            resolved = new BossInformant(informant.npcId(), informant.name(), informant.male(), informant.x(), informant.y(), informant.z(), informant.rx(), informant.ry(), informant.rz(), informant.rw(), newAccountId());
            try { repository.save(resolved); } catch (SQLException ex) { BossUtils.logger().error("Cannot migrate Headhunter Informant account: " + ex.getMessage()); return null; }
        }
        return wallet.createSystemAccount(resolved.accountId(), "INFORMANT", resolved.name(), PLUGIN_NAME).success() ? resolved : null;
    }
    private String newAccountId() { return "bosses:informant:" + UUID.randomUUID(); }
    private void initialize(Npc npc, BossInformant informant) { plugin.executeDelayed(.1f, () -> configure(npc, informant)); plugin.executeDelayed(.5f, () -> configure(npc, informant)); }
    private void configure(Npc npc, BossInformant informant) { if (npc == null || npc.isDead()) return; try { npc.setName(informant.name()); npc.setLocked(true); npc.setStatic(false); npc.setInteractable(true); npc.setInvincible(true); Skin skin = npc.getSkin(); skin.setGender(informant.male() ? Skin.Gender.Male : Skin.Gender.Female); skin.setSkinColor(informant.male() ? 0xC68642 : 0xF1C27D); skin.setHairColor(informant.male() ? 0x1C120C : 0x5C3B24); skin.setEyeColor(0x4E7AA8); skin.setHairstyle(informant.male() ? (byte) 58 : (byte) 108); skin.setBeard((byte) (informant.male() ? 1 : -1)); npc.getClothes().removeAll(); for (String garment : OUTFIT) { ClothingDefinition definition = Definitions.getClothingDefinition(garment); if (definition != null) npc.getClothes().add((short) definition.id); } } catch (Exception ex) { BossUtils.logger().error("Cannot initialize Headhunter Informant " + informant.npcId() + ": " + ex.getMessage()); } }
    private String text(Player player, String key, String... replacements) { return BossUtils.message(i18n, key, player, replacements); }
    private record Quote(BossGroup group, List<Npc> members, long price, BossInformant informant) { }
    public record AdminRow(long npcId, String name, String sector, long balance) { }
}
