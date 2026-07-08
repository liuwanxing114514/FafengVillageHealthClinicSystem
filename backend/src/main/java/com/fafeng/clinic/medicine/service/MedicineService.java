package com.fafeng.clinic.medicine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.common.PinyinUtils;
import com.fafeng.clinic.medicine.dto.SaveBarcodeRequest;
import com.fafeng.clinic.medicine.dto.SaveConversionRequest;
import com.fafeng.clinic.medicine.dto.SaveMedicineRequest;
import com.fafeng.clinic.medicine.dto.UpdateMedicineStatusRequest;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineBarcode;
import com.fafeng.clinic.medicine.entity.MedicineUnitConversion;
import com.fafeng.clinic.medicine.mapper.MedicineBarcodeMapper;
import com.fafeng.clinic.medicine.mapper.MedicineMapper;
import com.fafeng.clinic.medicine.mapper.MedicineUnitConversionMapper;
import com.fafeng.clinic.medicine.vo.BarcodeVO;
import com.fafeng.clinic.medicine.vo.ConversionVO;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MedicineService {

    private static final int DEFAULT_BOX_COUNT = 5;

    private final MedicineMapper medicineMapper;
    private final MedicineUnitConversionMapper conversionMapper;
    private final MedicineBarcodeMapper barcodeMapper;
    private final AuditLogService auditLogService;

    public MedicineService(MedicineMapper medicineMapper,
                           MedicineUnitConversionMapper conversionMapper,
                           MedicineBarcodeMapper barcodeMapper,
                           AuditLogService auditLogService) {
        this.medicineMapper = medicineMapper;
        this.conversionMapper = conversionMapper;
        this.barcodeMapper = barcodeMapper;
        this.auditLogService = auditLogService;
    }

    public PageVO<MedicineListItemVO> search(String keyword, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedStatus = status == null ? "" : status.trim();

        var result = medicineMapper.searchPage(
                new Page<>(safePage, safeSize),
                normalizedKeyword.isEmpty() ? null : normalizedKeyword,
                normalizedStatus.isEmpty() ? null : normalizedStatus);

        List<Long> medicineIds = result.getRecords().stream().map(Medicine::getId).toList();
        Map<Long, List<String>> barcodeMap = loadBarcodeTexts(medicineIds);
        Map<Long, List<MedicineUnitConversion>> conversionMap = loadConversions(medicineIds);

        List<MedicineListItemVO> records = result.getRecords().stream()
                .map(m -> toListItem(m, barcodeMap.getOrDefault(m.getId(), List.of()),
                        conversionMap.getOrDefault(m.getId(), List.of())))
                .toList();

        return new PageVO<>(records, result.getTotal(), safePage, safeSize);
    }

    @Transactional
    public MedicineDetailVO create(SaveMedicineRequest request) {
        Medicine medicine = new Medicine();
        applyRequest(medicine, request);
        medicine.setStatus(Medicine.STATUS_ACTIVE);
        medicine.setCreatedAt(OffsetDateTime.now());
        medicine.setUpdatedAt(OffsetDateTime.now());
        medicine.setStockThreshold(resolveStockThreshold(request.stockThreshold(), medicine, List.of()));
        medicineMapper.insert(medicine);

        auditLogService.log("CREATE_MEDICINE", "medicine", medicine.getId(),
                "{\"name\":\"" + escapeJson(medicine.getName()) + "\"}");
        return getDetail(medicine.getId());
    }

    public MedicineDetailVO getDetail(Long id) {
        Medicine medicine = requireMedicine(id);
        List<MedicineUnitConversion> conversions = listConversions(id);
        List<MedicineBarcode> barcodes = listBarcodes(id);
        return toDetail(medicine, conversions, barcodes);
    }

    @Transactional
    public MedicineDetailVO update(Long id, SaveMedicineRequest request) {
        Medicine medicine = requireMedicine(id);
        applyRequest(medicine, request);
        List<MedicineUnitConversion> conversions = listConversions(id);
        medicine.setStockThreshold(resolveStockThreshold(request.stockThreshold(), medicine, conversions));
        medicine.setUpdatedAt(OffsetDateTime.now());
        medicineMapper.updateById(medicine);

        auditLogService.log("UPDATE_MEDICINE", "medicine", medicine.getId(),
                "{\"name\":\"" + escapeJson(medicine.getName()) + "\"}");
        return getDetail(id);
    }

    @Transactional
    public MedicineDetailVO updateStatus(Long id, UpdateMedicineStatusRequest request) {
        Medicine medicine = requireMedicine(id);
        if (!Medicine.STATUS_ACTIVE.equals(request.status())
                && !Medicine.STATUS_INACTIVE.equals(request.status())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值无效");
        }
        medicine.setStatus(request.status());
        medicine.setUpdatedAt(OffsetDateTime.now());
        medicineMapper.updateById(medicine);

        auditLogService.log("UPDATE_MEDICINE_STATUS", "medicine", medicine.getId(),
                "{\"status\":\"" + request.status() + "\"}");
        return getDetail(id);
    }

    public List<ConversionVO> listConversionVOs(Long medicineId) {
        requireMedicine(medicineId);
        return listConversions(medicineId).stream().map(this::toConversionVO).toList();
    }

    @Transactional
    public ConversionVO addConversion(Long medicineId, SaveConversionRequest request) {
        Medicine medicine = requireMedicine(medicineId);
        validateConversionRequest(medicine, request);

        long exists = conversionMapper.selectCount(new LambdaQueryWrapper<MedicineUnitConversion>()
                .eq(MedicineUnitConversion::getMedicineId, medicineId)
                .eq(MedicineUnitConversion::getFromUnit, request.fromUnit())
                .eq(MedicineUnitConversion::getToUnit, request.toUnit()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该单位换算已存在");
        }

        MedicineUnitConversion conversion = new MedicineUnitConversion();
        conversion.setMedicineId(medicineId);
        conversion.setFromUnit(request.fromUnit().trim());
        conversion.setToUnit(request.toUnit().trim());
        conversion.setFactor(request.factor());
        conversion.setCreatedAt(OffsetDateTime.now());
        conversionMapper.insert(conversion);

        auditLogService.log("ADD_MEDICINE_CONVERSION", "medicine", medicineId,
                "{\"from\":\"" + request.fromUnit() + "\",\"to\":\"" + request.toUnit() + "\"}");
        return toConversionVO(conversion);
    }

    @Transactional
    public void deleteConversion(Long medicineId, Long conversionId) {
        requireMedicine(medicineId);
        MedicineUnitConversion conversion = conversionMapper.selectById(conversionId);
        if (conversion == null || !medicineId.equals(conversion.getMedicineId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "换算记录不存在");
        }
        conversionMapper.deleteById(conversionId);
        auditLogService.log("DELETE_MEDICINE_CONVERSION", "medicine", medicineId,
                "{\"conversionId\":" + conversionId + "}");
    }

    public List<BarcodeVO> listBarcodeVOs(Long medicineId) {
        requireMedicine(medicineId);
        return listBarcodes(medicineId).stream().map(this::toBarcodeVO).toList();
    }

    @Transactional
    public BarcodeVO addBarcode(Long medicineId, SaveBarcodeRequest request) {
        requireMedicine(medicineId);
        String barcode = request.barcode().trim();

        if (isBarcodeUsedByNonDeletedMedicine(barcode)) {
            throw new BusinessException(ErrorCode.CONFLICT, "条码已被其他药品使用");
        }

        MedicineBarcode entity = new MedicineBarcode();
        entity.setMedicineId(medicineId);
        entity.setBarcode(barcode);
        entity.setRemark(request.remark() == null ? "" : request.remark().trim());
        entity.setCreatedAt(OffsetDateTime.now());
        barcodeMapper.insert(entity);

        auditLogService.log("ADD_MEDICINE_BARCODE", "medicine", medicineId,
                "{\"barcode\":\"" + escapeJson(barcode) + "\"}");
        return toBarcodeVO(entity);
    }

    @Transactional
    public void deleteBarcode(Long medicineId, Long barcodeId) {
        requireMedicine(medicineId);
        MedicineBarcode barcode = barcodeMapper.selectById(barcodeId);
        if (barcode == null || !medicineId.equals(barcode.getMedicineId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "条码记录不存在");
        }
        barcodeMapper.deleteById(barcodeId);
        auditLogService.log("DELETE_MEDICINE_BARCODE", "medicine", medicineId,
                "{\"barcodeId\":" + barcodeId + "}");
    }

    @Transactional
    public void delete(Long id) {
        Medicine medicine = requireMedicine(id);
        if (!Medicine.STATUS_INACTIVE.equals(medicine.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请先停用药品后再删除");
        }
        medicine.setStatus(Medicine.STATUS_DELETED);
        medicine.setUpdatedAt(OffsetDateTime.now());
        medicineMapper.updateById(medicine);
        barcodeMapper.deleteByMedicineId(id);

        auditLogService.log("DELETE_MEDICINE", "medicine", medicine.getId(),
                "{\"name\":\"" + escapeJson(medicine.getName()) + "\"}");
    }

    private boolean isBarcodeUsedByNonDeletedMedicine(String barcode) {
        List<MedicineBarcode> matches = barcodeMapper.selectList(new LambdaQueryWrapper<MedicineBarcode>()
                .eq(MedicineBarcode::getBarcode, barcode));
        for (MedicineBarcode match : matches) {
            Medicine owner = medicineMapper.selectById(match.getMedicineId());
            if (owner != null && !Medicine.STATUS_DELETED.equals(owner.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private Medicine requireMedicine(Long id) {
        Medicine medicine = medicineMapper.selectById(id);
        if (medicine == null || Medicine.STATUS_DELETED.equals(medicine.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "药品不存在");
        }
        return medicine;
    }

    private void applyRequest(Medicine medicine, SaveMedicineRequest request) {
        medicine.setName(request.name().trim());
        medicine.setGenericName(nullToEmpty(request.genericName()));
        medicine.setDosageForm(nullToEmpty(request.dosageForm()));
        medicine.setSpecification(nullToEmpty(request.specification()));
        medicine.setBaseUnit(request.baseUnit().trim());
        medicine.setPackageUnit(nullToEmpty(request.packageUnit()).isEmpty()
                ? request.baseUnit().trim()
                : request.packageUnit().trim());
        medicine.setManufacturer(nullToEmpty(request.manufacturer()));
        medicine.setPurchasePrice(request.purchasePrice());
        medicine.setRemark(request.remark() == null ? "" : request.remark().trim());
        medicine.setPinyinAbbr(PinyinUtils.toAbbr(medicine.getName()));
    }

    private BigDecimal resolveStockThreshold(BigDecimal requested,
                                             Medicine medicine,
                                             List<MedicineUnitConversion> conversions) {
        if (requested != null) {
            return requested;
        }
        int packageFactor = packageToBaseFactor(medicine, conversions);
        return BigDecimal.valueOf((long) DEFAULT_BOX_COUNT * packageFactor);
    }

    private int packageToBaseFactor(Medicine medicine, List<MedicineUnitConversion> conversions) {
        String packageUnit = medicine.getPackageUnit();
        String baseUnit = medicine.getBaseUnit();
        if (packageUnit == null || packageUnit.isBlank() || packageUnit.equals(baseUnit)) {
            return 1;
        }
        for (MedicineUnitConversion c : conversions) {
            if (packageUnit.equals(c.getFromUnit()) && baseUnit.equals(c.getToUnit())) {
                return c.getFactor();
            }
            if (baseUnit.equals(c.getFromUnit()) && packageUnit.equals(c.getToUnit())) {
                return c.getFactor();
            }
        }
        return 1;
    }

    private BigDecimal toPackagesDisplay(BigDecimal stockThreshold,
                                         Medicine medicine,
                                         List<MedicineUnitConversion> conversions) {
        int factor = packageToBaseFactor(medicine, conversions);
        if (factor <= 0) {
            return stockThreshold;
        }
        return stockThreshold.divide(BigDecimal.valueOf(factor), 2, RoundingMode.HALF_UP);
    }

    private List<MedicineUnitConversion> listConversions(Long medicineId) {
        return conversionMapper.selectList(new LambdaQueryWrapper<MedicineUnitConversion>()
                .eq(MedicineUnitConversion::getMedicineId, medicineId)
                .orderByAsc(MedicineUnitConversion::getId));
    }

    private List<MedicineBarcode> listBarcodes(Long medicineId) {
        return barcodeMapper.selectList(new LambdaQueryWrapper<MedicineBarcode>()
                .eq(MedicineBarcode::getMedicineId, medicineId)
                .orderByAsc(MedicineBarcode::getId));
    }

    private Map<Long, List<String>> loadBarcodeTexts(List<Long> medicineIds) {
        if (medicineIds.isEmpty()) {
            return Map.of();
        }
        return barcodeMapper.selectList(new LambdaQueryWrapper<MedicineBarcode>()
                        .in(MedicineBarcode::getMedicineId, medicineIds))
                .stream()
                .collect(Collectors.groupingBy(
                        MedicineBarcode::getMedicineId,
                        Collectors.mapping(MedicineBarcode::getBarcode, Collectors.toList())));
    }

    private Map<Long, List<MedicineUnitConversion>> loadConversions(List<Long> medicineIds) {
        if (medicineIds.isEmpty()) {
            return Map.of();
        }
        return conversionMapper.selectList(new LambdaQueryWrapper<MedicineUnitConversion>()
                        .in(MedicineUnitConversion::getMedicineId, medicineIds))
                .stream()
                .collect(Collectors.groupingBy(MedicineUnitConversion::getMedicineId));
    }

    private void validateConversionRequest(Medicine medicine, SaveConversionRequest request) {
        if (request.fromUnit().equals(request.toUnit())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "换算单位不能相同");
        }
        String baseUnit = medicine.getBaseUnit();
        String packageUnit = medicine.getPackageUnit();
        boolean touchesBase = request.fromUnit().equals(baseUnit) || request.toUnit().equals(baseUnit);
        boolean touchesPackage = request.fromUnit().equals(packageUnit) || request.toUnit().equals(packageUnit);
        if (!touchesBase && !touchesPackage) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "换算单位须包含基本单位或包装单位");
        }
    }

    private MedicineListItemVO toListItem(Medicine medicine,
                                          List<String> barcodes,
                                          List<MedicineUnitConversion> conversions) {
        return new MedicineListItemVO(
                medicine.getId(),
                medicine.getName(),
                medicine.getGenericName(),
                medicine.getDosageForm(),
                medicine.getSpecification(),
                medicine.getBaseUnit(),
                medicine.getPackageUnit(),
                medicine.getManufacturer(),
                medicine.getPurchasePrice(),
                medicine.getStockThreshold(),
                toPackagesDisplay(medicine.getStockThreshold(), medicine, conversions),
                medicine.getStatus(),
                barcodes
        );
    }

    private MedicineDetailVO toDetail(Medicine medicine,
                                    List<MedicineUnitConversion> conversions,
                                    List<MedicineBarcode> barcodes) {
        return new MedicineDetailVO(
                medicine.getId(),
                medicine.getName(),
                medicine.getGenericName(),
                medicine.getDosageForm(),
                medicine.getSpecification(),
                medicine.getBaseUnit(),
                medicine.getPackageUnit(),
                medicine.getManufacturer(),
                medicine.getPurchasePrice(),
                medicine.getStockThreshold(),
                toPackagesDisplay(medicine.getStockThreshold(), medicine, conversions),
                medicine.getPinyinAbbr(),
                medicine.getRemark(),
                medicine.getStatus(),
                medicine.getCreatedAt(),
                medicine.getUpdatedAt(),
                conversions.stream().map(this::toConversionVO).toList(),
                barcodes.stream().map(this::toBarcodeVO).toList()
        );
    }

    private ConversionVO toConversionVO(MedicineUnitConversion conversion) {
        return new ConversionVO(
                conversion.getId(),
                conversion.getFromUnit(),
                conversion.getToUnit(),
                conversion.getFactor()
        );
    }

    private BarcodeVO toBarcodeVO(MedicineBarcode barcode) {
        return new BarcodeVO(barcode.getId(), barcode.getBarcode(), barcode.getRemark());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
