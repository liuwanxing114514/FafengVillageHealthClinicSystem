package com.fafeng.clinic.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.ai.entity.QuickPhrase;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

@Mapper
public interface QuickPhraseMapper extends BaseMapper<QuickPhrase> {

    @Delete("""
            DELETE FROM quick_phrase
            WHERE source = 'HISTORY'
              AND use_count <= #{maxCount}
              AND (last_used_at IS NULL OR last_used_at < #{cutoff})
            """)
    int deleteStaleHistory(@Param("cutoff") OffsetDateTime cutoff, @Param("maxCount") int maxCount);
}
