package com.fafeng.clinic.medicine.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.dto.SaveBarcodeRequest;
import com.fafeng.clinic.medicine.dto.SaveConversionRequest;
import com.fafeng.clinic.medicine.dto.SaveMedicineRequest;
import com.fafeng.clinic.medicine.dto.UpdateMedicineStatusRequest;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.BarcodeVO;
import com.fafeng.clinic.medicine.vo.ConversionVO;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @GetMapping
    public Result<PageVO<MedicineListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(medicineService.search(keyword, status, page, size));
    }

    @GetMapping("/by-barcode/{code}")
    public Result<MedicineListItemVO> findByBarcode(@PathVariable String code) {
        return Result.ok(medicineService.findByBarcode(code));
    }

    @PostMapping
    public Result<MedicineDetailVO> create(@Valid @RequestBody SaveMedicineRequest request) {
        return Result.ok(medicineService.create(request));
    }

    @GetMapping("/{id}")
    public Result<MedicineDetailVO> detail(@PathVariable Long id) {
        return Result.ok(medicineService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<MedicineDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveMedicineRequest request) {
        return Result.ok(medicineService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public Result<MedicineDetailVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicineStatusRequest request) {
        return Result.ok(medicineService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/conversions")
    public Result<List<ConversionVO>> listConversions(@PathVariable Long id) {
        return Result.ok(medicineService.listConversionVOs(id));
    }

    @PostMapping("/{id}/conversions")
    public Result<ConversionVO> addConversion(
            @PathVariable Long id,
            @Valid @RequestBody SaveConversionRequest request) {
        return Result.ok(medicineService.addConversion(id, request));
    }

    @DeleteMapping("/{id}/conversions/{conversionId}")
    public Result<Void> deleteConversion(
            @PathVariable Long id,
            @PathVariable Long conversionId) {
        medicineService.deleteConversion(id, conversionId);
        return Result.ok();
    }

    @GetMapping("/{id}/barcodes")
    public Result<List<BarcodeVO>> listBarcodes(@PathVariable Long id) {
        return Result.ok(medicineService.listBarcodeVOs(id));
    }

    @PostMapping("/{id}/barcodes")
    public Result<BarcodeVO> addBarcode(
            @PathVariable Long id,
            @Valid @RequestBody SaveBarcodeRequest request) {
        return Result.ok(medicineService.addBarcode(id, request));
    }

    @DeleteMapping("/{id}/barcodes/{barcodeId}")
    public Result<Void> deleteBarcode(
            @PathVariable Long id,
            @PathVariable Long barcodeId) {
        medicineService.deleteBarcode(id, barcodeId);
        return Result.ok();
    }
}
