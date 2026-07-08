package com.fafeng.clinic.importexcel.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private BigDecimal stockThreshold;
    private String batchNo = "";
    private LocalDate expiryDate;
    private BigDecimal initialStock;
    private String remark = "";
    private boolean expiryProvided;
    private final List<String> errors = new ArrayList<>();

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public String getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(String packageUnit) {
        this.packageUnit = packageUnit;
    }

    public String getConversionText() {
        return conversionText;
    }

    public void setConversionText(String conversionText) {
        this.conversionText = conversionText;
    }

    public Integer getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(Integer conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getStockThreshold() {
        return stockThreshold;
    }

    public void setStockThreshold(BigDecimal stockThreshold) {
        this.stockThreshold = stockThreshold;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(BigDecimal initialStock) {
        this.initialStock = initialStock;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isExpiryProvided() {
        return expiryProvided;
    }

    public void setExpiryProvided(boolean expiryProvided) {
        this.expiryProvided = expiryProvided;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String message) {
        errors.add(message);
    }
}
