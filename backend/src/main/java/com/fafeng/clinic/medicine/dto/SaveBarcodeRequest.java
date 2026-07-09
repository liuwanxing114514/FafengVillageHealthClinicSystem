package com.fafeng.clinic.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveBarcodeRequest(
        @NotBlank @Size(max = 32) String barcode,
        @Size(max = 128) String remark
) {
}
