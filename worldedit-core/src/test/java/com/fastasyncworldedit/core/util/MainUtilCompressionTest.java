package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.internal.io.FaweInputStream;
import com.fastasyncworldedit.core.internal.io.FaweOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MainUtilCompressionTest {

    private static final int BUFFER_SIZE = 1024;
    private static final byte[] FIRST_WRITE = "A small clipboard entity payload".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SECOND_WRITE = " followed by another entity".getBytes(StandardCharsets.UTF_8);

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void flushMakesSmallWritesReadableWithoutClosingTheStream(int compressionLevel) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (FaweOutputStream output = MainUtil.getCompressedOS(bytes, compressionLevel, BUFFER_SIZE)) {
            output.write(FIRST_WRITE);
            output.flush();
            assertFlushedData(bytes, FIRST_WRITE);

            // A flush must not finish the compression stream: subsequent writes must still work.
            output.write(SECOND_WRITE);
            output.flush();
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            expected.write(FIRST_WRITE);
            expected.write(SECOND_WRITE);
            assertFlushedData(bytes, expected.toByteArray());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    void flushedStreamsRemainReadableAfterClosing(int compressionLevel) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] payload = new byte[BUFFER_SIZE * 3 + 17];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31);
        }
        try (FaweOutputStream output = MainUtil.getCompressedOS(bytes, compressionLevel, BUFFER_SIZE)) {
            output.write(payload, 0, 19);
            output.flush();
            output.write(payload, 19, payload.length - 19);
        }
        try (FaweInputStream input = MainUtil.getCompressedIS(new ByteArrayInputStream(bytes.toByteArray()), BUFFER_SIZE)) {
            assertArrayEquals(payload, input.readAllBytes());
        }
    }

    private static void assertFlushedData(ByteArrayOutputStream bytes, byte[] expected) throws IOException {
        try (FaweInputStream input = MainUtil.getCompressedIS(new ByteArrayInputStream(bytes.toByteArray()), BUFFER_SIZE)) {
            // A flushed stream is deliberately unfinished, so only request the bytes already written.
            assertArrayEquals(expected, input.readNBytes(expected.length));
        }
    }

}
