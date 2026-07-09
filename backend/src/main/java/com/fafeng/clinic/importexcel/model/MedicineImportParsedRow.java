
package com.fafeng.clinic.importexcel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MedicineImportParsedRow {

    private int rowNumber;
    private String name = "";
    private String genericName = "";
    private String dosageForm = "";
    private String specification = "";
    private String baseUnit = "";
    private String packageUnit = "";
    private String conversionText = "";
    private Integer conversionFactor;
    private String manufacturer = "";
    private String barcode = "";
    private BigDecimal purchasePrice;
    private BigDecimal suggestedRetailPrice;
    private BigDecimal stockThreshold;
    private String batchNo = "";
    private LocalDate expiryDate;
    private BigDecimal initialStock;
    private String remark = "";
    private boolean expiryProvided;
    private final List<String> errors = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
