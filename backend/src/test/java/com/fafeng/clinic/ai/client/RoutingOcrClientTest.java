package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.config.OcrServiceOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingOcrClientTest {

    @Mock
    private ExternalServiceConfigService externalServiceConfigService;
    @Mock
    private HttpOcrClient localOcrClient;
    @Mock
    private VisionOcrClient visionOcrClient;

    @InjectMocks
    private RoutingOcrClient routingOcrClient;

    @Test
    void localModeDelegatesToHttpOcrClient() {
        when(externalServiceConfigService.isOcrEnabled()).thenReturn(true);
        when(externalServiceConfigService.getOcrMode()).thenReturn(OcrServiceOptions.MODE_LOCAL);
        when(localOcrClient.isConfigured()).thenReturn(true);
        when(localOcrClient.recognize(new byte[] {1}, "a.jpg", "image/jpeg")).thenReturn("local-text");

        assertTrue(routingOcrClient.isConfigured());
        assertEquals("local-text", routingOcrClient.recognize(new byte[] {1}, "a.jpg", "image/jpeg"));
    }

    @Test
    void visionModeDelegatesToVisionOcrClient() {
        when(externalServiceConfigService.isOcrEnabled()).thenReturn(true);
        when(externalServiceConfigService.getOcrMode()).thenReturn(OcrServiceOptions.MODE_VISION);
        when(visionOcrClient.isConfigured()).thenReturn(true);
        when(visionOcrClient.recognize(new byte[] {2}, "b.jpg", "image/jpeg")).thenReturn("vision-text");

        assertTrue(routingOcrClient.isConfigured());
        assertEquals("vision-text", routingOcrClient.recognize(new byte[] {2}, "b.jpg", "image/jpeg"));
    }

    @Test
    void notConfiguredWhenOcrDisabled() {
        when(externalServiceConfigService.isOcrEnabled()).thenReturn(false);
        assertFalse(routingOcrClient.isConfigured());
    }
}
