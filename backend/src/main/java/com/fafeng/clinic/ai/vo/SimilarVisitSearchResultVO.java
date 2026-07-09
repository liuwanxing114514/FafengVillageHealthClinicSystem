package com.fafeng.clinic.ai.vo;

import java.util.List;

public record SimilarVisitSearchResultVO(
        boolean available,
        List<SimilarVisitVO> items
) {
}
