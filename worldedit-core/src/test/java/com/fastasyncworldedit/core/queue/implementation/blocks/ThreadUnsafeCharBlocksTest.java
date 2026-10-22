package com.fastasyncworldedit.core.queue.implementation.blocks;

import com.fastasyncworldedit.core.extent.processor.heightmap.HeightMapType;
import com.fastasyncworldedit.core.math.BlockVector3ChunkMap;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThreadUnsafeCharBlocksTest {

    @Test
    void resetHandlesUninitializedMetadataAndCanBeRepeated() {
        ThreadUnsafeCharBlocks blocks = blocks(null, null, null, null);

        assertSame(blocks, blocks.reset());
        assertReset(blocks);
        assertSame(blocks, blocks.reset());
        assertReset(blocks);
    }

    @Test
    void resetClearsInitializedMetadataWithoutClearingSharedCollections() {
        FaweCompoundTag tag = FaweCompoundTag.of(LinCompoundTag.of(Map.of()));
        BlockVector3ChunkMap<FaweCompoundTag> tiles = new BlockVector3ChunkMap<>();
        tiles.put(1, 2, 3, tag);
        HashSet<FaweCompoundTag> entities = new HashSet<>();
        entities.add(tag);
        UUID removedEntity = UUID.randomUUID();
        HashSet<UUID> entityRemoves = new HashSet<>();
        entityRemoves.add(removedEntity);
        // Avoid bootstrapping the server's block registry just to obtain a height-map enum constant.
        @SuppressWarnings("unchecked")
        Map<HeightMapType, int[]> heightMaps = mock(Map.class);
        when(heightMaps.isEmpty()).thenReturn(false);
        ThreadUnsafeCharBlocks blocks = blocks(tiles, entities, entityRemoves, heightMaps);
        assertSame(heightMaps, blocks.getHeightMaps());

        blocks.reset();

        assertReset(blocks);
        assertSame(tag, tiles.get(1, 2, 3));
        assertTrue(entities.contains(tag));
        assertTrue(entityRemoves.contains(removedEntity));
    }

    @Test
    void metadataCanBeWrittenAgainAfterReset() {
        ThreadUnsafeCharBlocks blocks = blocks(
                new BlockVector3ChunkMap<>(), new HashSet<>(), new HashSet<>(), new HashMap<>()
        );
        blocks.reset();
        FaweCompoundTag tag = FaweCompoundTag.of(LinCompoundTag.of(Map.of()));
        UUID removedEntity = UUID.randomUUID();

        blocks.tile(1, 2, 3, tag);
        blocks.entity(tag);
        blocks.removeEntity(removedEntity);

        assertSame(tag, blocks.tile(1, 2, 3));
        assertTrue(blocks.entities().contains(tag));
        assertTrue(blocks.getEntityRemoves().contains(removedEntity));
        blocks.reset();
        assertReset(blocks);
    }

    private static ThreadUnsafeCharBlocks blocks(
            BlockVector3ChunkMap<FaweCompoundTag> tiles,
            HashSet<FaweCompoundTag> entities,
            HashSet<UUID> entityRemoves,
            Map<HeightMapType, int[]> heightMaps
    ) {
        return new ThreadUnsafeCharBlocks(
                new char[][]{new char[4096]}, 0, 0,
                new BiomeType[][]{new BiomeType[64]}, 1,
                new char[][]{new char[4096]}, new char[][]{new char[4096]},
                tiles, entities, entityRemoves, heightMaps, (char) 0,
                false, -1, SideEffectSet.none(), 4, 5
        );
    }

    private static void assertReset(ThreadUnsafeCharBlocks blocks) {
        assertNull(blocks.loadIfPresent(0));
        assertArrayEquals(new BiomeType[1][], blocks.getBiomes());
        assertArrayEquals(new char[1][], blocks.getLight());
        assertArrayEquals(new char[1][], blocks.getSkyLight());
        assertTrue(blocks.tiles().isEmpty());
        assertTrue(blocks.entities().isEmpty());
        assertTrue(blocks.getEntityRemoves().isEmpty());
        assertTrue(blocks.getHeightMaps().isEmpty());
        assertEquals(1, blocks.getSectionCount());
        assertEquals(4, blocks.getX());
        assertEquals(5, blocks.getZ());
    }

}
