package com.fafeng.clinic.patient.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.SavePatientRequest;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.patient.vo.PatientDetailVO;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final VisitService visitService;

    public PatientController(PatientService patientService, VisitService visitService) {
        this.patientService = patientService;
        this.visitService = visitService;
    }

    @GetMapping
    public Result<PageVO<PatientListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(patientService.search(keyword, page, size));
    }

    @PostMapping
    public Result<PatientDetailVO> create(@Valid @RequestBody SavePatientRequest request) {
        return Result.ok(patientService.create(request));
    }

    @GetMapping("/{id}")
    public Result<PatientDetailVO> detail(@PathVariable Long id) {
        return Result.ok(patientService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<PatientDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePatientRequest request) {
        return Result.ok(patientService.update(id, request));
    }

    @GetMapping("/{id}/visits")
    public Result<List<VisitListItemVO>> listVisits(@PathVariable Long id) {
        return Result.ok(visitService.listByPatient(id));
    }
}
