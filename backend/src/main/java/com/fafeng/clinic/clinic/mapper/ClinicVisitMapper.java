package com.fafeng.clinic.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    @Select("""
            SELECT COALESCE(SUM(GREATEST(amount_due - amount_paid, 0)), 0)
            FROM clinic_visit
            WHERE patient_id = #{patientId}
              AND status = 'ACTIVE'
            """)
    BigDecimal sumArrearsByPatientId(@Param("patientId") Long patientId);

    @Select("""
            <script>
            SELECT v.*
            FROM clinic_visit v
            INNER JOIN patient p ON p.id = v.patient_id
            <where>
              v.status = 'ACTIVE'
              AND p.status = 'ACTIVE'
              <if test="keyword != null and keyword != ''">
                AND (
                  p.name ILIKE CONCAT('%', #{keyword}, '%')
                  OR p.phone ILIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(v.diagnosis, '') ILIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(v.chief_complaint, '') ILIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test="dateFrom != null">
                AND v.visit_time &gt;= #{dateFrom}
              </if>
              <if test="dateTo != null">
                AND v.visit_time &lt; #{dateTo}
              </if>
              <if test="arrearsOnly != null and arrearsOnly">
                AND v.amount_due &gt; v.amount_paid
              </if>
            </where>
            ORDER BY v.visit_time DESC, v.id DESC
            </script>
            """)
    IPage<ClinicVisit> searchPage(Page<ClinicVisit> page,
                                  @Param("keyword") String keyword,
                                  @Param("dateFrom") OffsetDateTime dateFrom,
                                  @Param("dateTo") OffsetDateTime dateTo,
                                  @Param("arrearsOnly") Boolean arrearsOnly);
}
