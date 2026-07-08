package com.fafeng.clinic.clinic.vo;

import java.time.LocalDate;
import java.util.List;

public record PrescriptionPrintVO(
        String clinicName,
        String title,
        String patientName,
        String gender,
        Integer age,
        String address,
        String phone,
        String diagnosis,
        LocalDate prescriptionDate,
        List<PrescriptionItemVO> items,
        String doctorSignatureLabel
) {
}
