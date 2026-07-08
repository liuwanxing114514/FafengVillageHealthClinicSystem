package com.fafeng.clinic.patient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.patient.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    @Select("""
            <script>
            SELECT *
            FROM patient
            <where>
              status = 'ACTIVE'
              <if test="keyword != null and keyword != ''">
                AND (
                  name ILIKE CONCAT('%', #{keyword}, '%')
                  OR phone ILIKE CONCAT('%', #{keyword}, '%')
                  OR id_card ILIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            ORDER BY updated_at DESC, name ASC
            </script>
            """)
    IPage<Patient> searchPage(Page<Patient> page, @Param("keyword") String keyword);
}
