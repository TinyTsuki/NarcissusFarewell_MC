package xin.vanilla.narcissus.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UiArtifactBoundaryTest {
    static List<String> violations(Map<String, byte[]> files, boolean merged) {
        return UiBoundaryChecks.artifact(files, "xin/vanilla/narcissus/", merged);
    }

    @Test
    public void ownedClassIsAllowedAndEmbeddedSdkRequiresMergedArchive() {
        lombok.var writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(52, 1, "xin/vanilla/narcissus/Example", null, "java/lang/Object", null);
        writer.visitEnd();
        assertTrue(violations(com.google.common.collect.ImmutableMap.of("xin/vanilla/narcissus/Example.class", writer.toByteArray()), false).isEmpty());
        writer = new org.objectweb.asm.ClassWriter(0);
        writer.visit(52, 1, "xin/vanilla/banira/Example", null, "java/lang/Object", null);
        writer.visitEnd();
        assertFalse(violations(com.google.common.collect.ImmutableMap.of("xin/vanilla/banira/Example.class", writer.toByteArray()), false).isEmpty());
        assertTrue(violations(com.google.common.collect.ImmutableMap.of("xin/vanilla/banira/Example.class", writer.toByteArray()), true).isEmpty());
    }

    @Test
    public void engineStubTestProbeAndSourceLeaksAreRejected() {
        for (String path : com.google.common.collect.ImmutableList.of("com/sighs/apricityui/Engine.class", "com/sighs/apricityui/Engine.java",
                "META-INF/banira-aui-compile-api.properties", "xin/vanilla/narcissus/InjectedTest.class", "xin/vanilla/narcissus/RuntimeProbe.class", "docs/task.md"))
            assertFalse(path, violations(com.google.common.collect.ImmutableMap.of(path, new byte[0]), false).isEmpty());
    }

    @Test
    public void nestedEngineAndPomLeaksAreRejected() throws Exception {
        assertTrue(UiBoundaryChecks.pom("<project/>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertFalse(UiBoundaryChecks.pom("<project><dependencies><dependency><artifactId>apricityui</artifactId></dependency></dependencies></project>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        lombok.var output = new java.io.ByteArrayOutputStream();
        try (lombok.var zip = new java.util.zip.ZipOutputStream(output)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("com/sighs/apricityui/Engine.class"));
            zip.write(new byte[0]);
            zip.closeEntry();
        }
        assertFalse(violations(com.google.common.collect.ImmutableMap.of("META-INF/jarjar/engine.jar", output.toByteArray()), true).isEmpty());
    }

    @Test
    public void futureClassesAndUnsafePathsAreRejected() {
        byte[] header = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, 0, 53};
        assertFalse(violations(com.google.common.collect.ImmutableMap.of("xin/vanilla/narcissus/Future.class", header), false).isEmpty());
        for (String path : com.google.common.collect.ImmutableList.of("../escape", "assets/../foreign", "C:/foreign"))
            assertFalse(violations(com.google.common.collect.ImmutableMap.of(path, new byte[0]), false).isEmpty());
    }

    @Test
    public void formalArchivesContainOnlyProductFiles() throws Exception {
        String inputs = System.getProperty("banira.ui.artifacts");
        assertNotNull("uiArtifactTest must supply archive providers", inputs);
        for (String file : inputs.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            assertTrue(file, Files.isRegularFile(java.nio.file.Paths.get(file)));
            assertEquals(file, com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.artifact(UiBoundaryChecks.readZip(java.nio.file.Paths.get(file)),
                    "xin/vanilla/narcissus/", file.endsWith("-all.jar"), com.google.common.collect.ImmutableMap.of(
                            "META-INF/jars/core-3.6.4.jar", "25B926FD75C735867EB552786C2A87A7519301F07560E5F83D3DD9D044DA5CA9",
                            "META-INF/jars/toml-3.6.4.jar", "39A5CB91F0E7FC002644D125DA5DA06967E1CCC539BF2E3E36535DCE641CC999")));
        }
        String poms = System.getProperty("banira.ui.poms", "");
        if (!poms.isEmpty()) for (String pom : poms.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
            assertEquals(pom, com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.pom(Files.readAllBytes(java.nio.file.Paths.get(pom))));
    }

    @Test
    public void opaqueEngineCoordinatesAreRejected() {
        String xml = "<project><dependencies><dependency><groupId>maven.modrinth</groupId><artifactId>UfPdUeGy</artifactId></dependency></dependencies></project>";
        assertFalse(UiBoundaryChecks.pom(xml.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }

    @Test
    public void namespacedEngineDependenciesAreRejected() {
        String xml = "<m:project xmlns:m='http://maven.apache.org/POM/4.0.0'><m:dependencies><m:dependency><m:artifactId>apricityui</m:artifactId></m:dependency></m:dependencies></m:project>";
        assertFalse(UiBoundaryChecks.pom(xml.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }

    @Test
    public void localAgentInstructionsAreRejected() {
        assertFalse(violations(com.google.common.collect.ImmutableMap.of("AGENTS.md", new byte[0]), false).isEmpty());
    }

    private byte[] library(Map<String, byte[]> files) throws Exception {
        lombok.var bytes = new java.io.ByteArrayOutputStream();
        try (lombok.var zip = new java.util.zip.ZipOutputStream(bytes)) {
            for (lombok.var entry : files.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    public void exactLibraryContentPermitIgnoresZipOrderButRejectsMutationAndRenaming() throws Exception {
        lombok.var entries = new java.util.LinkedHashMap<String, byte[]>();
        entries.put("META-INF/NOTICE", new byte[]{1});
        entries.put("com/electronwill/nightconfig/core/info.txt", new byte[]{2});
        byte[] original = library(entries);
        String hash = UiBoundaryChecks.libraryFingerprint(original);
        String name = "META-INF/jars/core-3.6.4.jar";
        Map<String, String> permits = com.google.common.collect.ImmutableMap.of(name, hash);
        assertFalse(violations(com.google.common.collect.ImmutableMap.of(name, original), false).isEmpty());
        assertTrue(UiBoundaryChecks.artifact(com.google.common.collect.ImmutableMap.of(name, original), "xin/vanilla/narcissus/", false, permits).isEmpty());
        lombok.var reversed = new java.util.LinkedHashMap<String, byte[]>();
        reversed.put("com/electronwill/nightconfig/core/info.txt", new byte[]{2});
        reversed.put("META-INF/NOTICE", new byte[]{1});
        assertEquals(hash, UiBoundaryChecks.libraryFingerprint(library(reversed)));
        reversed.put("META-INF/NOTICE", new byte[]{3});
        assertFalse(UiBoundaryChecks.artifact(com.google.common.collect.ImmutableMap.of(name, library(reversed)), "xin/vanilla/narcissus/", false, permits).isEmpty());
        assertFalse(UiBoundaryChecks.artifact(com.google.common.collect.ImmutableMap.of("META-INF/jars/other.jar", original), "xin/vanilla/narcissus/", false, permits).isEmpty());
    }

    @Test
    public void exactPermitDoesNotHideEngineSdkOrSecondLevelArchiveLeaks() throws Exception {
        String name = "META-INF/jars/core-3.6.4.jar";
        for (lombok.var entry : com.google.common.collect.ImmutableList.of(com.google.common.collect.ImmutableMap.of("com/sighs/apricityui/Engine.class", new byte[]{0}),
                com.google.common.collect.ImmutableMap.of("xin/vanilla/banira/hidden.txt", new byte[]{1}),
                com.google.common.collect.ImmutableMap.of("META-INF/jars/hidden.jar", library(com.google.common.collect.ImmutableMap.of("entry.txt", new byte[]{2}))))) {
            byte[] bytes = library(entry);
            assertFalse(UiBoundaryChecks.artifact(com.google.common.collect.ImmutableMap.of(name, bytes), "xin/vanilla/narcissus/", false,
                    com.google.common.collect.ImmutableMap.of(name, UiBoundaryChecks.libraryFingerprint(bytes))).isEmpty());
        }
    }
}
