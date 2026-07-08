package com.fafeng.clinic.importexcel;

import com.fafeng.clinic.importexcel.model.MedicineImportParsedRow;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class MedicineImportPreviewCache {

    private static final Duration TTL = Duration.ofHours(1);

    private final Map<String, CachedPreview> cache = new ConcurrentHashMap<>();

    public String put(List<MedicineImportParsedRow> rows) {
        cleanupExpired();
        String id = UUID.randomUUID().toString();
        cache.put(id, new CachedPreview(Instant.now(), List.copyOf(rows)));
        return id;
    }

    public List<MedicineImportParsedRow> get(String previewId) {
        cleanupExpired();
        CachedPreview cached = cache.get(previewId);
        if (cached == null) {
            return null;
        }
        return cached.rows();
    }

    public void remove(String previewId) {
        cache.remove(previewId);
    }

    private void cleanupExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        cache.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }

    private record CachedPreview(Instant createdAt, List<MedicineImportParsedRow> rows) {
    }
}
