package com.fafeng.clinic.inventory.util;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineUnitConversion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class InventoryUnitConverter {

    private InventoryUnitConverter() {
    }

    public static BigDecimal toBaseQuantity(Medicine medicine,
                                            List<MedicineUnitConversion> conversions,
                                            BigDecimal quantity,
                                            String unit) {
        String normalizedUnit = unit == null ? "" : unit.trim();
        if (normalizedUnit.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单位不能为空");
        }
        if (normalizedUnit.equals(medicine.getBaseUnit())) {
            return quantity;
        }
        for (MedicineUnitConversion conversion : conversions) {
            if (normalizedUnit.equals(conversion.getFromUnit())
                    && medicine.getBaseUnit().equals(conversion.getToUnit())) {
                return quantity.multiply(BigDecimal.valueOf(conversion.getFactor()));
            }
            if (normalizedUnit.equals(conversion.getToUnit())
                    && medicine.getBaseUnit().equals(conversion.getFromUnit())) {
                return quantity.divide(BigDecimal.valueOf(conversion.getFactor()), 3, RoundingMode.HALF_UP);
            }
        }
        String packageUnit = medicine.getPackageUnit();
        if (packageUnit != null && !packageUnit.isBlank() && normalizedUnit.equals(packageUnit)) {
            int factor = packageToBaseFactor(medicine, conversions);
            return quantity.multiply(BigDecimal.valueOf(factor));
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "单位「" + normalizedUnit + "」与药品「" + medicine.getName() + "」不匹配");
    }

    private static int packageToBaseFactor(Medicine medicine, List<MedicineUnitConversion> conversions) {
        String packageUnit = medicine.getPackageUnit();
        String baseUnit = medicine.getBaseUnit();
        if (packageUnit == null || packageUnit.isBlank() || packageUnit.equals(baseUnit)) {
            return 1;
        }
        for (MedicineUnitConversion conversion : conversions) {
            if (packageUnit.equals(conversion.getFromUnit()) && baseUnit.equals(conversion.getToUnit())) {
                return conversion.getFactor();
            }
            if (baseUnit.equals(conversion.getFromUnit()) && packageUnit.equals(conversion.getToUnit())) {
                return conversion.getFactor();
            }
        }
        return 1;
    }
}
