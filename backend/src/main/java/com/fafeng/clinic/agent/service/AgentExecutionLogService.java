package com.fafeng.clinic.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.agent.entity.AgentExecutionLog;
import com.fafeng.clinic.agent.mapper.AgentExecutionLogMapper;
import com.fafeng.clinic.agent.vo.AgentExecutionLogVO;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AgentExecutionLogService {

    private final AgentExecutionLogMapper logMapper;

    public AgentExecutionLogService(AgentExecutionLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void log(String sessionId, String toolName, String argsSummary, String resultSummary, long durationMs) {
        AgentExecutionLog log = new AgentExecutionLog();
        log.setSessionId(sessionId);
        log.setToolName(toolName);
        log.setArgsSummary(truncate(argsSummary, 2000));
        log.setResultSummary(truncate(resultSummary, 4000));
        log.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
        log.setCreatedAt(OffsetDateTime.now());
        logMapper.insert(log);
    }

    public List<AgentExecutionLogVO> listBySession(String sessionId) {
        return logMapper.selectList(new LambdaQueryWrapper<AgentExecutionLog>()
                        .eq(AgentExecutionLog::getSessionId, sessionId)
                        .orderByAsc(AgentExecutionLog::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public List<AgentExecutionLogVO> listRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return logMapper.selectList(new LambdaQueryWrapper<AgentExecutionLog>()
                        .orderByDesc(AgentExecutionLog::getCreatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private AgentExecutionLogVO toVO(AgentExecutionLog log) {
        return new AgentExecutionLogVO(
                log.getId(),
                log.getSessionId(),
                log.getToolName(),
                log.getArgsSummary(),
                log.getResultSummary(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
