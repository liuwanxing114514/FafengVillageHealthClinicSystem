package com.fafeng.clinic.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.clinic.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrescriptionItemMapper extends BaseMapper<PrescriptionItem> {

    @Select("""
            SELECT *
            FROM prescription_item
            WHERE prescription_id = #{prescriptionId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<PrescriptionItem> listByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    @Delete("DELETE FROM prescription_item WHERE prescription_id = #{prescriptionId}")
    int deleteByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
