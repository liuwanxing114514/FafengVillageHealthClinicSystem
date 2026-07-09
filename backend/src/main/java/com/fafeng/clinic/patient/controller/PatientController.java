package com.fafeng.clinic.patient.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.PatientSearchQuery;
import com.fafeng.clinic.patient.dto.SavePatientRequest;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.patient.vo.PatientDetailVO;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import jakarta.validation.Valid;
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

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/visits")
    public Result<List<VisitListItemVO>> listVisits(@PathVariable Long id) {
        return Result.ok(visitService.listByPatient(id));
    }
}
