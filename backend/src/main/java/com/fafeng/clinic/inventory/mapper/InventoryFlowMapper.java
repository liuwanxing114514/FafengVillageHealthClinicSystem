package com.fafeng.clinic.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.inventory.entity.InventoryFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryFlowMapper extends BaseMapper<InventoryFlow> {

    @Select("""
            <script>
            SELECT *
            FROM inventory_flow
            <where>
              <if test="medicineId != null">
                AND medicine_id = #{medicineId}
              </if>
              <if test="flowType != null and flowType != ''">
                AND flow_type = #{flowType}
              </if>
            </where>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    IPage<InventoryFlow> searchPage(Page<InventoryFlow> page,
                                    @Param("medicineId") Long medicineId,
                                    @Param("flowType") String flowType);
}
