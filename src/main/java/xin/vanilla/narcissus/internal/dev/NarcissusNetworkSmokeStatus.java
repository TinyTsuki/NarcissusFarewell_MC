package xin.vanilla.narcissus.internal.dev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/** Dedicated network smoke 的 UTF-8 状态记录器。 */
public final class NarcissusNetworkSmokeStatus {
    public static final String ENABLE_PROPERTY = "narcissus.networkSmoke";
    public static final String PHASE_PROPERTY = "narcissus.networkSmoke.phase";
    public static final String STATUS_PROPERTY = "narcissus.networkSmoke.status";

    private static final Logger LOGGER = LogManager.getLogger();

    private NarcissusNetworkSmokeStatus() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    @Nonnull
    public static String phase() {
        return System.getProperty(PHASE_PROPERTY, "").trim();
    }

    public static synchronized void append(@Nonnull String line) {
        String configured = System.getProperty(STATUS_PROPERTY, "").trim();
        if (configured.isEmpty()) {
            throw new IllegalStateException("Missing " + STATUS_PROPERTY);
        }
        Path path = Paths.get(configured);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOGGER.info("Narcissus network smoke: {}", line);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to write network smoke status " + path, error);
        }
    }
}
