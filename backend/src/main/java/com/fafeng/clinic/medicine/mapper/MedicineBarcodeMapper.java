package com.fafeng.clinic.medicine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fafeng.clinic.medicine.entity.MedicineBarcode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MedicineBarcodeMapper extends BaseMapper<MedicineBarcode> {

    @Delete("DELETE FROM medicine_barcode WHERE medicine_id = #{medicineId}")
    int deleteByMedicineId(@Param("medicineId") Long medicineId);
}
