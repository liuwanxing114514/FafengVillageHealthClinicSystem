package com.fafeng.clinic.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.clinic.entity.Prescription;
import com.fafeng.clinic.clinic.entity.PrescriptionItem;
import com.fafeng.clinic.clinic.mapper.PrescriptionItemMapper;
import com.fafeng.clinic.clinic.mapper.PrescriptionMapper;
import com.fafeng.clinic.clinic.vo.VisitFeeSummaryVO;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineUnitConversion;
import com.fafeng.clinic.medicine.mapper.MedicineMapper;
import com.fafeng.clinic.medicine.mapper.MedicineUnitConversionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class VisitFeeService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final MedicineMapper medicineMapper;
    private final MedicineUnitConversionMapper conversionMapper;

    public VisitFeeService(PrescriptionMapper prescriptionMapper,
                           PrescriptionItemMapper prescriptionItemMapper,
                           MedicineMapper medicineMapper,
                           MedicineUnitConversionMapper conversionMapper) {
        this.prescriptionMapper = prescriptionMapper;
        this.prescriptionItemMapper = prescriptionItemMapper;
        this.medicineMapper = medicineMapper;
        this.conversionMapper = conversionMapper;
    }

    public VisitFeeSummaryVO summarizeForVisit(Long visitId) {
        List<Prescription> prescriptions = prescriptionMapper.selectList(new LambdaQueryWrapper<Prescription>()
                .eq(Prescription::getVisitId, visitId)
                .ne(Prescription::getStatus, Prescription.STATUS_VOID));
        BigDecimal suggestedDue = BigDecimal.ZERO;
        BigDecimal referenceCost = BigDecimal.ZERO;
        for (Prescription prescription : prescriptions) {
            for (PrescriptionItem item : prescriptionItemMapper.listByPrescriptionId(prescription.getId())) {
                Medicine medicine = medicineMapper.selectById(item.getMedicineId());
                if (medicine == null) {
                    continue;
                }
                BigDecimal baseQty = toBaseQuantity(medicine, item.getUnit(), item.getQuantity());
                BigDecimal retail = nullToZero(medicine.getSuggestedRetailPrice());
                BigDecimal purchase = nullToZero(medicine.getPurchasePrice());
                suggestedDue = suggestedDue.add(retail.multiply(baseQty));
                referenceCost = referenceCost.add(purchase.multiply(baseQty));
            }
        }
        return new VisitFeeSummaryVO(
                scaleMoney(suggestedDue),
                scaleMoney(referenceCost));
    }

    private BigDecimal toBaseQuantity(Medicine medicine, String unit, BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        String normalized = unit == null ? "" : unit.trim();
        if (normalized.isEmpty() || normalized.equals(medicine.getBaseUnit())) {
            return quantity;
        }
        if (normalized.equals(medicine.getPackageUnit())) {
            Integer factor = findPackageFactor(medicine.getId(), medicine.getPackageUnit(), medicine.getBaseUnit());
            if (factor != null && factor > 0) {
                return quantity.multiply(BigDecimal.valueOf(factor));
            }
        }
        return quantity;
    }

    private Integer findPackageFactor(Long medicineId, String fromUnit, String toUnit) {
        if (fromUnit == null || fromUnit.isBlank()) {
            return null;
        }
        List<MedicineUnitConversion> conversions = conversionMapper.selectList(
                new LambdaQueryWrapper<MedicineUnitConversion>()
                        .eq(MedicineUnitConversion::getMedicineId, medicineId)
                        .eq(MedicineUnitConversion::getFromUnit, fromUnit)
                        .eq(MedicineUnitConversion::getToUnit, toUnit));
        if (conversions.isEmpty()) {
            return null;
        }
        return conversions.get(0).getFactor();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
