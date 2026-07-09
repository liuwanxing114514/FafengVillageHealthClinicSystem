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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * 药品资料、单位换算与条码维护 API。
 */
@Tag(name = "药品", description = "药品建档、换算、条码与扫码匹配")
@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @Operation(summary = "药品列表")
    @GetMapping
    public Result<PageVO<MedicineListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(medicineService.search(keyword, status, page, size));
    }

    @Operation(summary = "按条码查药品", description = "扫码入库/出库匹配")
    @GetMapping("/by-barcode/{code}")
    public Result<MedicineListItemVO> findByBarcode(@PathVariable String code) {
        return Result.ok(medicineService.findByBarcode(code));
    }

    @Operation(summary = "新建药品")
    @PostMapping
    public Result<MedicineDetailVO> create(@Valid @RequestBody SaveMedicineRequest request) {
        return Result.ok(medicineService.create(request));
    }

    @Operation(summary = "药品详情")
    @GetMapping("/{id}")
    public Result<MedicineDetailVO> detail(@PathVariable Long id) {
        return Result.ok(medicineService.getDetail(id));
    }

    @Operation(summary = "更新药品")
    @PutMapping("/{id}")
    public Result<MedicineDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveMedicineRequest request) {
        return Result.ok(medicineService.update(id, request));
    }

    @Operation(summary = "启用/停用药品")
    @PatchMapping("/{id}/status")
    public Result<MedicineDetailVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicineStatusRequest request) {
        return Result.ok(medicineService.updateStatus(id, request));
    }

    @Operation(summary = "软删除药品")
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
