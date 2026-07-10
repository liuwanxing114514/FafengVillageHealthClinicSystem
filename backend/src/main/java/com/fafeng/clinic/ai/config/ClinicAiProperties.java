package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.ai")
public class ClinicAiProperties {

    private boolean enabled = false;
    private String provider = "noop";
    private String deepseekApiKey = "";
    private String deepseekBaseUrl = "https://api.deepseek.com";
    private String deepseekModel = "deepseek-chat";
    /** 主通道（如硅基流动）限流时，可选 DeepSeek 官方 API 兜底 */
    private String deepseekFallbackApiKey = "";
    private String deepseekFallbackBaseUrl = "https://api.deepseek.com";
    private String deepseekFallbackModel = "deepseek-chat";
    private String localBaseUrl = "http://localhost:11434";
    private String visitStructurePrompt = """
            你是诊所病历整理助手。根据医生提供的自由文本，提取结构化字段。
            只输出 JSON，不要 markdown 代码块，字段如下：
            chiefComplaint, presentIllness, pastHistory, diagnosis, treatment, remark
            缺失字段用空字符串。不要编造未提及的内容。
            """;
    private String inboundStructurePrompt = """
            你是诊所进货单整理助手。根据 OCR 识别的进货清单/发票/送货单文本，提取入库明细。
            只输出 JSON，不要 markdown 代码块，格式：
            {"supplier":"供应商名称","remark":"备注","lines":[{"medicineName":"药品名","specification":"规格","quantity":"数量","unit":"单位","batchNo":"批号","expiryDate":"YYYY-MM-DD或空","purchasePrice":"单价或空"}]}
            缺失字段用空字符串。不要编造未在原文出现的药品。
            """;
    private int agentMaxRounds = 5;
    private String agentSystemPrompt = """
            你是发凤村卫生室 AI 助手，只能通过受控工具查询诊所数据或生成待确认草稿。
            禁止直接修改库存、病历或执行出库。
            
            规则：
            - 查库存前先 searchMedicine 或提供 medicineName
            - 问临期药品用 queryExpiringMedicine
            - 查患者用 searchPatient；keyword 填姓名/电话/身份证片段，勿把「最近/最新/谁」等当作 keyword
            - 问「最近/最新/最后一位患者」时：searchPatient 不传 keyword，page=1，size=1（结果按更新时间倒序）
            - 问「有多少患者/列出患者」时：searchPatient 不传 keyword，按需设 size
            - 查某患者病历用 searchPatientVisit（先 searchPatient 得 patientId 亦可）
            - 出库请求用 generateOutboundDraft，只生成草稿不扣库存
            - 回答简洁、用中文、包含关键数字；必须依据工具返回，不可臆测
            - 禁止在回复中输出患者完整姓名、电话、身份证号；工具返回的姓名已是脱敏形式，请原样引用
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }

    public void setDeepseekApiKey(String deepseekApiKey) {
        this.deepseekApiKey = deepseekApiKey;
    }

    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }

    public void setDeepseekBaseUrl(String deepseekBaseUrl) {
        this.deepseekBaseUrl = deepseekBaseUrl;
    }

    public String getDeepseekModel() {
        return deepseekModel;
    }

    public void setDeepseekModel(String deepseekModel) {
        this.deepseekModel = deepseekModel;
    }

    public String getDeepseekFallbackApiKey() {
        return deepseekFallbackApiKey;
    }

    public void setDeepseekFallbackApiKey(String deepseekFallbackApiKey) {
        this.deepseekFallbackApiKey = deepseekFallbackApiKey;
    }

    public String getDeepseekFallbackBaseUrl() {
        return deepseekFallbackBaseUrl;
    }

    public void setDeepseekFallbackBaseUrl(String deepseekFallbackBaseUrl) {
        this.deepseekFallbackBaseUrl = deepseekFallbackBaseUrl;
    }

    public String getDeepseekFallbackModel() {
        return deepseekFallbackModel;
    }

    public void setDeepseekFallbackModel(String deepseekFallbackModel) {
        this.deepseekFallbackModel = deepseekFallbackModel;
    }

    public boolean hasDeepseekFallback() {
        return deepseekFallbackApiKey != null && !deepseekFallbackApiKey.isBlank();
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }

    public String getVisitStructurePrompt() {
        return visitStructurePrompt;
    }

    public void setVisitStructurePrompt(String visitStructurePrompt) {
        this.visitStructurePrompt = visitStructurePrompt;
    }

    public String getInboundStructurePrompt() {
        return inboundStructurePrompt;
    }

    public void setInboundStructurePrompt(String inboundStructurePrompt) {
        this.inboundStructurePrompt = inboundStructurePrompt;
    }

    public int getAgentMaxRounds() {
        return agentMaxRounds;
    }

    public void setAgentMaxRounds(int agentMaxRounds) {
        this.agentMaxRounds = agentMaxRounds;
    }

    public String getAgentSystemPrompt() {
        return agentSystemPrompt;
    }

    public void setAgentSystemPrompt(String agentSystemPrompt) {
        this.agentSystemPrompt = agentSystemPrompt;
    }
}
