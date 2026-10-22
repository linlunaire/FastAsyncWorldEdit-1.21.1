package com.fastasyncworldedit.core.history;

import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DiskStorageHistoryTest {

    private static final int INDEX = 7;
    private static final List<String> EXTENSIONS = List.of("bd", "bio", "nbtf", "nbtt", "entf", "entt");

    @TempDir
    Path directory;

    @Test
    void countsEachHistoryFileUsingItsOwnLength() throws IOException {
        long expected = 0;
        for (int i = 0; i < EXTENSIONS.size(); i++) {
            int length = 1 << i;
            Files.write(historyFile(EXTENSIONS.get(i)), new byte[length]);
            expected += length;
        }
        Files.write(directory.resolve("unrelated.txt"), new byte[128]);
        Files.write(directory.resolve("8.bd"), new byte[256]);

        assertEquals(expected, history().getSizeOnDisk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"bd", "bio", "nbtf", "nbtt", "entf", "entt"})
    void countsHistoryFileEvenWhenOtherFilesAreAbsent(String extension) throws IOException {
        Files.write(historyFile(extension), new byte[23]);

        assertEquals(23L, history().getSizeOnDisk());
    }

    @Test
    void missingHistoryHasZeroSize() {
        assertEquals(0L, history().getSizeOnDisk());
    }

    @Test
    void deletesAllHistoryFilesIncludingBiomeDataButLeavesOtherFiles() throws IOException {
        for (String extension : EXTENSIONS) {
            Files.write(historyFile(extension), new byte[]{1});
        }
        Path unrelated = Files.write(directory.resolve("unrelated.txt"), new byte[]{2});
        Path differentHistory = Files.write(directory.resolve("8.bio"), new byte[]{3});
        DiskStorageHistory history = history();

        history.deleteFiles();

        for (String extension : EXTENSIONS) {
            assertFalse(Files.exists(historyFile(extension)), extension + " history was not deleted");
        }
        assertTrue(Files.exists(unrelated));
        assertTrue(Files.exists(differentHistory));
        assertEquals(0L, history.getSizeOnDisk());

        // Already-absent files should remain harmless on a repeated cleanup.
        history.deleteFiles();
        assertTrue(Files.exists(differentHistory));
    }

    private DiskStorageHistory history() {
        return new DiskStorageHistory(directory.toFile(), mock(World.class), UUID.randomUUID(), INDEX);
    }

    private Path historyFile(String extension) {
        return directory.resolve(INDEX + "." + extension);
    }

}
