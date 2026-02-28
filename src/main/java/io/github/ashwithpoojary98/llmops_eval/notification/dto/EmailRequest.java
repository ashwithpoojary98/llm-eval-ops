package io.github.ashwithpoojary98.llmops_eval.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    private String to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
    private boolean isHtml;
    private String templateName;
    private Map<String, Object> templateVariables;
}
