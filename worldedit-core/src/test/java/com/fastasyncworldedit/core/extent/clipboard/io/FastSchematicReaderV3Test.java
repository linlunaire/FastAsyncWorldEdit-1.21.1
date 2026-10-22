package com.fastasyncworldedit.core.extent.clipboard.io;

import com.sk89q.jnbt.NBTConstants;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FastSchematicReaderV3Test {

    private static final int DATA_VERSION = 3955; // Minecraft 1.21.1
    private static final BlockVector3 DIMENSIONS = BlockVector3.at(2, 3, 4);
    private static final BlockVector3 OFFSET = BlockVector3.at(5, 6, 7);
    private static final UUID CLIPBOARD_ID = new UUID(0, 0);

    @TempDir
    Path temporaryDirectory;

    private MockedStatic<WorldEdit> worldEdit;
    private Clipboard clipboard;

    @BeforeEach
    void setUpPlatform() {
        WorldEdit instance = mock(WorldEdit.class);
        PlatformManager platformManager = mock(PlatformManager.class);
        Platform platform = mock(Platform.class);
        when(instance.getPlatformManager()).thenReturn(platformManager);
        when(platformManager.queryCapability(Capability.WORLD_EDITING)).thenReturn(platform);
        when(platform.getDataVersion()).thenReturn(DATA_VERSION);
        worldEdit = mockStatic(WorldEdit.class);
        worldEdit.when(WorldEdit::getInstance).thenReturn(instance);
        clipboard = mock(Clipboard.class);
    }

    @AfterEach
    void restoreWorldEdit() {
        if (worldEdit != null) {
            worldEdit.close();
        }
    }

    @Test
    void clipboardFactoryReceivesDimensionsThatArithmeticCannotMutate() throws IOException {
        byte[] schematic = schematic(NBTConstants.TYPE_COMPOUND, 0, false);
        try (FastSchematicReaderV3 reader = new FastSchematicReaderV3(new ByteArrayInputStream(schematic))) {
            Clipboard result = reader.read(CLIPBOARD_ID, dimensions -> {
                // Clipboard factories commonly subtract one to calculate the maximum corner.
                assertEquals(BlockVector3.at(1, 2, 3), dimensions.subtract(BlockVector3.ONE));
                assertEquals(DIMENSIONS, dimensions, "Factory arithmetic must not mutate the reader's dimensions");
                return clipboard;
            });
            assertSame(clipboard, result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {NBTConstants.TYPE_END, NBTConstants.TYPE_BYTE, NBTConstants.TYPE_COMPOUND})
    void readsEmptyEntityListsRegardlessOfElementType(int elementType) throws IOException {
        assertReadsEmptyEntities(new ByteArrayInputStream(schematic(elementType, 0, false)));
    }

    @ParameterizedTest
    @ValueSource(ints = {NBTConstants.TYPE_END, NBTConstants.TYPE_BYTE, NBTConstants.TYPE_COMPOUND})
    void readsCachedEmptyEntityListsBeforeDimensionsAndDataVersion(int elementType) throws IOException {
        assertReadsEmptyEntities(new ByteArrayInputStream(schematic(elementType, 0, true)));
    }

    @ParameterizedTest
    @ValueSource(ints = {NBTConstants.TYPE_END, NBTConstants.TYPE_BYTE, NBTConstants.TYPE_COMPOUND})
    void rereadsEmptyEntityListsFromResettableFiles(int elementType) throws IOException {
        Path file = temporaryDirectory.resolve("empty-entities.schem");
        Files.write(file, schematic(elementType, 0, true));
        assertReadsEmptyEntities(new FileInputStream(file.toFile()));
    }

    @ParameterizedTest
    @ValueSource(ints = {NBTConstants.TYPE_END, NBTConstants.TYPE_BYTE, NBTConstants.TYPE_STRING})
    void stillRejectsNonemptyEntityListsWithNoncompoundElements(int elementType) throws IOException {
        byte[] schematic = schematic(elementType, 1, false);
        try (FastSchematicReaderV3 reader = new FastSchematicReaderV3(new ByteArrayInputStream(schematic))) {
            IOException exception = assertThrows(IOException.class, () -> reader.read(CLIPBOARD_ID, ignored -> clipboard));
            assertEquals("Expected a compound block for tile entity list", exception.getMessage());
        }
    }

    private void assertReadsEmptyEntities(InputStream input) throws IOException {
        try (FastSchematicReaderV3 reader = new FastSchematicReaderV3(input)) {
            assertSame(clipboard, reader.read(CLIPBOARD_ID, dimensions -> {
                assertEquals(DIMENSIONS, dimensions);
                return clipboard;
            }));
            assertEquals(DATA_VERSION, reader.getDataVersion().orElseThrow());
            // Offset follows Entities in the stream, so this also checks that the empty list is fully consumed.
            verify(clipboard).setOrigin(OFFSET.multiply(-1));
            verifyNoMoreInteractions(clipboard);
        }
    }

    private static byte[] schematic(int elementType, int entityCount, boolean entitiesFirst) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
            writeNamedTag(output, NBTConstants.TYPE_COMPOUND, "");
            writeNamedTag(output, NBTConstants.TYPE_COMPOUND, "Schematic");
            writeNamedTag(output, NBTConstants.TYPE_INT, "Version");
            output.writeInt(3);
            if (entitiesFirst) {
                writeEntityList(output, elementType, entityCount);
            }
            writeNamedTag(output, NBTConstants.TYPE_INT, "DataVersion");
            output.writeInt(DATA_VERSION);
            writeNamedTag(output, NBTConstants.TYPE_SHORT, "Width");
            output.writeShort(DIMENSIONS.x());
            writeNamedTag(output, NBTConstants.TYPE_SHORT, "Height");
            output.writeShort(DIMENSIONS.y());
            writeNamedTag(output, NBTConstants.TYPE_SHORT, "Length");
            output.writeShort(DIMENSIONS.z());
            if (!entitiesFirst) {
                writeEntityList(output, elementType, entityCount);
            }
            writeNamedTag(output, NBTConstants.TYPE_INT_ARRAY, "Offset");
            output.writeInt(3);
            output.writeInt(OFFSET.x());
            output.writeInt(OFFSET.y());
            output.writeInt(OFFSET.z());
            output.writeByte(NBTConstants.TYPE_END);
            output.writeByte(NBTConstants.TYPE_END);
        }
        return bytes.toByteArray();
    }

    private static void writeEntityList(DataOutputStream output, int elementType, int count) throws IOException {
        writeNamedTag(output, NBTConstants.TYPE_LIST, "Entities");
        output.writeByte(elementType);
        output.writeInt(count);
    }

    private static void writeNamedTag(DataOutputStream output, int type, String name) throws IOException {
        output.writeByte(type);
        output.writeUTF(name);
    }

}
