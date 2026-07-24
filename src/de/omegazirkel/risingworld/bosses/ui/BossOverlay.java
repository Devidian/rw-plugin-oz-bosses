package de.omegazirkel.risingworld.bosses.ui;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.bosses.BossViewService;
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
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

/** Player ranking plus an administrator-only sector threat tab. */
public final class BossOverlay extends BasePluginOverlayWithTabs {
    private enum BossTab { RANKING, THREAT, NPCS }
    private static final float TABLE_HEIGHT = 350;
    private final BossViewService view;
    private BossTab tab = BossTab.RANKING;
    private String selectedSpawnType = "";

    public BossOverlay(BossViewService view, Player player) {
        super(player, ignored -> player.deleteAttribute("oz.bosses.ui.overlay"));
        this.view = view;
        titleLabelKey = "TC_BOSSES_UI_TITLE";
        descLabelKey = "TC_BOSSES_UI_SUBTITLE";
        legendLabelKey = "TC_BOSSES_UI_LEGEND";
        rebuild();
    }
    @Override protected I18n t() { return view.i18n(); }
    @Override protected void setupTabs() {
        setupTabContainer();
        addTab(t().get("TC_BOSSES_UI_TAB_RANKING", uiPlayer), 180, tab == BossTab.RANKING, () -> { tab = BossTab.RANKING; rebuild(); });
        if (uiPlayer.isAdmin()) addTab(t().get("TC_BOSSES_UI_TAB_THREAT", uiPlayer), 180, tab == BossTab.THREAT, true, () -> { tab = BossTab.THREAT; rebuild(); });
        if (uiPlayer.isAdmin()) addTab(t().get("TC_BOSSES_UI_TAB_NPCS", uiPlayer), 180, tab == BossTab.NPCS, true, () -> { tab = BossTab.NPCS; rebuild(); });
        if (tab != BossTab.RANKING && !uiPlayer.isAdmin()) tab = BossTab.RANKING;
        if (tab == BossTab.RANKING) rankingTable(); else if (tab == BossTab.THREAT) threatTable(); else npcTable();
    }
    private void rankingTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("TC_BOSSES_UI_TH_PLAYER", uiPlayer), t().get("TC_BOSSES_UI_TH_SCORE", uiPlayer), t().get("TC_BOSSES_UI_TH_BOSS_KILLS", uiPlayer), t().get("TC_BOSSES_UI_TH_FOLLOWER_KILLS", uiPlayer), t().get("TC_BOSSES_UI_TH_DAMAGE", uiPlayer)), Arrays.asList(30f, 18f, 17f, 17f, 18f));
        table.setScrollBodyHeight(TABLE_HEIGHT);
        if (view.ranking().isEmpty()) table.addRow(textRow(t().get("TC_BOSSES_UI_EMPTY", uiPlayer), 100));
        else for (BossViewService.RankingRow row : view.ranking()) table.addRow(new TableRow(Arrays.asList(cell(row.playerName(),30),cell(row.score(),18),cell(row.bossKills(),17),cell(row.followerKills(),17),cell(row.damage(),18))));
        body.addChild(table);
    }
    private void threatTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("TC_BOSSES_UI_TH_SECTOR", uiPlayer), t().get("TC_BOSSES_UI_TH_THREAT", uiPlayer), t().get("TC_BOSSES_UI_TH_ACTIVE", uiPlayer), t().get("TC_BOSSES_UI_TH_SPAWN_CHANCE", uiPlayer), t().get("TC_BOSSES_UI_TH_ACTION", uiPlayer)), Arrays.asList(28f, 18f, 18f, 18f, 18f));
        table.setPosition(0, 44, false); table.setScrollBodyHeight(TABLE_HEIGHT - 44);
        if (view.threatLevels().isEmpty()) table.addRow(textRow(t().get("TC_BOSSES_UI_EMPTY", uiPlayer), 100));
        else for (BossViewService.SectorRow row : view.threatLevels()) table.addRow(new TableRow(Arrays.asList(cell(row.sector(),28),cell(row.threat(),18),cell(row.activeGroups(),18),cell(row.spawnChance() + "%",18),actionCell(row))));
        body.addChild(table);
        UILabel typeLabel = new UILabel(t().get("TC_BOSSES_UI_SPAWN_TYPE", uiPlayer));
        typeLabel.setPosition(12, 10, false); typeLabel.setSize(140, 30, false); typeLabel.setFontSize(13); body.addChild(typeLabel);
        List<DropdownOption> types = new ArrayList<>(); types.add(new DropdownOption("", t().get("TC_BOSSES_UI_SPAWN_RANDOM", uiPlayer))); types.addAll(view.spawnTypes().stream().map(type -> new DropdownOption(type.id(), type.label())).toList());
        Dropdown typeDropdown = new Dropdown(types, selectedSpawnType, selected -> selectedSpawnType = selected == null ? "" : selected);
        typeDropdown.setPosition(150, 8, false); typeDropdown.setSize(190, 30, false); body.addChild(typeDropdown);
    }
    private void npcTable() {
        TableScrollView table = new TableScrollView(Arrays.asList(t().get("TC_BOSSES_UI_TH_NPC_NAME", uiPlayer), t().get("TC_BOSSES_UI_TH_NPC_TYPE", uiPlayer), t().get("TC_BOSSES_UI_TH_NPC_GROUP", uiPlayer), t().get("TC_BOSSES_UI_TH_NPC_HEALTH", uiPlayer), t().get("TC_BOSSES_UI_TH_ACTION", uiPlayer)), Arrays.asList(29f, 18f, 14f, 12f, 27f));
        table.setScrollBodyHeight(TABLE_HEIGHT);
        List<BossViewService.NamedNpcRow> npcs = view.namedNpcs();
        if (npcs.isEmpty()) table.addRow(textRow(t().get("TC_BOSSES_UI_NO_NPCS", uiPlayer), 100));
        else for (BossViewService.NamedNpcRow npc : npcs) table.addRow(new TableRow(Arrays.asList(cell(npc.name(),29), cell(npc.type(),18), cell(npc.groupId(),14), cell(npc.health(),12), npcActionCell(npc))));
        body.addChild(table);
    }
    private TableRow textRow(String text, float width) { return new TableRow(Arrays.asList(cell(text, width))); }
    private TableCell actionCell(BossViewService.SectorRow row) {
        OZUIElement actions = new OZUIElement(); actions.setSize(148, 28, false);
        AdvancedButton spawn = AdvancedButtonFactory.defaultButton(t().get("TC_BOSSES_UI_SPAWN", uiPlayer), event -> { if (view.spawnInSector(row.sector(), selectedSpawnType)) rebuild(); else uiPlayer.sendTextMessage(t().get("TC_BOSSES_UI_SPAWN_UNAVAILABLE", uiPlayer)); });
        spawn.setPivot(Pivot.MiddleLeft); spawn.setPosition(0, 50, true); spawn.setSize(72, 26, false); actions.addChild(spawn);
        AdvancedButton clear = AdvancedButtonFactory.danger(t().get("TC_BOSSES_UI_CLEAR", uiPlayer), event -> showClearConfirmation(row));
        clear.setPivot(Pivot.MiddleLeft); clear.setPosition(76, 50, true); clear.setSize(72, 26, false); actions.addChild(clear);
        return new TableCell(actions, 18);
    }
    private void showClearConfirmation(BossViewService.SectorRow row) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(410, 190, false); dialog.setBackgroundColor(0, 0, 0, 0.94f); dialog.setBorder(1); dialog.setBorderColor(0.95f, 0.35f, 0.25f, 0.8f); addChild(dialog);
        UILabel text = new UILabel(t().get("TC_BOSSES_UI_CLEAR_CONFIRM", uiPlayer).replace("PH_SECTOR", row.sector())); text.setPivot(Pivot.UpperLeft); text.setPosition(20, 22, false); text.setSize(370, 78, false); text.setFontSize(15); text.setTextWrap(true); dialog.addChild(text);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("TC_BOSSES_UI_CANCEL", uiPlayer), event -> removeChild(dialog)); cancel.setPivot(Pivot.UpperLeft); cancel.setPosition(20, 132, false); cancel.setSize(140, 32, false); dialog.addChild(cancel);
        AdvancedButton confirm = AdvancedButtonFactory.danger(t().get("TC_BOSSES_UI_CLEAR", uiPlayer), event -> { removeChild(dialog); int cleared = view.clearActiveGroups(row.sector()); uiPlayer.sendTextMessage(t().get("TC_BOSSES_UI_CLEAR_DONE", uiPlayer).replace("PH_COUNT", Integer.toString(cleared))); rebuild(); }); confirm.setPivot(Pivot.UpperLeft); confirm.setPosition(250, 132, false); confirm.setSize(140, 32, false); dialog.addChild(confirm);
    }
    private TableCell npcActionCell(BossViewService.NamedNpcRow npc) {
        OZUIElement actions = new OZUIElement(); actions.setSize(148, 28, false);
        AdvancedButton teleport = AdvancedButtonFactory.defaultButton(t().get("TC_BOSSES_UI_TELEPORT", uiPlayer), event -> { if (!view.teleportToNpc(uiPlayer, npc.id())) uiPlayer.sendTextMessage(t().get("TC_BOSSES_UI_NPC_UNAVAILABLE", uiPlayer)); });
        teleport.setPivot(Pivot.MiddleLeft); teleport.setPosition(0, 50, true); teleport.setSize(72, 26, false); actions.addChild(teleport);
        AdvancedButton delete = AdvancedButtonFactory.danger(t().get("TC_BOSSES_UI_DELETE", uiPlayer), event -> showNpcDeleteConfirmation(npc));
        delete.setPivot(Pivot.MiddleLeft); delete.setPosition(76, 50, true); delete.setSize(72, 26, false); actions.addChild(delete);
        return new TableCell(actions, 27);
    }
    private void showNpcDeleteConfirmation(BossViewService.NamedNpcRow npc) {
        OZUIElement dialog = new OZUIElement(); dialog.setPivot(Pivot.MiddleCenter); dialog.setPosition(50, 50, true); dialog.setSize(470, 200, false); dialog.setBackgroundColor(0, 0, 0, 0.94f); dialog.setBorder(1); dialog.setBorderColor(0.95f, 0.35f, 0.25f, 0.8f); addChild(dialog);
        UILabel text = new UILabel(t().get("TC_BOSSES_UI_DELETE_CONFIRM", uiPlayer).replace("PH_NPC", npc.name())); text.setPivot(Pivot.UpperLeft); text.setPosition(20, 22, false); text.setSize(430, 78, false); text.setFontSize(15); text.setTextWrap(true); dialog.addChild(text);
        AdvancedButton cancel = AdvancedButtonFactory.cancel(t().get("TC_BOSSES_UI_CANCEL", uiPlayer), event -> removeChild(dialog)); cancel.setPivot(Pivot.UpperLeft); cancel.setPosition(16, 144, false); cancel.setSize(130, 32, false); dialog.addChild(cancel);
        AdvancedButton one = AdvancedButtonFactory.danger(t().get("TC_BOSSES_UI_DELETE_NPC", uiPlayer), event -> { removeChild(dialog); view.deleteNpc(npc.id(), false); rebuild(); }); one.setPivot(Pivot.UpperLeft); one.setPosition(170, 144, false); one.setSize(130, 32, false); dialog.addChild(one);
        AdvancedButton group = AdvancedButtonFactory.danger(t().get("TC_BOSSES_UI_DELETE_GROUP", uiPlayer), event -> { removeChild(dialog); view.deleteNpc(npc.id(), true); rebuild(); }); group.setPivot(Pivot.UpperLeft); group.setPosition(324, 144, false); group.setSize(130, 32, false); dialog.addChild(group);
    }
    private TableCell cell(Object value, float width) { UILabel label = new UILabel(String.valueOf(value)); label.setFont(Font.Default); label.setFontSize(13); label.setTextAlign(TextAnchor.MiddleLeft); return new TableCell(label,width); }
}
