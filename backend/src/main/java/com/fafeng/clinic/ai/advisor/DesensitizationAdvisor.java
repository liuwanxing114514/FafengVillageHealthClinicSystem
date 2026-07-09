package com.fafeng.clinic.ai.advisor;

import com.fafeng.clinic.ai.util.Desensitizer;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 出站 LLM 请求前对用户消息脱敏，作为 {@code ChatClient} 链的第一道拦截。
 */
@Component
public class DesensitizationAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        Prompt prompt = request.prompt();
        List<Message> messages = prompt.getInstructions();
        List<Message> desensitized = new ArrayList<>(messages.size());
        boolean changed = false;
        for (Message message : messages) {
            if (message instanceof UserMessage userMessage) {
                String text = userMessage.getText();
                if (text != null && !text.isBlank()) {
                    String masked = Desensitizer.desensitizeText(text, Desensitizer.PatientContext.empty());
                    if (!text.equals(masked)) {
                        desensitized.add(new UserMessage(masked));
                        changed = true;
                        continue;
                    }
                }
            }
            desensitized.add(message);
        }
        if (!changed) {
            return request;
        }
        return request.mutate().prompt(new Prompt(desensitized)).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "desensitization";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
