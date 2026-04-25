package xin.vanilla.narcissus.integration.ui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.util.ClientCostCalculator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ApricityUI extends Screen {

    private static final String path = NarcissusFarewell.MODID + "/index.html";

    private final SafeWorldCoordinate lastPlayerPos = new SafeWorldCoordinate();
    private long lastUpdateTime = 0;

    private Document document;
    private WaypointItem lastSelectedItem;
    private WaypointItem selectedItem;
    private Element selectedElement;

    public ApricityUI() {
        super(NarcissusComponent.get().literal("WaypointScreen").toChat());
    }

    @Override
    protected void init() {
        if (super.minecraft == null || super.minecraft.player == null) {
            return;
        }
        super.init();
        if (this.document != null) return;
        this.document = Document.create(path);

        PlayerTeleportData data = PlayerTeleportData.getData(super.minecraft.player);

        // panel-private: home
        List<WaypointItem> homeItems = new ArrayList<>();
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            homeItems.add(new WaypointItem().type(WaypointItem.Type.HOME).name(key.value()).safeWorldCoordinate(data.getHomeCoordinate().get(key)).canTeleport(true));
        }

        // panel-public: stage
        List<WaypointItem> stageItems = new ArrayList<>();
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : ClientStageData.getStageCoordinate().entrySet()) {
            stageItems.add(new WaypointItem().type(WaypointItem.Type.STAGE).name(entry.getKey().value()).safeWorldCoordinate(entry.getValue()).canTeleport(true));
        }

        // panel-footprints: back
        List<WaypointItem> backItems = new ArrayList<>();
        List<TeleportRecord> records = data.getTeleportRecords().stream()
                .filter(r -> r.getBefore() != null)
                .collect(Collectors.toList());
        java.util.Set<String> seenRecordTypes = new java.util.HashSet<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            TeleportRecord record = records.get(i);
            String recordTypeName = record.getTeleportType().name();
            boolean canTp = !seenRecordTypes.contains(recordTypeName);
            if (canTp) seenRecordTypes.add(recordTypeName);
            backItems.add(new WaypointItem().type(WaypointItem.Type.BACK).name(recordTypeName).safeWorldCoordinate(record.getBefore())
                    .canTeleport(canTp).recordType(recordTypeName));
        }

        // 默认选中第一个可用项
        WaypointItem firstItem = null;
        if (!homeItems.isEmpty()) firstItem = homeItems.get(0);
        else if (!stageItems.isEmpty()) firstItem = stageItems.get(0);
        else if (!backItems.isEmpty()) firstItem = backItems.get(0);
        if (firstItem != null) firstItem.selected(true);

        populatePanel(".panel-private .panel-list", homeItems, WaypointItem.Type.HOME);
        populatePanel(".panel-public .panel-list", stageItems, WaypointItem.Type.STAGE);
        populatePanel(".panel-footprints .panel-list", backItems, null);

        if (firstItem != null) {
            this.selectedItem = firstItem;
            this.lastSelectedItem = firstItem;
            Element firstEl = this.document.querySelector(".list-item.selected");
            if (firstEl != null) this.selectedElement = firstEl;
        }

        // 传送卡数量
        updateTicketCount(data.getTeleportCard());

        // 传送按钮点击与初始样式
        Element teleportBtn = this.document.querySelector(".teleport-btn");
        if (teleportBtn != null) {
            boolean canTp = firstItem != null && firstItem.canTeleport();
            teleportBtn.setAttribute("class", canTp ? "teleport-btn" : "teleport-btn disabled");
            teleportBtn.addEventListener("mousedown", event -> onTeleportClick());
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float tick) {
        if (this.selectedItem != null) {
            long now = System.currentTimeMillis();
            boolean needUpdate = (now - this.lastUpdateTime > 500 && this.selectedItem != this.lastSelectedItem) || isPlayerMoved();
            if (needUpdate) {
                this.lastUpdateTime = now;
                this.lastSelectedItem = this.selectedItem;
            }

            Element footer = document.querySelector(".footer");
            if (footer != null) {
                Element name = footer.querySelector(".detail-name");
                if (name != null) name.innerText = this.selectedItem.name();
                Element type = footer.querySelector(".detail-type");
                if (type != null) {
                    type.setAttribute("class", "detail-type " + this.selectedItem.getDetailTypeClass());
                    type.innerText = this.selectedItem.getDetailTypeName();
                }
                Element dim = footer.querySelector(".detail-dim");
                if (dim != null) dim.innerText = this.selectedItem.getDimensionName();
                Element coordinate = footer.querySelector(".detail-coord");
                if (coordinate != null) coordinate.innerText = this.selectedItem.getCoordinateName();
                Element distance = footer.querySelector(".detail-distance");
                if (distance != null) {
                    SafeWorldCoordinate pos = this.selectedItem.safeWorldCoordinate();
                    if (super.minecraft != null && super.minecraft.player != null && pos != null && pos.dimension() == super.minecraft.player.level().dimension()) {
                        distance.innerText = String.format("%sm", NumberUtils.toFixedEx(pos.distanceFrom(new SafeWorldCoordinate(super.minecraft.player)), 1));
                    } else {
                        distance.innerText = "∞m";
                    }
                }
                Element cost = footer.querySelector(".cost-value");
                if (cost != null) {
                    cost.innerText = calculateCostDisplay(this.selectedItem);
                }
            }
        }

        Element teleportBtn = document.querySelector(".teleport-btn");
        if (teleportBtn != null) {
            boolean canTp = this.selectedItem != null && this.selectedItem.canTeleport();
            teleportBtn.setAttribute("class", canTp ? "teleport-btn" : "teleport-btn disabled");
        }

        if (super.minecraft != null && super.minecraft.player != null) {
            PlayerTeleportData data = PlayerTeleportData.getData(super.minecraft.player);
            updateTicketCount(data.getTeleportCard());
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        Document.remove(path);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void populatePanel(String selector, List<WaypointItem> items, WaypointItem.Type delType) {
        Element panelList = this.document.querySelector(selector);
        if (panelList == null) return;
        panelList.children.clear();
        Consumer<WaypointItem> onDelete = (delType == WaypointItem.Type.HOME || delType == WaypointItem.Type.STAGE)
                ? this::showDelConfirmOverlay : null;
        for (WaypointItem item : items) {
            Element element = item.toElement(this.document, onDelete);
            updateItemElementClass(element, item, item.selected());
            element.addEventListener("mousedown", event -> selectedItem(element, item));
            panelList.append(element);
            if (item.selected()) {
                this.selectedElement = element;
                this.selectedItem = item;
            }
        }
    }

    private void showDelConfirmOverlay(WaypointItem item) {
        if (item == null || this.document == null) return;
        Element app = this.document.querySelector(".app");
        if (app == null) return;
        app.append(createDelConfirmOverlay(item));
    }

    private void removeDelConfirmOverlay() {
        if (this.document == null) return;
        Element overlay = this.document.querySelector(".del-confirm-overlay");
        if (overlay != null) overlay.remove();
    }

    private void doActualDelete(WaypointItem item) {
        if (item == null || item.safeWorldCoordinate() == null || minecraft == null || minecraft.player == null) return;
        int typeOrdinal = item.type() == WaypointItem.Type.HOME ? 0 : 1;
        String dimension = item.safeWorldCoordinate().dimensionId();
        PacketUtils.sendPacketToServer(new WaypointDelToServer(typeOrdinal, item.name(), dimension));
        ApricityUI self = this;
        new Thread(() -> {
            if (minecraft != null) {
                minecraft.execute(() -> {
                    if (minecraft.screen == self) {
                        Document.remove(path);
                        self.document = null;
                        self.init();
                    }
                });
            }
        }).start();
    }

    private Element createDelConfirmOverlay(WaypointItem item) {
        /*
        <div class="del-confirm-overlay">
            <div class="del-confirm-dialog">
                <div class="del-confirm-title-wrap">
                    <span class="del-confirm-title">确认删除</span>
                </div>
                <div class="del-confirm-msg-wrap">
                    <span class="del-confirm-msg">确定要删除该传送点吗？</span>
                </div>
                <div class="del-confirm-btns">
                    <span class="del-confirm-btn del-confirm-cancel">取消</span>
                    <span class="del-confirm-btn del-confirm-ok">删除</span>
                </div>
            </div>
        </div>
         */
        Element overlay = this.document.createElement("div");
        overlay.setAttribute("class", "del-confirm-overlay");

        Element dialog = this.document.createElement("div");
        dialog.setAttribute("class", "del-confirm-dialog");

        Element titleWrap = this.document.createElement("div");
        titleWrap.setAttribute("class", "del-confirm-title-wrap");
        Element title = this.document.createElement("span");
        title.setAttribute("class", "del-confirm-title");
        title.innerText = NarcissusComponent.get().transClientAuto("del_confirm_title").toString();
        titleWrap.append(title);
        dialog.append(titleWrap);

        Element msgWrap = this.document.createElement("div");
        msgWrap.setAttribute("class", "del-confirm-msg-wrap");
        Element msg = this.document.createElement("span");
        msg.setAttribute("class", "del-confirm-msg");
        msg.innerText = NarcissusComponent.get().transClientAuto("del_confirm_msg").toString();
        msgWrap.append(msg);
        dialog.append(msgWrap);

        Element btns = this.document.createElement("div");
        btns.setAttribute("class", "del-confirm-btns");

        Element cancelBtn = this.document.createElement("span");
        cancelBtn.setAttribute("class", "del-confirm-btn del-confirm-cancel");
        cancelBtn.innerText = NarcissusComponent.get().transClientAuto("cancel").toString();
        cancelBtn.addEventListener("mousedown", event -> {
            event.stopPropagation();
            removeDelConfirmOverlay();
        });

        Element okBtn = this.document.createElement("span");
        okBtn.setAttribute("class", "del-confirm-btn del-confirm-ok");
        okBtn.innerText = NarcissusComponent.get().transClientAuto("delete").toString();
        okBtn.addEventListener("mousedown", event -> {
            event.stopPropagation();
            removeDelConfirmOverlay();
            doActualDelete(item);
        });

        btns.append(cancelBtn);
        btns.append(okBtn);
        dialog.append(btns);
        overlay.append(dialog);

        return overlay;
    }

    private void updateItemElementClass(Element element, WaypointItem item, boolean selected) {
        String cls = "list-item";
        if (selected) cls += " selected";
        if (item != null && !item.canTeleport()) cls += " disabled";
        element.setAttribute("class", cls);
    }

    private void updateTicketCount(int count) {
        Element ticketNum = this.document.querySelector(".ticket-num");
        if (ticketNum != null) {
            ticketNum.innerText = String.valueOf(count);
        }
    }

    private String calculateCostDisplay(WaypointItem item) {
        if (item == null || item.safeWorldCoordinate() == null || minecraft == null || minecraft.player == null)
            return "";
        return ClientCostCalculator.formatCostDisplay(minecraft.player, item.safeWorldCoordinate(), itemTypeToEnum(item.type()));
    }

    private static EnumTeleportType itemTypeToEnum(WaypointItem.Type type) {
        switch (type) {
            case HOME:
                return EnumTeleportType.TP_HOME;
            case STAGE:
                return EnumTeleportType.TP_STAGE;
            case BACK:
                return EnumTeleportType.TP_BACK;
            default:
                return EnumTeleportType.TP_HOME;
        }
    }

    private void onTeleportClick() {
        if (this.selectedItem == null || !this.selectedItem.canTeleport()) return;
        EnumTeleportType type = itemTypeToEnum(this.selectedItem.type());
        String name = this.selectedItem.name();
        String dimension = "";
        if (type == EnumTeleportType.TP_HOME || type == EnumTeleportType.TP_STAGE) {
            if (this.selectedItem.safeWorldCoordinate() != null) {
                dimension = this.selectedItem.safeWorldCoordinate().dimensionId();
            }
        } else if (type == EnumTeleportType.TP_BACK) {
            name = this.selectedItem.recordType() != null ? this.selectedItem.recordType() : "";
        }
        PacketUtils.sendPacketToServer(new WaypointTeleportToServer(type, name, dimension));
        this.onClose();
    }

    private boolean isPlayerMoved() {
        if (super.minecraft != null && super.minecraft.player != null) {
            boolean changed = lastPlayerPos.x() != super.minecraft.player.getX()
                    || lastPlayerPos.y() != super.minecraft.player.getY()
                    || lastPlayerPos.z() != super.minecraft.player.getZ()
                    || lastPlayerPos.dimension() != super.minecraft.player.level().dimension();
            if (changed) {
                lastPlayerPos.fromVec3(super.minecraft.player.position()).dimension(super.minecraft.player.level().dimension());
            }
            return changed;
        }
        return false;
    }

    private void selectedItem(Element element, WaypointItem item) {
        WaypointItem prevItem = this.selectedItem;
        this.lastSelectedItem = this.selectedItem;
        this.selectedItem = item;
        if (this.selectedElement != null && prevItem != null) {
            updateItemElementClass(this.selectedElement, prevItem, false);
        }
        String cls = "list-item selected";
        if (item != null && !item.canTeleport()) cls += " disabled";
        element.setAttribute("class", cls);
        this.selectedElement = element;
    }


    @Data
    @Accessors(fluent = true)
    public static class WaypointItem {
        private Type type;
        private String name;
        private SafeWorldCoordinate safeWorldCoordinate;
        private boolean selected;
        private boolean canTeleport = true;
        private String recordType;

        public enum Type {
            HOME,
            STAGE,
            BACK,
        }

        public String getDetailTypeClass() {
            switch (this.type) {
                case HOME:
                    return "detail-type-private";
                case STAGE:
                    return "detail-type-public";
                case BACK:
                    return "detail-type-footprints";
                default:
                    return "";
            }
        }

        public String getDetailTypeName() {
            switch (this.type) {
                case HOME:
                    return NarcissusComponent.get().transClientAuto("private").toString();
                case STAGE:
                    return NarcissusComponent.get().transClientAuto("public").toString();
                case BACK:
                    return NarcissusComponent.get().transClientAuto("footprints").toString();
                default:
                    return "";
            }
        }

        public String getDimensionName() {
            if (NarcissusLang.hasTranslation(EnumI18nType.WORD, getDimKey(safeWorldCoordinate.dimension()))) {
                return NarcissusComponent.get().transClientAuto(getDimKey(safeWorldCoordinate.dimension())).toString();
            } else {
                return safeWorldCoordinate.dimension().location().toString();
            }
        }

        public String getCoordinateName() {
            return String.format("(%s,%s,%s)", this.safeWorldCoordinate.xInt(), this.safeWorldCoordinate.yInt(), this.safeWorldCoordinate.zInt());
        }

        public Element toElement(Document document) {
            return toElement(document, null);
        }

        public Element toElement(Document document, Consumer<WaypointItem> onDelete) {
            /*
            <div class="list-item selected">
                <div class="item-content">
                    <span class="item-name">name</span>
                    <div class="item-meta">
                        <span class="item-dim">dim</span>
                        <span class="item-coord">(x,y,z)</span>
                    </div>
                </div>
                <div class="item-del-wrap"><span class="item-del">×</span></div>
            </div>
            */
            Element div = document.createElement("div");
            String cls = "list-item";
            if (this.selected) cls += " selected";
            if (!this.canTeleport) cls += " disabled";
            div.setAttribute("class", cls);

            Element content = document.createElement("div");
            content.setAttribute("class", "item-content");

            Element spanName = document.createElement("span");
            spanName.setAttribute("class", "item-name");
            spanName.innerText = this.name;
            content.append(spanName);

            Element divMeta = document.createElement("div");
            divMeta.setAttribute("class", "item-meta");

            Element spanDim = document.createElement("span");
            spanDim.setAttribute("class", "item-dim");
            spanDim.innerText = getDimensionName();
            divMeta.append(spanDim);

            Element spanCoord = document.createElement("span");
            spanCoord.setAttribute("class", "item-coord");
            spanCoord.innerText = getCoordinateName();
            divMeta.append(spanCoord);

            content.append(divMeta);
            div.append(content);

            if (onDelete != null && (this.type == Type.HOME || this.type == Type.STAGE)) {
                Element delWarp = document.createElement("div");
                delWarp.setAttribute("class", "item-del-wrap");

                Element delBtn = document.createElement("span");
                delBtn.setAttribute("class", "item-del");
                delBtn.setAttribute("title", NarcissusComponent.get().transClientAuto("delete").toString());
                delBtn.innerText = "×";
                delBtn.addEventListener("mousedown", event -> {
                    event.stopPropagation();
                    onDelete.accept(this);
                });

                delWarp.append(delBtn);
                div.append(delWarp);
            }

            return div;
        }

        private static String getDimKey(ResourceKey<Level> dimension) {
            return "dim." + dimension.location().toString().replaceAll(":", ".");
        }
    }

}
