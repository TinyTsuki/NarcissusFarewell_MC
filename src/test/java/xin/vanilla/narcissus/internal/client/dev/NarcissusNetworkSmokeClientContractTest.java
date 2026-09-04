package xin.vanilla.narcissus.internal.client.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeClientContractTest {

    @Test
    public void configEchoWaitsForTheSentinelValueInsteadOfTheFirstPlayerDataPacket() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "client", "dev", "NarcissusNetworkSmokeClientRunner.java")), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(runner.contains("if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {\n            return;\n        }"));
    }

    @Test
    public void networkingSmokeExcludesTheClientOnlyRenderer() throws Exception {
        String script = new String(Files.readAllBytes(Paths.get("gradle", "network-smoke.gradle")), StandardCharsets.UTF_8);

        assertTrue(script.contains("module: '5ZwdcRci'"));
    }
}
