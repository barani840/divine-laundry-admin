package com.divinelaundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsappProviderProperties(
        boolean enabled,
        String graphBaseUrl,
        String graphApiVersion,
        String phoneNumberId,
        String accessToken,
        String templateName,
        String templateLanguage,
        String documentTemplateName,
        String documentTemplateLanguage,
        String paymentTemplateName,
        String paymentTemplateLanguage,
        String paymentTemplateBodyParameterOrder,
        String paymentTemplateUrlButtonIndex,
        String paymentTemplateUrlPrefix,
        String paymentTemplateCategory) {

    private static final Set<String> PAYMENT_BODY_PARAMETERS = Set.of(
            "customer_name", "invoice_order_reference", "requested_amount", "payment_request_reference");

    @ConstructorBinding
    public WhatsappProviderProperties {
        graphBaseUrl = trim(graphBaseUrl);
        graphApiVersion = trim(graphApiVersion);
        phoneNumberId = trim(phoneNumberId);
        accessToken = trim(accessToken);
        templateName = trim(templateName);
        templateLanguage = trim(templateLanguage);
        documentTemplateName = trim(documentTemplateName);
        documentTemplateLanguage = trim(documentTemplateLanguage);
        paymentTemplateName = trim(paymentTemplateName);
        paymentTemplateLanguage = trim(paymentTemplateLanguage);
        paymentTemplateBodyParameterOrder = trim(paymentTemplateBodyParameterOrder);
        paymentTemplateUrlButtonIndex = trim(paymentTemplateUrlButtonIndex);
        paymentTemplateUrlPrefix = trim(paymentTemplateUrlPrefix);
        paymentTemplateCategory = trim(paymentTemplateCategory);
        }

        public WhatsappProviderProperties(
            boolean enabled,
            String graphBaseUrl,
            String graphApiVersion,
            String phoneNumberId,
            String accessToken,
            String templateName,
            String templateLanguage,
            String documentTemplateName,
            String documentTemplateLanguage) {
        this(enabled, graphBaseUrl, graphApiVersion, phoneNumberId, accessToken, templateName, templateLanguage,
            documentTemplateName, documentTemplateLanguage, "", "", "", "", "", "");
    }

    public WhatsappProviderProperties(
            boolean enabled,
            String graphBaseUrl,
            String graphApiVersion,
            String phoneNumberId,
            String accessToken,
            String templateName,
            String templateLanguage) {
        this(enabled, graphBaseUrl, graphApiVersion, phoneNumberId, accessToken,
            templateName, templateLanguage, "", templateLanguage, "", "", "", "", "", "");
    }

    public boolean isConfigured() {
        return enabled
                && validGraphBaseUrl()
                && hasText(graphApiVersion)
                && validPhoneNumberId()
                && hasText(accessToken)
                && hasText(templateName)
                && hasText(templateLanguage);
    }

    public String configurationMessage() {
        if (!enabled) return "WhatsApp automatic sending is disabled";
        if (!validGraphBaseUrl()) return "WHATSAPP_GRAPH_BASE_URL must be an HTTP(S) URL";
        if (!hasText(graphApiVersion)) return "WHATSAPP_GRAPH_API_VERSION is required";
        if (!validPhoneNumberId()) return "WHATSAPP_PHONE_NUMBER_ID is invalid";
        if (!hasText(accessToken)) return "WHATSAPP_ACCESS_TOKEN is required";
        if (!hasText(templateName)) return "WHATSAPP_TEMPLATE_NAME is required";
        if (!hasText(templateLanguage)) return "WHATSAPP_TEMPLATE_LANGUAGE is required";
        return "WhatsApp provider configuration is incomplete";
    }

    public String documentConfigurationMessage() {
        if (!enabled) return "WhatsApp automatic sending is disabled";
        if (!hasText(documentTemplateName)) return "WHATSAPP_DOCUMENT_TEMPLATE_NAME is required";
        if (!hasText(documentTemplateLanguage)) return "WHATSAPP_DOCUMENT_TEMPLATE_LANGUAGE is required";
        if (!isConfigured()) return configurationMessage();
        return "WhatsApp document provider configuration is incomplete";
    }

    public boolean isDocumentConfigured() {
        return isConfigured() && hasText(documentTemplateName) && hasText(documentTemplateLanguage);
    }

    public boolean hasPaymentTemplateConfiguration() {
        return hasText(paymentTemplateName) || hasText(paymentTemplateLanguage)
                || hasText(paymentTemplateBodyParameterOrder) || hasText(paymentTemplateUrlButtonIndex)
                || hasText(paymentTemplateUrlPrefix) || hasText(paymentTemplateCategory);
    }

    public boolean isPaymentTemplateConfigured() {
        return isConfigured()
                && hasText(paymentTemplateName)
                && hasText(paymentTemplateLanguage)
                && "UTILITY".equalsIgnoreCase(paymentTemplateCategory)
                && isValidPaymentBodyParameterOrder()
                && paymentTemplateUrlButtonIndex != null && paymentTemplateUrlButtonIndex.matches("\\d+")
                && validPaymentUrlPrefix()
                && isDistinctPaymentTemplateName();
    }

    public List<String> paymentTemplateBodyParameters() {
        if (!hasText(paymentTemplateBodyParameterOrder)) return List.of();
        return Arrays.stream(paymentTemplateBodyParameterOrder.split(","))
                .map(String::trim)
                .toList();
    }

    public String paymentTemplateConfigurationMessage() {
        if (!isConfigured()) return configurationMessage();
        if (!hasText(paymentTemplateName)) return "WHATSAPP_PAYMENT_TEMPLATE_NAME is required";
        if (!hasText(paymentTemplateLanguage)) return "WHATSAPP_PAYMENT_TEMPLATE_LANGUAGE is required";
        if (!hasText(paymentTemplateCategory)) return "WHATSAPP_PAYMENT_TEMPLATE_CATEGORY is required";
        if (!"UTILITY".equalsIgnoreCase(paymentTemplateCategory)) {
            return "WHATSAPP_PAYMENT_TEMPLATE_CATEGORY must be UTILITY";
        }
        if (!isValidPaymentBodyParameterOrder()) return "WHATSAPP_PAYMENT_TEMPLATE_BODY_PARAMETER_ORDER is invalid";
        if (paymentTemplateUrlButtonIndex == null || !paymentTemplateUrlButtonIndex.matches("\\d+")) {
            return "WHATSAPP_PAYMENT_TEMPLATE_URL_BUTTON_INDEX must be a non-negative integer";
        }
        if (!validPaymentUrlPrefix()) return "WHATSAPP_PAYMENT_TEMPLATE_URL_PREFIX must be an HTTPS URL prefix";
        if (!isDistinctPaymentTemplateName()) return "Payment requests require a dedicated template name";
        return "Payment template configuration is incomplete";
    }

    public String endpoint(String resource) {
        String base = graphBaseUrl == null ? "" : graphBaseUrl.replaceAll("/+$", "");
        String version = graphApiVersion == null ? "" : graphApiVersion.replaceAll("^/+|/+$", "");
        return "%s/%s/%s/%s".formatted(base, version, phoneNumberId, resource);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String configurationState() {
        if (!enabled) return "DISABLED";
        return isConfigured() && isDocumentConfigured() ? "ENABLED_CONFIGURED" : "ENABLED_CONFIGURATION_INCOMPLETE";
    }

    private boolean validGraphBaseUrl() {
        if (!hasText(graphBaseUrl)) return false;
        try {
            URI uri = URI.create(graphBaseUrl);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && hasText(uri.getHost()) && uri.getUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private boolean validPhoneNumberId() {
        return hasText(phoneNumberId) && phoneNumberId.matches("[A-Za-z0-9_-]+");
    }

    private boolean isValidPaymentBodyParameterOrder() {
        List<String> parameters = paymentTemplateBodyParameters();
        return parameters.size() == PAYMENT_BODY_PARAMETERS.size()
                && new HashSet<>(parameters).equals(PAYMENT_BODY_PARAMETERS);
    }

    private boolean validPaymentUrlPrefix() {
        if (!hasText(paymentTemplateUrlPrefix)) return false;
        try {
            URI uri = URI.create(paymentTemplateUrlPrefix);
            return "https".equalsIgnoreCase(uri.getScheme()) && hasText(uri.getHost())
                    && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private boolean isDistinctPaymentTemplateName() {
        return !"laundry_invoice_utility".equalsIgnoreCase(paymentTemplateName)
                && !"_laundry_invoice_document".equalsIgnoreCase(paymentTemplateName)
                && !paymentTemplateName.equalsIgnoreCase(templateName)
                && (!hasText(documentTemplateName) || !paymentTemplateName.equalsIgnoreCase(documentTemplateName));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
