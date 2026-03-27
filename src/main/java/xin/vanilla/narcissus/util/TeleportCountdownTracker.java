package xin.vanilla.narcissus.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TeleportCountdownTracker {

    /**
     * 相对倒计时开始位置的平方距离阈值
     */
    private static final double MOVE_EPSILON_SQ = 1e-4;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private TeleportCountdownTracker() {
    }

    public static final class Session {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final UUID playerId;
        private final double startX;
        private final double startY;
        private final double startZ;
        private final boolean watchMove;
        private final boolean watchDamage;

        private Session(UUID playerId, double startX, double startY, double startZ, boolean watchMove, boolean watchDamage) {
            this.playerId = playerId;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.watchMove = watchMove;
            this.watchDamage = watchDamage;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        /**
         * 倒计时正常结束、即将传送时调用：未取消则从表中移除并返回 true。
         */
        public boolean tryMarkCompleteAndRemove() {
            if (cancelled.get()) {
                return false;
            }
            SESSIONS.remove(playerId, this);
            return true;
        }

        void markCancelledOnly() {
            cancelled.set(true);
        }

        private void cancelSilently() {
            cancelled.set(true);
            SESSIONS.remove(playerId, this);
        }

        private void cancelWithNotify(ServerPlayerEntity player, boolean damageReason) {
            if (cancelled.getAndSet(true)) {
                return;
            }
            SESSIONS.remove(playerId, this);
            Component msg = damageReason
                    ? NarcissusComponent.get().transAuto("tp_countdown_cancelled_damage")
                    : NarcissusComponent.get().transAuto("tp_countdown_cancelled_move");
            MessageUtils.sendNotification(player, msg);
        }
    }

    /**
     * 开始新的传送倒计时会话；若该玩家已有会话则静默取消旧会话。
     */
    public static Session begin(ServerPlayerEntity player, boolean watchMove, boolean watchDamage) {
        UUID id = player.getUUID();
        Session session = new Session(id, player.getX(), player.getY(), player.getZ(), watchMove, watchDamage);
        Session old = SESSIONS.put(id, session);
        if (old != null) {
            old.cancelSilently();
        }
        return session;
    }

    public static void onPlayerLogout(UUID playerId) {
        Session s = SESSIONS.remove(playerId);
        if (s != null) {
            s.markCancelledOnly();
        }
    }

    public static void onPlayerHurt(ServerPlayerEntity player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null || !s.watchDamage || s.isCancelled()) {
            return;
        }
        s.cancelWithNotify(player, true);
    }

    public static void tickMovementCheck(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            if (!s.watchMove || s.isCancelled()) {
                continue;
            }
            ServerPlayerEntity p = server.getPlayerList().getPlayer(s.playerId);
            if (p == null) {
                s.cancelSilently();
                continue;
            }
            if (p.distanceToSqr(s.startX, s.startY, s.startZ) > MOVE_EPSILON_SQ) {
                s.cancelWithNotify(p, false);
            }
        }
    }
}
