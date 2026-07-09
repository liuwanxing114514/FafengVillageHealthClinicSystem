package com.fafeng.clinic.inventory.util;

import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineUnitConversion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryUnitConverterTest {

    @Test
    void convertsPackageToBase() {
        Medicine medicine = new Medicine();
        medicine.setName("测试药");
        medicine.setBaseUnit("粒");
        medicine.setPackageUnit("盒");

        MedicineUnitConversion conversion = new MedicineUnitConversion();
        conversion.setFromUnit("盒");
        conversion.setToUnit("粒");
        conversion.setFactor(12);

        BigDecimal result = InventoryUnitConverter.toBaseQuantity(
                medicine, List.of(conversion), BigDecimal.valueOf(2), "盒");
        assertEquals(0, result.compareTo(BigDecimal.valueOf(24)));
    }
}
