package com.fastasyncworldedit.core.queue;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class IChunkExtentTest {

    @Test
    void blockLightUsesBlockLightChannelAndChunkLocalCoordinates() {
        IChunk chunk = mock(IChunk.class);
        @SuppressWarnings("unchecked")
        IChunkExtent<IChunk> extent = mock(IChunkExtent.class, CALLS_REAL_METHODS);
        when(extent.getOrCreateChunk(-2, 2)).thenReturn(chunk);

        extent.setBlockLight(-17, -40, 34, 12);

        verify(chunk).setBlockLight(15, -40, 2, 12);
        verifyNoMoreInteractions(chunk);
    }

    @Test
    void skyLightStillUsesSkyLightChannel() {
        IChunk chunk = mock(IChunk.class);
        @SuppressWarnings("unchecked")
        IChunkExtent<IChunk> extent = mock(IChunkExtent.class, CALLS_REAL_METHODS);
        when(extent.getOrCreateChunk(1, -1)).thenReturn(chunk);

        extent.setSkyLight(18, 64, -1, 15);

        verify(chunk).setSkyLight(2, 64, 15, 15);
        verifyNoMoreInteractions(chunk);
    }
}
