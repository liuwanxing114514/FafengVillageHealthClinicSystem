package com.fafeng.clinic.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ClinicVisitMapper extends BaseMapper<ClinicVisit> {

    @Select("""
            SELECT *
            FROM clinic_visit
            WHERE patient_id = #{patientId}
              AND status = 'ACTIVE'
            ORDER BY visit_time DESC, id DESC
            """)
    List<ClinicVisit> listByPatientId(@Param("patientId") Long patientId);
}
