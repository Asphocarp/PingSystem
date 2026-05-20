package app.jyu.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderTickDeltaContractTest {
	private static final String FRAME_DELTA_IN_WORLD_CONTEXT = "WorldRenderContext\\.of\\((?:(?!\\)\\);)[\\s\\S]){0,500}getGameTimeDeltaTicks\\(\\)";

	@Test
	void worldRenderContextUsesInterpolationPartialTick() throws IOException {
		final var root = findRepoRoot(Path.of(System.getProperty("user.dir")).toAbsolutePath());

		assertNotNull(root, "Could not locate repository root");

		try (var files = Files.walk(root)) {
			final var offenders = files
				.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !path.toString().contains("/build/"))
				.filter(path -> sourceContains(path, "WorldRenderContext.of("))
				.filter(path -> sourceContainsPattern(path, FRAME_DELTA_IN_WORLD_CONTEXT))
				.map(root::relativize)
				.map(Path::toString)
				.sorted()
				.toList();

			assertTrue(
				offenders.isEmpty(),
				"WorldRenderContext.tickDelta must use render partial tick, not frame delta: " + offenders
			);
		}
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

	private static boolean sourceContainsPattern(Path path, String pattern) {
		try {
			return Files.readString(path).matches("[\\s\\S]*" + pattern + "[\\s\\S]*");
		} catch (IOException e) {
			return false;
		}
	}
}
