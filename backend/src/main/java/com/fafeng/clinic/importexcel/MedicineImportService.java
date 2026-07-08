package com.fafeng.clinic.importexcel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.importexcel.dto.MedicineImportConfirmRequest;
import com.fafeng.clinic.importexcel.model.MedicineImportParsedRow;
import com.fafeng.clinic.importexcel.vo.MedicineImportConfirmResultVO;
import com.fafeng.clinic.importexcel.vo.MedicineImportPreviewVO;
import com.fafeng.clinic.importexcel.vo.MedicineImportRowVO;
import com.fafeng.clinic.inventory.dto.InboundRequest;
import com.fafeng.clinic.inventory.service.InventoryService;
import com.fafeng.clinic.medicine.dto.SaveBarcodeRequest;
import com.fafeng.clinic.medicine.dto.SaveConversionRequest;
import com.fafeng.clinic.medicine.dto.SaveMedicineRequest;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.entity.MedicineBarcode;
import com.fafeng.clinic.medicine.mapper.MedicineBarcodeMapper;
import com.fafeng.clinic.medicine.mapper.MedicineMapper;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MedicineImportService {

    private final MedicineImportExcelParser parser;
    private final MedicineImportPreviewCache previewCache;
    private final MedicineService medicineService;
    private final InventoryService inventoryService;
    private final MedicineMapper medicineMapper;
    private final MedicineBarcodeMapper barcodeMapper;

    public MedicineImportService(MedicineImportExcelParser parser,
                                 MedicineImportPreviewCache previewCache,
                                 MedicineService medicineService,
                                 InventoryService inventoryService,
                                 MedicineMapper medicineMapper,
                                 MedicineBarcodeMapper barcodeMapper) {
        this.parser = parser;
        this.previewCache = previewCache;
        this.medicineService = medicineService;
        this.inventoryService = inventoryService;
        this.medicineMapper = medicineMapper;
        this.barcodeMapper = barcodeMapper;
    }

    public byte[] buildTemplate() {
        return parser.buildTemplateWorkbook();
    }

    public MedicineImportPreviewVO preview(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 .xlsx 格式");
        }

        List<MedicineImportParsedRow> rows;
        try {
            rows = parser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取文件失败");
        }

        validateRows(rows);
        String previewId = previewCache.put(rows);

        int validCount = (int) rows.stream().filter(MedicineImportParsedRow::isValid).count();
        int errorCount = rows.size() - validCount;
        return new MedicineImportPreviewVO(
                previewId,
                rows.size(),
                validCount,
                errorCount,
                errorCount == 0 && validCount > 0,
                rows.stream().map(this::toRowVO).toList()
        );
    }

    @Transactional
    public MedicineImportConfirmResultVO confirm(MedicineImportConfirmRequest request) {
        List<MedicineImportParsedRow> rows = previewCache.take(request.previewId());
        if (rows == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预览已过期或已确认，请重新上传");
        }

        validateRows(rows);
        long invalidCount = rows.stream().filter(row -> !row.isValid()).count();
        if (invalidCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "存在错误行，请修正后重新上传");
        }

        int medicineCount = 0;
        int inventoryCount = 0;
        for (MedicineImportParsedRow row : rows) {
            MedicineDetailVO medicine = medicineService.create(toSaveRequest(row));
            medicineCount++;

            if (row.getConversionFactor() != null && row.getConversionFactor() > 1) {
                String packageUnit = resolvePackageUnit(row);
                if (!packageUnit.equals(row.getBaseUnit())) {
                    medicineService.addConversion(medicine.id(), new SaveConversionRequest(
                            packageUnit,
                            row.getBaseUnit(),
                            row.getConversionFactor()
                    ));
                }
            }

            if (!row.getBarcode().isBlank()) {
                medicineService.addBarcode(medicine.id(), new SaveBarcodeRequest(row.getBarcode(), "Excel导入"));
            }

            if (row.getInitialStock() != null && row.getInitialStock().compareTo(BigDecimal.ZERO) > 0) {
                inventoryService.inbound(new InboundRequest(
                        medicine.id(),
                        row.getInitialStock(),
                        row.getBaseUnit(),
                        row.getBatchNo(),
                        row.getExpiryDate(),
                        row.getPurchasePrice(),
                        null,
                        row.getRemark().isBlank() ? "Excel导入初始库存" : row.getRemark()
                ));
                inventoryCount++;
            }
        }

        return new MedicineImportConfirmResultVO(medicineCount, inventoryCount);
    }

    private void validateRows(List<MedicineImportParsedRow> rows) {
        Set<String> names = new HashSet<>();
        Set<String> barcodes = new HashSet<>();

        for (MedicineImportParsedRow row : rows) {
            row.getErrors().clear();

            if (row.getName().isBlank()) {
                row.addError("药品名称不能为空");
            } else {
                String nameKey = row.getName().trim();
                if (!names.add(nameKey)) {
                    row.addError("导入文件中存在重复药品名称");
                }
                if (existsMedicineByName(nameKey)) {
                    row.addError("药品名称已存在：" + nameKey);
                }
            }

            if (row.getBaseUnit().isBlank()) {
                row.addError("基本单位不能为空");
            }

            if (row.getPurchasePrice() == null) {
                row.addError("进货单价不能为空或格式无效");
            } else if (row.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
                row.addError("进货单价不能为负数");
            }

            if (row.getStockThreshold() != null && row.getStockThreshold().compareTo(BigDecimal.ZERO) < 0) {
                row.addError("库存下限不能为负数");
            }

            if (!row.getConversionText().isBlank() && row.getConversionFactor() == null) {
                row.addError("换算关系格式无效，示例：1盒=24粒 或 24");
            }

            if (row.getConversionFactor() != null) {
                if (row.getConversionFactor() <= 0) {
                    row.addError("换算系数必须大于 0");
                }
                String packageUnit = resolvePackageUnit(row);
                if (packageUnit.equals(row.getBaseUnit())) {
                    row.addError("换算关系需要包装单位与基本单位不同");
                }
            }

            if (!row.getBarcode().isBlank()) {
                String barcode = row.getBarcode().trim();
                if (!barcodes.add(barcode)) {
                    row.addError("导入文件中存在重复条码");
                }
                if (isBarcodeUsed(barcode)) {
                    row.addError("条码已被其他药品使用");
                }
            }

            BigDecimal initialStock = row.getInitialStock() == null ? BigDecimal.ZERO : row.getInitialStock();
            if (initialStock.compareTo(BigDecimal.ZERO) > 0 && row.getBatchNo().isBlank()) {
                row.addError("填写初始库存时必须提供批号");
            }
            if (initialStock.compareTo(BigDecimal.ZERO) < 0) {
                row.addError("初始库存数量不能为负数");
            }

            if (row.isExpiryProvided() && row.getExpiryDate() == null) {
                row.addError("有效期格式无效，请使用 yyyy-MM-dd");
            }
        }
    }

    private String resolvePackageUnit(MedicineImportParsedRow row) {
        return row.getPackageUnit().isBlank() ? row.getBaseUnit() : row.getPackageUnit();
    }

    private SaveMedicineRequest toSaveRequest(MedicineImportParsedRow row) {
        return new SaveMedicineRequest(
                row.getName(),
                row.getGenericName(),
                row.getDosageForm(),
                row.getSpecification(),
                row.getBaseUnit(),
                row.getPackageUnit(),
                row.getManufacturer(),
                row.getPurchasePrice(),
                row.getStockThreshold(),
                row.getRemark()
        );
    }

    private MedicineImportRowVO toRowVO(MedicineImportParsedRow row) {
        return new MedicineImportRowVO(
                row.getRowNumber(),
                row.isValid(),
                List.copyOf(row.getErrors()),
                row.getName(),
                row.getGenericName(),
                row.getDosageForm(),
                row.getSpecification(),
                row.getBaseUnit(),
                row.getPackageUnit(),
                row.getConversionText(),
                row.getManufacturer(),
                row.getBarcode(),
                row.getPurchasePrice(),
                row.getStockThreshold(),
                row.getBatchNo(),
                row.getExpiryDate(),
                row.getInitialStock(),
                row.getRemark()
        );
    }

    private boolean existsMedicineByName(String name) {
        return medicineMapper.selectCount(new LambdaQueryWrapper<Medicine>()
                .eq(Medicine::getName, name)
                .ne(Medicine::getStatus, Medicine.STATUS_DELETED)) > 0;
    }

    private boolean isBarcodeUsed(String barcode) {
        MedicineBarcode match = barcodeMapper.selectOne(new LambdaQueryWrapper<MedicineBarcode>()
                .eq(MedicineBarcode::getBarcode, barcode));
        if (match == null) {
            return false;
        }
        Medicine owner = medicineMapper.selectById(match.getMedicineId());
        return owner != null && !Medicine.STATUS_DELETED.equals(owner.getStatus());
    }
}
