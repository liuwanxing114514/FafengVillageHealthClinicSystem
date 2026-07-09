package com.fafeng.clinic.importexcel;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.importexcel.dto.MedicineImportConfirmRequest;
import com.fafeng.clinic.importexcel.vo.MedicineImportConfirmResultVO;
import com.fafeng.clinic.importexcel.vo.MedicineImportPreviewVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import/medicine")
public class MedicineImportController {

    private final MedicineImportService importService;

    public MedicineImportController(MedicineImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] body = importService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medicine-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @PostMapping("/preview")
    public Result<MedicineImportPreviewVO> preview(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.preview(file));
    }

    @PostMapping("/confirm")
    public Result<MedicineImportConfirmResultVO> confirm(@Valid @RequestBody MedicineImportConfirmRequest request) {
        return Result.ok(importService.confirm(request));
    }
}
