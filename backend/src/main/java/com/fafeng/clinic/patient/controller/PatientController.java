package com.fafeng.clinic.patient.controller;

import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.PatientSearchQuery;
import com.fafeng.clinic.patient.dto.SavePatientRequest;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.patient.vo.PatientDetailVO;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者资料 CRUD 与历史病历查询 API。
 */
@Tag(name = "患者", description = "患者建档、搜索与欠款统计")
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final VisitService visitService;

    @Operation(summary = "患者列表")
    @GetMapping
    public Result<PageVO<PatientListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PatientSearchQuery query = new PatientSearchQuery(
                keyword, name, phone, idCard, address, gender, remark, ageMin, ageMax);
        return Result.ok(patientService.search(query, page, size));
    }

    @Operation(summary = "新建患者")
    @PostMapping
    public Result<PatientDetailVO> create(@Valid @RequestBody SavePatientRequest request) {
        return Result.ok(patientService.create(request));
    }

    @Operation(summary = "患者详情", description = "含累计欠款")
    @GetMapping("/{id}")
    public Result<PatientDetailVO> detail(@PathVariable Long id) {
        return Result.ok(patientService.getDetail(id));
    }

    @Operation(summary = "更新患者")
    @PutMapping("/{id}")
    public Result<PatientDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePatientRequest request) {
        return Result.ok(patientService.update(id, request));
    }

    @Operation(summary = "软删除患者")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "患者历史病历")
    @GetMapping("/{id}/visits")
    public Result<List<VisitListItemVO>> listVisits(@PathVariable Long id) {
        return Result.ok(visitService.listByPatient(id));
    }
}
