package com.fafeng.clinic.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.ai.entity.VisitEmbedding;
import com.fafeng.clinic.ai.model.SimilarVisitMatchRow;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface VisitEmbeddingMapper extends BaseMapper<VisitEmbedding> {

    @Select("""
            SELECT COUNT(*) FROM clinic_visit cv
            WHERE cv.status = 'ACTIVE'
            """)
    long countActiveVisits();

    @Select("""
            SELECT COUNT(*) FROM visit_embedding ve
            INNER JOIN clinic_visit cv ON cv.id = ve.visit_id
            WHERE cv.status = 'ACTIVE'
            """)
    long countSyncedActiveVisits();

    @Select("""
            SELECT COUNT(*) FROM clinic_visit cv
            LEFT JOIN visit_embedding ve ON ve.visit_id = cv.id
            WHERE cv.status = 'ACTIVE'
              AND (ve.id IS NULL OR cv.updated_at > ve.source_updated_at)
            """)
    long countPendingSync();

    @Select("""
            SELECT MAX(ve.synced_at) FROM visit_embedding ve
            """)
    OffsetDateTime findLatestSyncedAt();

    @Select("""
            SELECT cv.*
            FROM clinic_visit cv
            WHERE cv.status = 'ACTIVE'
            ORDER BY cv.id
            """)
    List<ClinicVisit> listActiveVisitsForFullSync();

    @Select("""
            SELECT cv.*
            FROM clinic_visit cv
            LEFT JOIN visit_embedding ve ON ve.visit_id = cv.id
            WHERE cv.status = 'ACTIVE'
              AND (ve.id IS NULL OR cv.updated_at > ve.source_updated_at)
            ORDER BY cv.id
            """)
    List<ClinicVisit> listActiveVisitsForIncrementalSync();

    @Insert("""
            INSERT INTO visit_embedding (
                visit_id, embedding, text_summary, embedding_model, embedding_dimensions,
                source_updated_at, synced_at, created_at
            ) VALUES (
                #{visitId}, #{embeddingVector}::vector, #{textSummary}, #{embeddingModel}, #{embeddingDimensions},
                #{sourceUpdatedAt}, #{syncedAt}, #{createdAt}
            )
            ON CONFLICT (visit_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                text_summary = EXCLUDED.text_summary,
                embedding_model = EXCLUDED.embedding_model,
                embedding_dimensions = EXCLUDED.embedding_dimensions,
                source_updated_at = EXCLUDED.source_updated_at,
                synced_at = EXCLUDED.synced_at
            """)
    int upsertEmbedding(@Param("visitId") Long visitId,
                        @Param("embeddingVector") String embeddingVector,
                        @Param("textSummary") String textSummary,
                        @Param("embeddingModel") String embeddingModel,
                        @Param("embeddingDimensions") int embeddingDimensions,
                        @Param("sourceUpdatedAt") OffsetDateTime sourceUpdatedAt,
                        @Param("syncedAt") OffsetDateTime syncedAt,
                        @Param("createdAt") OffsetDateTime createdAt);

    @Delete("""
            DELETE FROM visit_embedding ve
            USING clinic_visit cv
            WHERE ve.visit_id = cv.id AND cv.status = 'VOID'
            """)
    int deleteVoidVisitEmbeddings();

    @Select("""
            SELECT ve.visit_id AS visitId,
                   ve.text_summary AS textSummary,
                   cv.visit_time AS visitTime,
                   1 - (ve.embedding <=> #{queryVector}::vector) AS similarity
            FROM visit_embedding ve
            INNER JOIN clinic_visit cv ON cv.id = ve.visit_id
            WHERE cv.status = 'ACTIVE'
              AND (CAST(#{excludeVisitId} AS bigint) IS NULL OR ve.visit_id <> CAST(#{excludeVisitId} AS bigint))
            ORDER BY ve.embedding <=> #{queryVector}::vector
            LIMIT #{limit}
            """)
    List<SimilarVisitMatchRow> searchSimilar(@Param("queryVector") String queryVector,
                                             @Param("excludeVisitId") Long excludeVisitId,
                                             @Param("limit") int limit);
}
