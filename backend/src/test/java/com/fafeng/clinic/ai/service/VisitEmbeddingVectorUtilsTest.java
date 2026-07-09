package com.fafeng.clinic.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisitEmbeddingVectorUtilsTest {

    @Test
    void toPgVectorLiteral_formatsArray() {
        assertEquals("[1.0,2.5]", VisitEmbeddingService.toPgVectorLiteral(new float[]{1.0f, 2.5f}));
    }
}
