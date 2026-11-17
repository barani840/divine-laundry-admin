package com.divinelaundry.service;

import com.divinelaundry.config.WhatsappProviderProperties;
import com.divinelaundry.domain.WhatsappDeliveryStatus;
import com.divinelaundry.domain.WhatsappMessage;
import com.divinelaundry.repository.WhatsappMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class WhatsappQueuedMessageDispatcher {
    private static final Logger log = LoggerFactory.getLogger(WhatsappQueuedMessageDispatcher.class);
    private final WhatsappMessageRepository messages;
    private final WhatsappProviderProperties properties;
    private final WhatsappService whatsapp;

    public WhatsappQueuedMessageDispatcher(
            WhatsappMessageRepository messages,
            WhatsappProviderProperties properties,
            WhatsappService whatsapp) {
        this.messages = messages;
        this.properties = properties;
        this.whatsapp = whatsapp;
    }

    @Scheduled(fixedDelayString = "${app.whatsapp.queue-poll-interval-ms:30000}")
    public void dispatchWaitingMessages() {
        if (!properties.isConfigured()) return;

        for (WhatsappMessage message : messages.findTop50ByDeliveryStatusOrderByCreatedAtAsc(
                WhatsappDeliveryStatus.WAITING_FOR_PROVIDER)) {
            if ("DOCUMENT".equalsIgnoreCase(message.getMediaType()) && !properties.isDocumentConfigured()) {
                continue;
            }
            if ("TEMPLATE".equalsIgnoreCase(message.getMediaType()) && !properties.isPaymentTemplateConfigured()) {
                continue;
            }
            try {
                whatsapp.dispatchWaitingMessage(message.getId());
            } catch (RuntimeException error) {
                log.warn("WhatsApp queued message dispatch failed messageId={}", message.getId());
            }
        }
    }
}
