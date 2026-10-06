package xin.vanilla.narcissus.internal.dev;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationBudget;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Synthetic rich routes plus read-only HELP and guarded missing-request feedback.
 */
public final class NarcissusNetworkSmokeNotifications {
    public static final int EXPLICIT_COLOR = 0xFF55FF55;
    private final String phase;
    private boolean sent;
    private static boolean runtimeRecorded;
    private static final String FAKE_CLASS = "com.advancedfakeplayers.entity.FakeServerPlayer";
    private static final String FAKE_PLAYER_COORDINATE = "curse.maven:advanced-fake-player-1566022:8869054";
    private static boolean providerVerified;

    public NarcissusNetworkSmokeNotifications(String phase) {
        requirePhase(phase);
        this.phase = phase;
    }

    public boolean sendWhenReady(ServerPlayer player) {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        if (sent) return true;
        if (!PlayerUtils.isRemoteClientModInstalled(player, Banira.MOD_ID)) return false;
        recordRuntime();
        sendWhenReady(true, (type, component) -> MessageUtils.sendNotification(player, component, type));
        executeReadOnlyCommands(player);
        NarcissusNetworkSmokeStatus.append("PASS notification-routes-submitted phase=" + phase + " types=10 commands=2");
        return true;
    }

    private void executeReadOnlyCommands(ServerPlayer player) {
        if (!NarcissusFarewell.getTeleportRequest().isEmpty()) {
            throw new IllegalStateException("Refusing tpaskyes with pending requests");
        }
        PlayerTeleportData playerData = PlayerTeleportData.getData(player);
        boolean notified = playerData.isNotified();
        Vec3 position = player.position();
        Object dimension = player.serverLevel().dimension();
        try {
            int help = player.getServer().getCommands().getDispatcher().execute(
                    NarcissusUtils.getCommand(EnumCommandType.HELP), player.createCommandSourceStack());
            int missing = player.getServer().getCommands().getDispatcher().execute(
                    NarcissusUtils.getCommand(EnumCommandType.TP_ASK_YES), player.createCommandSourceStack());
            if (help != 1 || missing != 0 || !NarcissusFarewell.getTeleportRequest().isEmpty()
                    || !position.equals(player.position()) || !dimension.equals(player.serverLevel().dimension())) {
                throw new IllegalStateException("Read-only notification command contract changed");
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
            throw new IllegalStateException("Configured notification commands unavailable", error);
        } finally {
            playerData.setNotified(notified);
        }
        NarcissusNetworkSmokeStatus.append("PASS notification-real-callers-submitted phase=" + phase
                + " help=1 missing-request=1 teleports=0 notified=restored");
    }

    public static void recordRuntime() {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        verifyFakePlayerRuntime();
        if (runtimeRecorded) return;
        try {
            Object mod = ModList.get().getModContainerById(Banira.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("Banira mod container unavailable")).getMod();
            String baniraSource = BaniraCodex.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            if (mod == null || !baniraSource.equals(mod.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm())) {
                throw new IllegalStateException("Banira mod container does not own the loaded facade");
            }
            Path path = ModList.get().getModFileById(Banira.MOD_ID).getFile().getFilePath().toAbsolutePath();
            if (!Files.isRegularFile(path)) throw new IllegalStateException("Banira mod file unavailable: " + path);
            byte[] bytes = Files.readAllBytes(path);
            StringBuilder hash = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
                hash.append(String.format("%02x", b & 255));
            for (Class<?> type : new Class<?>[]{Component.class, MessageUtils.class, NotificationBudget.class}) {
                // Forge's modjar URL is evidence, not a filesystem URI; bind it to the registered Banira entrypoint.
                String source = type.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
                if (!baniraSource.equals(source) || type.getClassLoader() != BaniraCodex.class.getClassLoader()) {
                    throw new IllegalStateException("Loaded class does not match Banira mod source/loader: " + type.getName() + " " + source);
                }
                NarcissusNetworkSmokeStatus.append("RUNTIME " + type.getName() + " codeSource=" + source
                        + " loader-match=banira-entrypoint sha256=" + hash + " size=" + bytes.length + " path=" + path);
            }
            runtimeRecorded = true;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot fingerprint loaded Banira", error);
        }
    }

    public static void verifyFakePlayerRuntime() {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        if (providerVerified) return;
        try {
            requireProvider(FAKE_PLAYER_COORDINATE.equals(System.getProperty("narcissus.networkSmoke.fakePlayerCoordinate")),
                    "Unexpected Advanced Fake Players coordinate");
            Class<?> type = Class.forName(FAKE_CLASS);
            requireProvider(ServerPlayer.class.isAssignableFrom(type), "Advanced Fake Players actor is not a real ServerPlayer");
            net.minecraftforge.fml.ModContainer container = ModList.get().getModContainerById("advancedfakeplayers")
                    .orElseThrow(() -> new IllegalStateException("Advanced Fake Players mod container unavailable"));
            requireProvider("1.0.0".equals(container.getModInfo().getVersion().toString()), "Unexpected Advanced Fake Players version");
            Object mod = container.getMod();
            String source = type.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
            requireProvider(mod != null && type.getClassLoader() == mod.getClass().getClassLoader()
                            && source.equals(mod.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm()),
                    "Advanced Fake Players actor does not belong to the loaded Advanced Fake Players mod");
            Path path = ModList.get().getModFileById("advancedfakeplayers").getFile().getFilePath().toAbsolutePath();
            requireProvider(Files.isRegularFile(path), "Advanced Fake Players mod file unavailable");
            byte[] bytes = Files.readAllBytes(path);
            StringBuilder hash = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
                hash.append(String.format("%02x", b & 255));
            String expectedHash = System.getProperty("narcissus.networkSmoke.fakePlayerSha256", "");
            requireProvider(expectedHash.equals(hash.toString()), "Loaded Advanced Fake Players JAR differs from parent runtime fingerprint");
            NarcissusNetworkSmokeStatus.append("RUNTIME " + FAKE_CLASS + " provider=advancedfakeplayers coordinate=" + FAKE_PLAYER_COORDINATE
                    + " version=1.0.0 codeSource=" + source + " loader-match=advancedfakeplayers-entrypoint sha256=" + hash
                    + " size=" + bytes.length + " path=" + path);
            providerVerified = true;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot fingerprint loaded Advanced Fake Players", error);
        }
    }

    private static void requireProvider(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    boolean sendWhenReady(boolean installed, BiConsumer<String, Component> sender) {
        if (sent) return true;
        if (!installed) return false;
        // A transport failure aborts the smoke, never retries a partially submitted fixture.
        sent = true;
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) sender.accept(type, payload(phase, type));
        return true;
    }

    public static String token(String phase, String type) {
        requirePhase(phase);
        if (!Arrays.asList(NarcissusNotificationTypes.ALL_TYPE_IDS).contains(type)) {
            throw new IllegalArgumentException("Unknown notification type " + type);
        }
        return "[narcissus-notification:" + phase + ":" + type + "]";
    }

    public static Component payload(String phase, String type) {
        String token = token(phase, type);
        Component child = NarcissusComponent.get().literal("explicit-child").color(EXPLICIT_COLOR)
                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/narcissus-smoke " + token))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("hover " + token)));
        return NarcissusComponent.get().literal(token)
                .append(NarcissusComponent.get().literal(" nested ").append(child));
    }

    public static void verifyNested(Component received, String phase, String type) {
        Component expected = payload(phase, type);
        // NotificationBudget binds the receiver's language on every node before serialization.
        if (!received.isLanguageCodeEmpty()) bindLanguage(expected, received.languageCodeOrDefault("en_us"));
        if (!expected.text().equals(received.text()) || received.getChildren().size() != 1
                || !expected.getChildren().get(0).toJson().equals(received.getChildren().get(0).toJson())) {
            throw new IllegalStateException("Nested text/color/click/hover changed for " + token(phase, type));
        }
    }

    private static void bindLanguage(Component component, String language) {
        component.languageCode(language);
        for (Component child : component.getChildren()) bindLanguage(child, language);
    }

    /**
     * Independent approved routing expectation, not an echo of defaultDisplay().
     */
    public static EnumNotificationTypeDisplayMode expectedDisplay(String type) {
        switch (type) {
            case NarcissusNotificationTypes.INTERACTIVE_TP_FLOW:
            case NarcissusNotificationTypes.INTERACTIVE_SHARE:
            case NarcissusNotificationTypes.INTERACTIVE_HELP:
            case NarcissusNotificationTypes.INTERACTIVE_COORDINATE_LIST:
            case NarcissusNotificationTypes.INTERACTIVE_QUERY:
                return EnumNotificationTypeDisplayMode.VANILLA_CHAT;
            default:
                return EnumNotificationTypeDisplayMode.OVERLAY;
        }
    }

    public static String verifiedMarker(String phase) {
        requirePhase(phase);
        return "PASS notification-regression phase=" + phase + " types=10 network=10 chat=5 overlay=5 factories=120";
    }

    public static boolean clientVerified(String phase, List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("FAIL ")) throw new IllegalStateException("Notification client failed: " + line);
        }
        return lines.contains(verifiedMarker(phase));
    }

    private static void requirePhase(String phase) {
        if (!"phase-one".equals(phase) && !"phase-two".equals(phase)) {
            throw new IllegalArgumentException("Unknown smoke phase " + phase);
        }
    }
}
