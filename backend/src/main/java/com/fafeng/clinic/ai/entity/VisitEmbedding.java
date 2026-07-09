package com.fafeng.clinic.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@TableName("visit_embedding")
@Data
@NoArgsConstructor
public class VisitEmbedding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long visitId;
    private String textSummary;
    private String embeddingModel;
    private Integer embeddingDimensions;
    private OffsetDateTime sourceUpdatedAt;
    private OffsetDateTime syncedAt;
    private OffsetDateTime createdAt;
}
