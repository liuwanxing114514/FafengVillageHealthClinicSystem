package com.fafeng.clinic.clinic.service;

import com.fafeng.clinic.clinic.entity.Prescription;
import com.fafeng.clinic.clinic.entity.PrescriptionItem;
import com.fafeng.clinic.clinic.mapper.PrescriptionItemMapper;
import com.fafeng.clinic.clinic.mapper.PrescriptionMapper;
import com.fafeng.clinic.clinic.vo.VisitFeeSummaryVO;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.mapper.MedicineMapper;
import com.fafeng.clinic.medicine.mapper.MedicineUnitConversionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitFeeServiceTest {

    @Mock
    private PrescriptionMapper prescriptionMapper;
    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;
    @Mock
    private MedicineMapper medicineMapper;
    @Mock
    private MedicineUnitConversionMapper conversionMapper;

    @InjectMocks
    private VisitFeeService visitFeeService;

    @Test
    void summarizeForVisit_multipliesRetailAndPurchaseByQuantity() {
        Prescription prescription = new Prescription();
        prescription.setId(1L);
        prescription.setStatus(Prescription.STATUS_CONFIRMED);

        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineId(10L);
        item.setQuantity(new BigDecimal("2"));
        item.setUnit("盒");

        Medicine medicine = new Medicine();
        medicine.setId(10L);
        medicine.setBaseUnit("粒");
        medicine.setPackageUnit("盒");
        medicine.setSuggestedRetailPrice(new BigDecimal("15.50"));
        medicine.setPurchasePrice(new BigDecimal("8.00"));

        when(prescriptionMapper.selectList(any())).thenReturn(List.of(prescription));
        when(prescriptionItemMapper.listByPrescriptionId(1L)).thenReturn(List.of(item));
        when(medicineMapper.selectById(10L)).thenReturn(medicine);
        when(conversionMapper.selectList(any())).thenReturn(List.of());

        VisitFeeSummaryVO summary = visitFeeService.summarizeForVisit(100L);

        assertEquals(new BigDecimal("31.00"), summary.suggestedAmountDue());
        assertEquals(new BigDecimal("16.00"), summary.referencePurchaseCost());
    }
}
