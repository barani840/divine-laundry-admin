package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.divinelaundry.domain.WhatsappDeliveryStatus;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class WhatsappQueuedMessageDispatcherTest {
    @Test
    void disabledProviderLeavesWaitingMessagesAlone() {
        WhatsappMessageRepository messages = mock(WhatsappMessageRepository.class);
        WhatsappService whatsapp = mock(WhatsappService.class);
        WhatsappProviderProperties properties = properties(false, "", "");
        WhatsappQueuedMessageDispatcher dispatcher = new WhatsappQueuedMessageDispatcher(messages, properties, whatsapp);

        dispatcher.dispatchWaitingMessages();

        verifyNoInteractions(messages, whatsapp);
    }

    @Test
    void configuredProviderDispatchesWaitingImageMessages() {
        WhatsappMessageRepository messages = mock(WhatsappMessageRepository.class);
        WhatsappService whatsapp = mock(WhatsappService.class);
        WhatsappProviderProperties properties = properties(true, "image-template", "");
        WhatsappMessage waiting = mock(WhatsappMessage.class);
        when(waiting.getId()).thenReturn(41L);
        when(waiting.getMediaType()).thenReturn("IMAGE");
        when(messages.findTop50ByDeliveryStatusOrderByCreatedAtAsc(WhatsappDeliveryStatus.WAITING_FOR_PROVIDER))
                .thenReturn(List.of(waiting));
        WhatsappQueuedMessageDispatcher dispatcher = new WhatsappQueuedMessageDispatcher(messages, properties, whatsapp);

        dispatcher.dispatchWaitingMessages();

        verify(whatsapp).dispatchWaitingMessage(41L);
    }

    @Test
    void documentMessagesWaitUntilDocumentTemplateIsConfigured() {
        WhatsappMessageRepository messages = mock(WhatsappMessageRepository.class);
        WhatsappService whatsapp = mock(WhatsappService.class);
        WhatsappProviderProperties properties = properties(true, "image-template", "");
        WhatsappMessage waiting = mock(WhatsappMessage.class);
        when(waiting.getId()).thenReturn(42L);
        when(waiting.getMediaType()).thenReturn("DOCUMENT");
        when(messages.findTop50ByDeliveryStatusOrderByCreatedAtAsc(WhatsappDeliveryStatus.WAITING_FOR_PROVIDER))
                .thenReturn(List.of(waiting));
        WhatsappQueuedMessageDispatcher dispatcher = new WhatsappQueuedMessageDispatcher(messages, properties, whatsapp);

        dispatcher.dispatchWaitingMessages();

        verifyNoInteractions(whatsapp);
    }

    private static WhatsappProviderProperties properties(boolean enabled, String imageTemplate, String documentTemplate) {
        return new WhatsappProviderProperties(enabled, "https://graph.example", "v-test", "123456789",
                "test-token", imageTemplate, "en", documentTemplate, "en");
    }
}
