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
                  OR address ILIKE CONCAT('%', #{keyword}, '%')
                  OR COALESCE(remark, '') ILIKE CONCAT('%', #{keyword}, '%')
                  OR CAST(age AS TEXT) = #{keyword}
                )
              </if>
              <if test="name != null and name != ''">
                AND name ILIKE CONCAT('%', #{name}, '%')
              </if>
              <if test="phone != null and phone != ''">
                AND phone ILIKE CONCAT('%', #{phone}, '%')
              </if>
              <if test="idCard != null and idCard != ''">
                AND id_card ILIKE CONCAT('%', #{idCard}, '%')
              </if>
              <if test="address != null and address != ''">
                AND address ILIKE CONCAT('%', #{address}, '%')
              </if>
              <if test="gender != null and gender != ''">
                AND gender = #{gender}
              </if>
              <if test="remark != null and remark != ''">
                AND COALESCE(remark, '') ILIKE CONCAT('%', #{remark}, '%')
              </if>
              <if test="ageMin != null">
                AND age &gt;= #{ageMin}
              </if>
              <if test="ageMax != null">
                AND age &lt;= #{ageMax}
              </if>
            </where>
            ORDER BY updated_at DESC, name ASC
            </script>
            """)
    IPage<Patient> searchPage(Page<Patient> page,
                              @Param("keyword") String keyword,
                              @Param("name") String name,
                              @Param("phone") String phone,
                              @Param("idCard") String idCard,
                              @Param("address") String address,
                              @Param("gender") String gender,
                              @Param("remark") String remark,
                              @Param("ageMin") Integer ageMin,
                              @Param("ageMax") Integer ageMax);
}
