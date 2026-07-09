package com.fafeng.clinic.ai.client;

public interface OcrClient {

    boolean isConfigured();

    String recognize(byte[] imageBytes, String filename, String contentType);
}
