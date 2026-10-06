package xin.vanilla.narcissus.internal.neoforge.cost;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.BaniraPlayerDataService;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.service.cost.CostPaymentService;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

public class NeoForgeCostPaymentTest {
    private NeoForgeCostPlayerFixture fixture;
    private NeoForgeCostPlayerFixture.RecordingPlayer payer, moving;
    private PlayerTeleportData data;
    private EnumCostType type = EnumCostType.EXP_POINT;
    private EnumCardType mode = EnumCardType.REQUIRE_ONE_WITH_COST;
    private boolean enabled = true, connected = true, commandResult = true, omitRequestRoles;
    private int amount = 5, commands, syncs;
    private long now = 100;
    private String command, item = "minecraft:stone";
    private TeleportRequest request;
    private final Map<String, TeleportRequest> requests = new HashMap<>();
    private NeoForgeCostPayment nativePayment;
    private CostPaymentService service;

    @Before
    public void setup() throws Exception {
        fixture = new NeoForgeCostPlayerFixture();
        fixture.setup();
        payer = fixture.player();
        moving = payer;
        data = PlayerTeleportData.getData(payer);
        data.setTeleportCard(2);
        nativePayment = new NeoForgeCostPayment(() -> now, p -> connected && (p == payer || p == moving),
                requests::get, (p, value) -> {
            assertSame(payer, p);
            commands++;
            command = value;
            return commandResult;
        },
                p -> {
                    assertSame(payer, p);
                    syncs++;
                });
        service = new CostPaymentService(ctx -> CostCalculation.success(amount, amount), nativePayment);
    }

    @After
    public void cleanup() throws Exception {
        fixture.cleanup();
    }

    private CostContext context(CostPhase phase) {
        CostPlayerView payerView = view(payer), movingView = view(moving);
        return (CostContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CostContext.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "operationId":
                    return 1L;
                case "generationId":
                    return 1L;
                case "phase":
                    return phase;
                case "teleportType":
                    return request == null ? EnumTeleportType.TP_HOME : request.getTeleportType();
                case "costType":
                    return type;
                case "parameters":
                    return new CostParameters(type, 5, 0, 0, -1, item, "fee {amount} {amount} $1\\tail", "");
                case "cardSettings":
                    return new CostCardSettings(enabled, 0, mode);
                case "player":
                    return movingView;
                case "payer":
                    return payerView;
                case "requester":
                    return request == null || omitRequestRoles ? Optional.empty() : Optional.of(view(request.getRequester()));
                case "targetPlayer":
                    return request == null || omitRequestRoles ? Optional.empty() : Optional.of(view(request.getTarget()));
                case "request":
                    return Optional.ofNullable(request).map(r -> new CostRequestInfo(r.getRequestId(),
                            r.getRequestTime().getTime(), r.getExpireTime(), false, false));
                case "destination":
                    return Optional.of(new CostPosition("minecraft:overworld", 10, 64, 0, 0, 0, false, EnumSafeMode.NONE));
                case "targetWorld":
                    return Optional.of(Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CostWorldView.class},
                            (w, wm, wa) -> {
                                if (wm.getName().equals("dimensionId")) return "minecraft:overworld";
                                throw new UnsupportedOperationException();
                            }));
                case "nativeServer":
                    return payer.server;
                case "cooldownSeconds":
                    return 0;
                default:
                    throw new UnsupportedOperationException(m.getName());
            }
        });
    }

    private CostPlayerView view(ServerPlayer player) {
        return (CostPlayerView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CostPlayerView.class}, (p, m, a) -> {
            switch (m.getName()) {
                case "uuid":
                    return player.getUUID();
                case "nativePlayer":
                    return ((Class<?>) a[0]).cast(player);
                case "nativeTeleportData":
                    return ((Class<?>) a[0]).cast(PlayerTeleportData.getData(player));
                case "teleportCards":
                    return PlayerTeleportData.getData(player).peekTeleportCard();
                default:
                    throw new UnsupportedOperationException(m.getName());
            }
        });
    }

    private CostPaymentPlan commit() {
        return service.commit(CostOperation.create(context(CostPhase.CHECK)), context(CostPhase.COMMIT));
    }

    @Test
    public void supportBlockMustRemainAvailableAfterTheSameItemFee() {
        type = EnumCostType.ITEM;
        payer.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
        CostPaymentPlan fee = CostPaymentPlan.ready(5, 0, 5, false, false);
        assertFalse(NeoForgeCostPayment.hasSupportItemAfterPayment(context(CostPhase.COMMIT), fee, new ItemStack(Items.STONE)));
        payer.getInventory().setItem(0, new ItemStack(Items.STONE, 6));
        assertTrue(NeoForgeCostPayment.hasSupportItemAfterPayment(context(CostPhase.COMMIT), fee, new ItemStack(Items.STONE)));
        assertEquals(6, payer.getInventory().getItem(0).getCount());
    }

    @Test
    public void prospectiveRequestCheckHasRolesButCannotCommitWithoutRegistration() throws Exception {
        liveRequest(EnumTeleportType.TP_ASK);
        assertEquals(EnumCostFailure.NONE, nativePayment.validate(context(CostPhase.CHECK)));
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, nativePayment.validate(context(CostPhase.COMMIT)));
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, nativePayment.validate(context(CostPhase.PREVIEW)));
        omitRequestRoles = true;
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, nativePayment.validate(context(CostPhase.CHECK)));
        assertEquals(10, payer.totalExperience);
        assertEquals(2, data.peekTeleportCard());
    }

    @Test
    public void supportBlockCanComeFromAnotherStackWithoutMatchingFeeTags() {
        type = EnumCostType.ITEM;
        item = "minecraft:stone{fee:1}";
        ItemStack tagged = new ItemStack(Items.STONE, 5);
        CompoundTag tag = new CompoundTag();
        tag.putInt("fee", 1);
        net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, tagged, tag);
        payer.getInventory().setItem(0, tagged);
        payer.getInventory().setItem(1, new ItemStack(Items.STONE));
        assertTrue(NeoForgeCostPayment.hasSupportItemAfterPayment(context(CostPhase.COMMIT),
                CostPaymentPlan.ready(5, 0, 5, false, false), new ItemStack(Items.STONE)));
        assertEquals(5, payer.getInventory().getItem(0).getCount());
    }

    @Test
    public void missingCardsCannotChangeNativeExperienceHealthFoodOrItems() {
        data.setTeleportCard(0);
        payer.getInventory().setItem(0, new ItemStack(Items.STONE, 10));
        for (EnumCostType resource : EnumCostType.values()) {
            if (resource == EnumCostType.NONE) continue;
            type = resource;
            assertEquals(EnumCostFailure.INSUFFICIENT_CARDS, commit().failure());
            assertEquals(10, payer.totalExperience);
            assertEquals(10, payer.experienceLevel);
            assertEquals(10, payer.health, 0);
            assertEquals(10, payer.food.getFoodLevel());
            assertEquals(10, payer.getInventory().getItem(0).getCount());
            assertEquals(0, commands);
        }
        assertEquals(0, syncs);
    }

    @Test
    public void exactNativeDebitsAreReadOnlyUntilCommit() {
        for (EnumCostType resource : Arrays.asList(EnumCostType.EXP_POINT, EnumCostType.EXP_LEVEL, EnumCostType.HEALTH, EnumCostType.HUNGER)) {
            type = resource;
            CostPaymentPlan preview = service.plan(CostCalculation.success(5, 5), context(CostPhase.PREVIEW));
            assertTrue(preview.isSuccess());
            assertEquals(10, payer.totalExperience);
            assertEquals(2, data.peekTeleportCard());
            assertTrue(commit().isCommitted());
            assertEquals(resource == EnumCostType.EXP_POINT ? 5 : 10, payer.totalExperience);
            assertEquals(resource == EnumCostType.EXP_LEVEL ? 5 : 10, payer.experienceLevel);
            assertEquals(resource == EnumCostType.HEALTH ? 5 : 10, payer.health, 0);
            assertEquals(resource == EnumCostType.HUNGER ? 5 : 10, payer.food.getFoodLevel());
            payer.totalExperience = payer.experienceLevel = 10;
            payer.health = 10;
            payer.food.setFoodLevel(10);
            data.setTeleportCard(2);
        }
        assertEquals(4, syncs);
    }

    @Test
    public void healthMustRemainPositiveAndInvalidResourceStateIsRejected() {
        type = EnumCostType.HEALTH;
        amount = 10;
        assertEquals(EnumCostFailure.INSUFFICIENT_RESOURCE, commit().failure());
        assertEquals(10, payer.health, 0);
        payer.health = Float.NaN;
        amount = 1;
        assertEquals(EnumCostFailure.ACTOR_UNAVAILABLE, commit().failure());
        assertEquals(2, data.peekTeleportCard());
    }

    @Test
    public void cancelledOrModifiedExperienceDebitMustNotChargeCards() {
        for (EnumCostType resource : Arrays.asList(EnumCostType.EXP_POINT, EnumCostType.EXP_LEVEL)) {
            type = resource;
            payer.cancelExperience = true;
            assertEquals(EnumCostFailure.PAYMENT_FAILED, commit().failure());
            assertEquals(10, payer.totalExperience);
            assertEquals(10, payer.experienceLevel);
            assertEquals(2, data.peekTeleportCard());
            payer.cancelExperience = false;
            payer.experienceFactor = 2;
            assertEquals(EnumCostFailure.PAYMENT_FAILED, commit().failure());
            assertEquals(10, payer.totalExperience);
            assertEquals(10, payer.experienceLevel);
            assertEquals(2, data.peekTeleportCard());
            payer.experienceFactor = 0;
        }
        assertEquals(0, syncs);
    }

    @Test
    public void itemPaymentMatchesIdAndEntireNbtAcrossAllInventorySlots() {
        type = EnumCostType.ITEM;
        item = "minecraft:stone{custom:1}";
        ItemStack matching = new ItemStack(Items.STONE, 2);
        CompoundTag tag = new CompoundTag();
        tag.putInt("custom", 1);
        net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, matching, tag);
        payer.getInventory().setItem(0, matching);
        payer.getInventory().setItem(1, new ItemStack(Items.STONE, 20));
        ItemStack extraTag = matching.copy();
        extraTag.setCount(20);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, extraTag, tagValue -> tagValue.putInt("extra", 2));
        payer.getInventory().setItem(2, extraTag);
        ItemStack offhand = matching.copy();
        offhand.setCount(3);
        payer.getInventory().setItem(40, offhand);
        assertTrue(commit().isCommitted());
        assertTrue(payer.getInventory().getItem(0).isEmpty());
        assertTrue(payer.getInventory().getItem(40).isEmpty());
        assertEquals(20, payer.getInventory().getItem(1).getCount());
        assertEquals(20, payer.getInventory().getItem(2).getCount());
        assertEquals(1, data.peekTeleportCard());
        assertEquals(1, syncs);
    }

    @Test
    public void legacyNbtListsAreNotComponentSyntax() {
        type = EnumCostType.ITEM;
        item = "minecraft:stone{labels:[\"[item]\"]}";
        amount = 1;
        enabled = false;
        ItemStack matching = new ItemStack(Items.STONE, 2);
        CompoundTag tag = new CompoundTag();
        net.minecraft.nbt.ListTag labels = new net.minecraft.nbt.ListTag();
        labels.add(net.minecraft.nbt.StringTag.valueOf("[item]"));
        tag.put("labels", labels);
        net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, matching, tag);
        payer.getInventory().setItem(0, matching);
        assertTrue(commit().isCommitted());
        assertEquals(1, payer.getInventory().getItem(0).getCount());
        assertEquals(2, data.peekTeleportCard());
    }

    @Test
    public void insufficientOrInvalidItemsDoNotPartiallyRemoveInventory() {
        type = EnumCostType.ITEM;
        payer.getInventory().setItem(0, new ItemStack(Items.STONE, 2));
        assertEquals(EnumCostFailure.INSUFFICIENT_RESOURCE, commit().failure());
        assertEquals(2, payer.getInventory().getItem(0).getCount());
        assertEquals(2, data.peekTeleportCard());
        item = "minecraft:missing_item";
        assertEquals(EnumCostFailure.INVALID_PAYMENT, commit().failure());
        assertEquals(0, syncs);
    }

    @Test
    public void commandUsesLiteralReplacementAndOnlyCommitExecutesIt() {
        type = EnumCostType.COMMAND;
        assertTrue(service.plan(CostCalculation.success(5, 5), context(CostPhase.CHECK)).commandPending());
        assertEquals(0, commands);
        assertTrue(commit().isCommitted());
        assertEquals("fee 5 5 $1\\tail", command);
        assertEquals(1, commands);
        assertEquals(1, data.peekTeleportCard());
        assertEquals(1, syncs);
    }

    @Test
    public void failedCommandRestoresReservedCardsAndDoesNotSync() {
        type = EnumCostType.COMMAND;
        commandResult = false;
        assertEquals(EnumCostFailure.COMMAND_FAILED, commit().failure());
        assertEquals(2, data.peekTeleportCard());
        assertEquals(1, commands);
        assertEquals(0, syncs);
    }

    @Test
    public void waivedCommandHasNoExternalSideEffects() {
        type = EnumCostType.COMMAND;
        mode = EnumCardType.WAIVE_COST;
        assertFalse(service.plan(CostCalculation.success(5, 5), context(CostPhase.PREVIEW)).commandPending());
        assertTrue(commit().isCommitted());
        assertEquals(0, commands);
        assertEquals(1, data.peekTeleportCard());
    }

    @Test
    public void freeTypeDoesNotDependOnCorruptLegacyCardBalance() {
        type = EnumCostType.NONE;
        data.setTeleportCard(-1);
        assertTrue(commit().isCommitted());
        assertEquals(-1, data.peekTeleportCard());
        assertEquals(0, syncs);
    }

    @Test
    public void thrownCommandRestoresCardsWithoutReexecutingOnRetry() {
        type = EnumCostType.COMMAND;
        nativePayment = new NeoForgeCostPayment(() -> now, p -> true, requests::get,
                (p, c) -> {
                    commands++;
                    throw new IllegalStateException("External command failed");
                }, p -> syncs++);
        service = new CostPaymentService(ctx -> CostCalculation.success(5, 5), nativePayment);
        CostOperation op = CostOperation.create(context(CostPhase.CHECK));
        assertEquals(EnumCostFailure.PAYMENT_FAILED, service.commit(op, context(CostPhase.COMMIT)).failure());
        assertEquals(2, data.peekTeleportCard());
        assertEquals(0, syncs);
        assertFalse(service.commit(op, context(CostPhase.COMMIT)).isSuccess());
        assertEquals(1, commands);
    }

    @Test
    public void actualPayerMustBeConnectedAndAlive() {
        connected = false;
        assertEquals(EnumCostFailure.ACTOR_UNAVAILABLE, commit().failure());
        connected = true;
        payer.health = 0;
        assertEquals(EnumCostFailure.ACTOR_UNAVAILABLE, commit().failure());
        assertEquals(10, payer.totalExperience);
        assertEquals(2, data.peekTeleportCard());
        assertEquals(0, syncs);
    }

    @Test
    public void deletedExpiredOrReboundRequestCannotCharge() throws Exception {
        liveRequest(EnumTeleportType.TP_ASK);
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, commit().failure());
        requests.put(request.getRequestId(), request);
        now = 200;
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, commit().failure());
        now = 100;
        assertTrue(commit().isCommitted());
    }

    private void liveRequest(EnumTeleportType type) throws Exception {
        request = new TeleportRequest().setRequester(payer).setTarget(moving).setTeleportType(type);
        Field time = TeleportRequest.class.getDeclaredField("requestTime");
        time.setAccessible(true);
        time.set(request, new Date(0));
        Field expiry = TeleportRequest.class.getDeclaredField("expireTime");
        expiry.setAccessible(true);
        expiry.set(request, 200L);
    }

    @Test
    public void requestMustBindBothNativeActorRoles() throws Exception {
        liveRequest(EnumTeleportType.TP_ASK);
        requests.put(request.getRequestId(), request);
        omitRequestRoles = true;
        assertEquals(EnumCostFailure.REQUEST_UNAVAILABLE, commit().failure());
        assertEquals(10, payer.totalExperience);
        assertEquals(2, data.peekTeleportCard());
    }

    @Test
    public void hereRequestMovesTheTargetButDebitsTheRequester() throws Exception {
        NeoForgeCostPlayerFixture other = new NeoForgeCostPlayerFixture();
        other.setup();
        try {
            moving = other.player();
            liveRequest(EnumTeleportType.TP_HERE);
            requests.put(request.getRequestId(), request);
            assertTrue(commit().isCommitted());
            assertEquals(5, payer.totalExperience);
            assertEquals(10, moving.totalExperience);
            assertEquals(1, data.peekTeleportCard());
        } finally {
            other.cleanup();
        }
    }

    @Test
    public void hereEndpointChangingDimensionCannotChargeForTheOldDestination() throws Exception {
        liveRequest(EnumTeleportType.TP_HERE);
        requests.put(request.getRequestId(), request);
        Field dimension = net.minecraft.world.level.Level.class.getDeclaredField("dimension");
        dimension.setAccessible(true);
        dimension.set(payer.world, net.minecraft.world.level.Level.NETHER);
        assertEquals(EnumCostFailure.UNKNOWN_TARGET, commit().failure());
        assertEquals(10, payer.totalExperience);
        assertEquals(2, data.peekTeleportCard());
    }

    @Test
    public void paidCardsWriteOneActualNbtSnapshotAfterCommit() throws Exception {
        List<CompoundTag> saves = bindPlayerStore(false);
        assertTrue(service.plan(CostCalculation.success(5, 5), context(CostPhase.PREVIEW)).isSuccess());
        assertEquals(0, saves.size());
        assertTrue(commit().isCommitted());
        assertEquals(1, saves.size());
        assertEquals(1, saves.get(0).getInt("teleportCard"));
        assertFalse(data.isDirty());
        assertEquals(1, syncs);
    }

    @Test
    public void storageFailureKeepsCardsDirtyAndOperationPaid() throws Exception {
        bindPlayerStore(true);
        CostOperation operation = CostOperation.create(context(CostPhase.CHECK));
        assertTrue(service.commit(operation, context(CostPhase.COMMIT)).isCommitted());
        assertTrue("Failed storage must remain retryable without another payment", data.isDirty());
        assertEquals(1, data.peekTeleportCard());
        assertEquals(5, payer.totalExperience);
        assertEquals(0, syncs);
        assertFalse(service.commit(operation, context(CostPhase.COMMIT)).isSuccess());
    }

    private List<CompoundTag> bindPlayerStore(boolean fail) throws Exception {
        Field owner = PlayerTeleportData.class.getDeclaredField("player");
        owner.setAccessible(true);
        owner.set(data, payer);
        List<CompoundTag> saves = new ArrayList<>();
        BaniraPlayerDataService store = new BaniraPlayerDataService() {
            public Object getOrCreate(UUID id, String modId) {
                throw new AssertionError("Existing cache was ignored");
            }

            public void put(UUID id, String modId, Object value) {
                assertEquals(payer.getUUID(), id);
                if (fail) throw new IllegalStateException("Storage failed");
                saves.add(((CompoundTag) value).copy());
            }

            public void flush(UUID id) {
                throw new AssertionError("No forced disk flush per payment");
            }
        };
        BaniraPlatforms.install((BaniraPlatform) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{BaniraPlatform.class}, (p, m, a) -> {
                    if (m.getName().equals("playerDataService")) return store;
                    throw new UnsupportedOperationException(m.getName());
                }));
        return saves;
    }
}
