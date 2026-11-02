package xin.vanilla.narcissus.client.ui;

import java.util.Set;

@lombok.Value
@lombok.experimental.Accessors(fluent = true)
@lombok.AllArgsConstructor
public class AccessPolicyPresentation {
    String tpaKey;
    String tphKey;

    public static AccessPolicyPresentation of(String uuid, Set<String> black, Set<String> white,
                                              Set<String> tpa, Set<String> tph) {
        boolean ignored = black.contains(uuid) || !white.isEmpty() && !white.contains(uuid);
        return new AccessPolicyPresentation(key(ignored, tpa.contains(uuid)), key(ignored, tph.contains(uuid)));
    }

    private static String key(boolean ignored, boolean automatic) {
        return ignored ? "access_request_ignored" : automatic ? "access_request_auto" : "access_request_manual";
    }
}
