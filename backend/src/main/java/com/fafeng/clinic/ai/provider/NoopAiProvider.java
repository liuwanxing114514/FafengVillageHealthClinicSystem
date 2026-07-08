package com.fafeng.clinic.ai.provider;

import org.springframework.stereotype.Component;

@Component
public class NoopAiProvider implements AiProvider {

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
