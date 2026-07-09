package com.fafeng.clinic.ai.model;

import java.util.ArrayList;
import java.util.List;

public class InboundDraftPayload {

    private String supplier = "";
    private String remark = "";
    private String imagePath = "";
    private String ocrText = "";
    private List<InboundDraftLine> lines = new ArrayList<>();

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public List<InboundDraftLine> getLines() {
        return lines;
    }

    public void setLines(List<InboundDraftLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }
}
