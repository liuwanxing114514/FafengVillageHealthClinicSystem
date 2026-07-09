package com.fafeng.clinic.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.medicine.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {

    @Select("""
            <script>
            SELECT DISTINCT m.*
            FROM medicine m
            LEFT JOIN medicine_barcode b ON b.medicine_id = m.id
            <where>
              <choose>
                <when test="status != null and status != ''">
                  AND m.status = #{status}
                </when>
                <otherwise>
                  AND m.status != 'DELETED'
                </otherwise>
              </choose>
              <if test="keyword != null and keyword != ''">
                AND (
                  m.name ILIKE CONCAT('%', #{keyword}, '%')
                  OR m.pinyin_abbr ILIKE CONCAT('%', #{keyword}, '%')
                  OR b.barcode = #{keyword}
                  OR b.barcode ILIKE CONCAT(#{keyword}, '%')
                )
              </if>
            </where>
            ORDER BY m.name ASC, m.specification ASC
            </script>
            """)
    IPage<Medicine> searchPage(Page<Medicine> page,
                               @Param("keyword") String keyword,
                               @Param("status") String status);
}
