package com.jimuqu.common.excel.utils;

import cn.idev.excel.annotation.ExcelProperty;
import com.jimuqu.common.core.exception.ServiceException;
import com.jimuqu.common.excel.annotation.ExcelDictFormat;
import com.jimuqu.common.excel.annotation.ExcelEnumFormat;
import com.jimuqu.common.excel.convert.ExcelDictConvert;
import com.jimuqu.common.excel.convert.ExcelEnumConvert;
import com.jimuqu.common.excel.core.DropDownOptions;
import cn.idev.excel.exception.ExcelAnalysisException;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.noear.solon.validation.annotation.NotBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelUtilContractTest {

    @Test
    void converterExpressionProducesLabelOnlyDropdown() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.<DictRow>of(), "用户数据", DictRow.class, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            List<? extends DataValidation> validations = workbook.getSheetAt(0).getDataValidations();
            assertEquals(1, validations.size());
            assertArrayEquals(new String[]{"正常", "停用"},
                    validations.get(0).getValidationConstraint().getExplicitListValues());
            CellRangeAddress range = validations.get(0).getRegions().getCellRangeAddresses()[0];
            assertEquals(1, range.getFirstRow());
            assertEquals(1000, range.getLastRow());
        }
    }

    @Test
    void converterExpressionsHandleLiteralSeparatorsAndEqualsInLabels() {
        assertEquals("正常.停用", ExcelUtil.convertByExp("0.1", "0=正常,1=停用", "."));
        assertEquals("0.1", ExcelUtil.reverseByExp("正常.停用", "0=正常,1=停用", "."));
        assertEquals("A=B", ExcelUtil.convertByExp("1", "1=A=B", ","));
        assertEquals("", ExcelUtil.convertByExp("1", "", ","));
    }

    @Test
    void linkedOptionNamesMustBeValidExcelNames() {
        assertThrows(ServiceException.class, () -> DropDownOptions.validateOptionValue("1部门"));
        assertThrows(ServiceException.class, () -> DropDownOptions.validateOptionValue("A1"));
        assertThrows(ServiceException.class, () -> DropDownOptions.validateOptionValue("研发-一部"));
        assertEquals("研发_一部", DropDownOptions.createOptionValue("研发", "一部"));
    }

    @Test
    void importValidationReportsTheRowAndValidationMessage() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.of(new RequiredRow("", "保留行")),
                "用户数据", RequiredRow.class, output);

        ExcelAnalysisException exception = assertThrows(ExcelAnalysisException.class,
                () -> ExcelUtil.importExcel(new ByteArrayInputStream(output.toByteArray()), RequiredRow.class, true));

        assertTrue(exception.getMessage().contains("第2行数据校验异常"));
        assertTrue(exception.getMessage().contains("用户账号不能为空"));
    }

    @Test
    void enumConverterRoundTripsAndRejectsUnknownLabels() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.of(new EnumRow("0")), "枚举", EnumRow.class, output);

        RawEnumRow exported = ExcelUtil.importExcel(
                new ByteArrayInputStream(output.toByteArray()), RawEnumRow.class).get(0);
        EnumRow imported = ExcelUtil.importExcel(
                new ByteArrayInputStream(output.toByteArray()), EnumRow.class).get(0);
        assertEquals("正常", exported.getStatus());
        assertEquals("0", imported.getStatus());

        ByteArrayOutputStream invalid = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.of(new RawEnumRow("未知")), "枚举", RawEnumRow.class, invalid);
        assertThrows(ExcelAnalysisException.class,
                () -> ExcelUtil.importExcel(new ByteArrayInputStream(invalid.toByteArray()), EnumRow.class, false));
    }

    @Test
    void bigNumberConverterKeepsLongIdsExactAndAllowsNulls() throws Exception {
        long id = 123456789012345678L;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.exportExcel(List.of(new BigNumberRow(null, "空值"), new BigNumberRow(id, "大数")),
                "大数", BigNumberRow.class, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertEquals(CellType.BLANK, workbook.getSheetAt(0).getRow(1)
                    .getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType());
            assertEquals(Long.toString(id), workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
        }
    }

    private static class DictRow {
        @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
        @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
        private String status;
    }

    public static class RequiredRow {
        @ExcelProperty("用户账号")
        @NotBlank(message = "用户账号不能为空")
        private String userName;

        @ExcelProperty("备注")
        private String remark;

        public RequiredRow() {
        }

        public RequiredRow(String userName, String remark) {
            this.userName = userName;
            this.remark = remark;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class EnumRow {
        @ExcelProperty(value = "状态", converter = ExcelEnumConvert.class)
        @ExcelEnumFormat(enumClass = Status.class)
        private String status;

        public EnumRow() {
        }

        public EnumRow(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class RawEnumRow {
        @ExcelProperty("状态")
        private String status;

        public RawEnumRow() {
        }

        public RawEnumRow(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public enum Status {
        ENABLED("0", "正常"), DISABLED("1", "停用");

        private final String code;
        private final String text;

        Status(String code, String text) {
            this.code = code;
            this.text = text;
        }

        public String getCode() {
            return code;
        }

        public String getText() {
            return text;
        }
    }

    public static class BigNumberRow {
        @ExcelProperty("编号")
        private Long id;

        @ExcelProperty("说明")
        private String label;

        public BigNumberRow() {
        }

        public BigNumberRow(Long id, String label) {
            this.id = id;
            this.label = label;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
