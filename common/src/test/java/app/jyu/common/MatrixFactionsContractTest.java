package app.jyu.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatrixFactionsContractTest {
	private static final Pattern VERSION_ENTRY = Pattern.compile("(?m)^  - minecraft_version:");
	private static final Pattern FACTIONS_VERSION = Pattern.compile("(?m)^    factions_version:");

	@Test
	void everyVersionMatrixEntryPinsFactionsVersion() throws IOException {
		final var root = findRepoRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
		assertNotNull(root, "Could not locate repository root");

		final var matrix = Files.readString(root.resolve("ci/version-matrix.yml"));
		final var versionCount = countMatches(VERSION_ENTRY, matrix);
		final var factionsCount = countMatches(FACTIONS_VERSION, matrix);

		assertTrue(
			versionCount == factionsCount,
			"Every version matrix entry must pin factions_version: expected " + versionCount + ", found " + factionsCount
		);
	}

	@Test
	void allBuildSrcCommonPluginsKeepFactionsCompileOnlyDependency() throws IOException {
		final var root = findRepoRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
		assertNotNull(root, "Could not locate repository root");

		final var missing = findFiles(root, "multiloader-common.gradle").stream()
			.filter(path -> path.toString().contains("/buildSrc/"))
			.filter(path -> !sourceContains(path, "maven.modrinth:factions:${factions_version}"))
			.map(root::relativize)
			.map(Path::toString)
			.sorted()
			.toList();

		assertTrue(missing.isEmpty(), "Factions compile-only dependency missing from: " + missing);
	}

	@Test
	void allFabricMetadataFilesSuggestFactions() throws IOException {
		final var root = findRepoRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
		assertNotNull(root, "Could not locate repository root");

		final var missing = findFiles(root, "fabric.mod.json").stream()
			.filter(path -> !sourceContains(path, "\"factions\": \"*\""))
			.map(root::relativize)
			.map(Path::toString)
			.sorted()
			.toList();

		assertTrue(missing.isEmpty(), "Fabric metadata missing suggested Factions dependency: " + missing);
	}

	@Test
	void migrationScriptClearsRowScopedSableVersion() throws IOException {
		final var root = findRepoRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());
		assertNotNull(root, "Could not locate repository root");

		final var script = Files.readString(root.resolve("scripts/migrate-version.sh"));

		assertTrue(script.contains("OPTIONAL_MATRIX_PROPERTIES"), "Migration script must track optional row-scoped properties");
		assertTrue(script.contains("\"sable_version\""), "Migration script must clear stale sable_version between rows");
		assertTrue(script.contains("remove_property"), "Migration script must remove absent optional properties before applying a row");
	}

	private static int countMatches(Pattern pattern, String text) {
		final var matcher = pattern.matcher(text);
		var count = 0;

		while (matcher.find()) {
			count++;
		}

		return count;
	}

	private static List<Path> findFiles(Path root, String fileName) throws IOException {
		final var matches = new ArrayList<Path>();

		try (var files = Files.walk(root)) {
			files
				.filter(path -> path.getFileName().toString().equals(fileName))
				.filter(path -> !path.toString().contains("/build/"))
				.forEach(matches::add);
		}

		return matches;
	}

	private static Path findRepoRoot(Path start) {
		for (var path = start; path != null; path = path.getParent()) {
			if (Files.isDirectory(path.resolve("ci/version-overrides")) && Files.isDirectory(path.resolve("common/src/test"))) {
				return path;
			}
		}

		return null;
	}

	private static boolean sourceContains(Path path, String text) {
		try {
			return Files.readString(path).contains(text);
		} catch (IOException e) {
			return false;
		}
	}
}
