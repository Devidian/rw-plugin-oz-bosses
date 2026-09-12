package de.omegazirkel.risingworld.bosses.ui;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.bosses.BossViewService;
import de.omegazirkel.risingworld.bosses.BossInformantService;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.Dropdown;
import de.omegazirkel.risingworld.tools.ui.DropdownOption;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

/** Player ranking plus an administrator-only sector threat tab. */
public final class BossOverlay extends BasePluginOverlayWithTabs {
    private enum BossTab { RANKING, THREAT, NPCS, INFORMANTS }
    private static final float TABLE_HEIGHT = 350;
    private final BossViewService view;
    private BossTab tab = BossTab.RANKING;
    private String selectedSpawnType = "";

    public BossOverlay(BossViewService view, Player player) {
        super(player, ignored -> player.deleteAttribute("oz.bosses.ui.overlay"));
        this.view = view;
        titleLabelKey = "tc.bosses.ui.title";
        descLabelKey = "tc.bosses.ui.subtitle";
        legendLabelKey = "tc.bosses.ui.legend";
        rebuild();
    }
    @Override protected I18n t() { return view.i18n(); }
    @Override protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("tc.bosses.ui.tab.ranking", uiPlayer), 180, tab == BossTab.RANKING, () -> { tab = BossTab.RANKING; rebuild(); });
        if (uiPlayer.isAdmin()) addTab(t().get("tc.bosses.ui.tab.threat", uiPlayer), 180, tab == BossTab.THREAT, true, () -> { tab = BossTab.THREAT; rebuild(); });
        if (uiPlayer.isAdmin()) addTab(t().get("tc.bosses.ui.tab.npcs", uiPlayer), 180, tab == BossTab.NPCS, true, () -> { tab = BossTab.NPCS; rebuild(); });
        if (uiPlayer.isAdmin()) addTab(t().get("tc.bosses.ui.tab.informants", uiPlayer), 180, tab == BossTab.INFORMANTS, true, () -> { tab = BossTab.INFORMANTS; rebuild(); });
        if (tab != BossTab.RANKING && !uiPlayer.isAdmin()) tab = BossTab.RANKING;
        if (tab == BossTab.RANKING) rankingTable(); else if (tab == BossTab.THREAT) threatTable(); else if (tab == BossTab.NPCS) npcTable(); else informantTable();
    }
    private void rankingTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.bosses.ui.th.player", uiPlayer), t().get("tc.bosses.ui.th.score", uiPlayer), t().get("tc.bosses.ui.th.boss.kills", uiPlayer), t().get("tc.bosses.ui.th.follower.kills", uiPlayer), t().get("tc.bosses.ui.th.damage", uiPlayer)), Arrays.asList(30f, 18f, 17f, 17f, 18f));
        table.setScrollBodyHeight(TABLE_HEIGHT);
        if (view.ranking().isEmpty()) table.addRow(textRow(t().get("tc.bosses.ui.empty", uiPlayer), 100));
        else for (BossViewService.RankingRow row : view.ranking()) table.addRow(new TableRow(Arrays.asList(cell(row.playerName(),30),cell(row.score(),18),cell(row.bossKills(),17),cell(row.followerKills(),17),cell(row.damage(),18))));
        body.addChild(table);
    }
    private void threatTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.bosses.ui.th.sector", uiPlayer), t().get("tc.bosses.ui.th.threat", uiPlayer), t().get("tc.bosses.ui.th.active", uiPlayer), t().get("tc.bosses.ui.th.spawn.chance", uiPlayer), t().get("tc.bosses.ui.th.action", uiPlayer)), Arrays.asList(28f, 18f, 18f, 18f, 18f));
        table.setPosition(0, 44, false); table.setScrollBodyHeight(TABLE_HEIGHT - 44);
        if (view.threatLevels().isEmpty()) table.addRow(textRow(t().get("tc.bosses.ui.empty", uiPlayer), 100));
        else for (BossViewService.SectorRow row : view.threatLevels()) table.addRow(new TableRow(Arrays.asList(cell(row.sector(),28),cell(row.threat(),18),cell(row.activeGroups(),18),cell(row.spawnChance() + "%",18),actionCell(row))));
        body.addChild(table);
        UILabel typeLabel = new UILabel(t().get("tc.bosses.ui.spawn.type", uiPlayer));
        typeLabel.setPosition(12, 10, false); typeLabel.setSize(140, 30, false); typeLabel.setFontSize(13); body.addChild(typeLabel);
        List<DropdownOption> types = new ArrayList<>(); types.add(new DropdownOption("", t().get("tc.bosses.ui.spawn.random", uiPlayer))); types.addAll(view.spawnTypes().stream().map(type -> new DropdownOption(type.id(), type.label())).toList());
        Dropdown typeDropdown = new Dropdown(types, selectedSpawnType, selected -> selectedSpawnType = selected == null ? "" : selected);
        typeDropdown.setPosition(150, 8, false); typeDropdown.setSize(190, 30, false); body.addChild(typeDropdown);
    }
    private void npcTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.bosses.ui.th.npc.name", uiPlayer), t().get("tc.bosses.ui.th.npc.type", uiPlayer), t().get("tc.bosses.ui.th.npc.group", uiPlayer), t().get("tc.bosses.ui.th.npc.health", uiPlayer), t().get("tc.bosses.ui.th.action", uiPlayer)), Arrays.asList(29f, 18f, 14f, 12f, 27f));
        table.setScrollBodyHeight(TABLE_HEIGHT);
        List<BossViewService.NamedNpcRow> npcs = view.bossGroupNpcs();
        if (npcs.isEmpty()) table.addRow(textRow(t().get("tc.bosses.ui.no.npcs", uiPlayer), 100));
        else for (BossViewService.NamedNpcRow npc : npcs) table.addRow(new TableRow(Arrays.asList(cell(npc.name(),29), cell(npc.type(),18), cell(npc.groupId(),14), cell(npc.health(),12), npcActionCell(npc))));
        body.addChild(table);
    }
    private void informantTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("tc.bosses.ui.th.informant.name", uiPlayer), t().get("tc.bosses.ui.th.informant.sector", uiPlayer), t().get("tc.bosses.ui.th.informant.balance", uiPlayer), t().get("tc.bosses.ui.th.action", uiPlayer)), Arrays.asList(34f, 20f, 20f, 26f));
        table.setScrollBodyHeight(TABLE_HEIGHT);
        List<BossInformantService.AdminRow> informants = view.informants();
        if (informants.isEmpty()) table.addRow(textRow(t().get("tc.bosses.ui.no.informants", uiPlayer), 100));
        else for (BossInformantService.AdminRow informant : informants) table.addRow(new TableRow(Arrays.asList(cell(informant.name(), 34), cell(informant.sector(), 20), cell(informant.balance(), 20), informantActionCell(informant))));
        body.addChild(table);
    }
    private TableRow textRow(String text, float width) { return new TableRow(Arrays.asList(cell(text, width))); }
    private TableCell actionCell(BossViewService.SectorRow row) {
        OZUIElement actions = new OZUIElement(); actions.setSize(148, 28, false);
        AdvancedButton spawn = AdvancedButtonFactory.defaultButton(t().get("tc.bosses.ui.spawn", uiPlayer), event -> { if (view.spawnInSector(row.sector(), selectedSpawnType)) rebuild(); else uiPlayer.sendTextMessage(t().get("tc.bosses.ui.spawn.unavailable", uiPlayer)); });
        spawn.setPivot(Pivot.MiddleLeft); spawn.setPosition(0, 50, true); spawn.setSize(72, 26, false); actions.addChild(spawn);
        AdvancedButton clear = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.clear", uiPlayer), event -> showClearConfirmation(row));
        clear.setPivot(Pivot.MiddleLeft); clear.setPosition(76, 50, true); clear.setSize(72, 26, false); actions.addChild(clear);
        return new TableCell(actions, 18);
    }
    private void showClearConfirmation(BossViewService.SectorRow row) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(410, 190, false); dialog.setBackgroundColor(0, 0, 0, 0.94f); dialog.setBorder(1); dialog.setBorderColor(0.95f, 0.35f, 0.25f, 0.8f); addChild(dialog);
        UILabel text = new UILabel(t().get("tc.bosses.ui.clear.confirm", uiPlayer).replace("PH_SECTOR", row.sector())); text.setPivot(Pivot.UpperLeft); text.setPosition(20, 22, false); text.setSize(370, 78, false); text.setFontSize(15); text.setTextWrap(true); dialog.addChild(text);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("tc.bosses.ui.cancel", uiPlayer), event -> removeChild(dialog)); cancel.setPivot(Pivot.UpperLeft); cancel.setPosition(20, 132, false); cancel.setSize(140, 32, false); dialog.addChild(cancel);
        AdvancedButton confirm = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.clear", uiPlayer), event -> { removeChild(dialog); int cleared = view.clearActiveGroups(row.sector()); uiPlayer.sendTextMessage(t().get("tc.bosses.ui.clear.done", uiPlayer).replace("PH_COUNT", Integer.toString(cleared))); rebuild(); }); confirm.setPivot(Pivot.UpperLeft); confirm.setPosition(250, 132, false); confirm.setSize(140, 32, false); dialog.addChild(confirm);
    }
    private TableCell npcActionCell(BossViewService.NamedNpcRow npc) {
        OZUIElement actions = new OZUIElement(); actions.setSize(148, 28, false);
        AdvancedButton teleport = AdvancedButtonFactory.defaultButton(t().get("tc.bosses.ui.teleport", uiPlayer), event -> { if (!view.teleportToNpc(uiPlayer, npc.id())) uiPlayer.sendTextMessage(t().get("tc.bosses.ui.npc.unavailable", uiPlayer)); });
        teleport.setPivot(Pivot.MiddleLeft); teleport.setPosition(0, 50, true); teleport.setSize(72, 26, false); actions.addChild(teleport);
        AdvancedButton delete = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.delete", uiPlayer), event -> showNpcDeleteConfirmation(npc));
        delete.setPivot(Pivot.MiddleLeft); delete.setPosition(76, 50, true); delete.setSize(72, 26, false); actions.addChild(delete);
        return new TableCell(actions, 27);
    }
    private void showNpcDeleteConfirmation(BossViewService.NamedNpcRow npc) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(470, 200, false); dialog.setBackgroundColor(0, 0, 0, 0.94f); dialog.setBorder(1); dialog.setBorderColor(0.95f, 0.35f, 0.25f, 0.8f); addChild(dialog);
        UILabel text = new UILabel(t().get("tc.bosses.ui.delete.confirm", uiPlayer).replace("PH_NPC", npc.name())); text.setPivot(Pivot.UpperLeft); text.setPosition(20, 22, false); text.setSize(430, 78, false); text.setFontSize(15); text.setTextWrap(true); dialog.addChild(text);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("tc.bosses.ui.cancel", uiPlayer), event -> removeChild(dialog)); cancel.setPivot(Pivot.UpperLeft); cancel.setPosition(16, 144, false); cancel.setSize(130, 32, false); dialog.addChild(cancel);
        AdvancedButton one = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.delete.npc", uiPlayer), event -> { removeChild(dialog); view.deleteNpc(npc.id(), false); rebuild(); }); one.setPivot(Pivot.UpperLeft); one.setPosition(170, 144, false); one.setSize(130, 32, false); dialog.addChild(one);
        AdvancedButton group = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.delete.group", uiPlayer), event -> { removeChild(dialog); view.deleteNpc(npc.id(), true); rebuild(); }); group.setPivot(Pivot.UpperLeft); group.setPosition(324, 144, false); group.setSize(130, 32, false); dialog.addChild(group);
    }
    private TableCell informantActionCell(BossInformantService.AdminRow informant) {
        OZUIElement actions = new OZUIElement(); actions.setSize(148, 28, false);
        AdvancedButton details = AdvancedButtonFactory.defaultButton(t().get("tc.bosses.ui.details", uiPlayer), event -> showInformantDetails(informant));
        details.setPivot(Pivot.MiddleCenter); details.setPosition(50, 50, true); details.setSize(120, 26, false); actions.addChild(details);
        return new TableCell(actions, 26);
    }
    private void showInformantDetails(BossInformantService.AdminRow informant) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(470, 245, false); dialog.setBackgroundColor(0, 0, 0, 0.94f); dialog.setBorder(1); dialog.setBorderColor(0.85f, 0.65f, 0.2f, 0.8f); addChild(dialog);
        UILabel title = new UILabel(informant.name()); title.setPivot(Pivot.UpperLeft); title.setPosition(20, 18, false); title.setSize(430, 28, false); title.setFont(Font.DefaultBold); title.setFontSize(18); dialog.addChild(title);
        UITextField name = new UITextField(informant.name()); name.setPivot(Pivot.UpperLeft); name.setPosition(20, 62, false); name.setSize(290, 30, false); name.setMaxCharacters(120); dialog.addChild(name);
        AdvancedButton rename = AdvancedButtonFactory.defaultButton(t().get("tc.bosses.ui.informant.rename", uiPlayer), event -> name.getCurrentText(uiPlayer, value -> { if (view.renameInformant(informant.npcId(), value)) { removeChild(dialog); rebuild(); } else uiPlayer.sendTextMessage(t().get("tc.bosses.ui.informant.rename.failed", uiPlayer)); }));
        rename.setPivot(Pivot.UpperLeft); rename.setPosition(326, 62, false); rename.setSize(124, 30, false); dialog.addChild(rename);
        AdvancedButton dissolve = AdvancedButtonFactory.danger(t().get("tc.bosses.ui.informant.dissolve", uiPlayer), event -> { if (view.dissolveInformant(informant.npcId())) { removeChild(dialog); rebuild(); } else uiPlayer.sendTextMessage(t().get("tc.bosses.ui.informant.dissolve.failed", uiPlayer)); });
        dissolve.setPivot(Pivot.UpperLeft); dissolve.setPosition(300, 190, false); dissolve.setSize(150, 32, false); dialog.addChild(dissolve);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("tc.bosses.ui.cancel", uiPlayer), event -> removeChild(dialog)); cancel.setPivot(Pivot.UpperLeft); cancel.setPosition(20, 190, false); cancel.setSize(130, 32, false); dialog.addChild(cancel);
    }
    private TableCell cell(Object value, float width) { UILabel label = new UILabel(String.valueOf(value)); label.setFont(Font.Default); label.setFontSize(13); label.setTextAlign(TextAnchor.MiddleLeft); return new TableCell(label,width); }
}
