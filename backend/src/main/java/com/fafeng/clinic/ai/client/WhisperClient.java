package com.fafeng.clinic.ai.client;

public interface WhisperClient {

    boolean isConfigured();

    String transcribe(byte[] audioBytes, String filename, String contentType);
}
