package com.fafeng.clinic.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.inventory.entity.InventoryBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryBatchMapper extends BaseMapper<InventoryBatch> {

    @Select("""
            SELECT *
            FROM inventory_batch
            WHERE medicine_id = #{medicineId}
              AND status = 'ACTIVE'
              AND quantity > 0
            ORDER BY expiry_date ASC NULLS LAST, id ASC
            """)
    List<InventoryBatch> listAvailableForFefo(@Param("medicineId") Long medicineId);
}
