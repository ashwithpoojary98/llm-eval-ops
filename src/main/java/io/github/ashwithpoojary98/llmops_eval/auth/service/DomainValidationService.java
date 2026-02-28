package io.github.ashwithpoojary98.llmops_eval.auth.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.DomainNotAllowedException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ValidationException;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainValidationService {

    private final OrganizationRepository organizationRepository;

    public void validateEmailDomain(String email, UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ValidationException("Organization not found"));

        validateEmailDomain(email, org);
    }

    public void validateEmailDomain(String email, Organization org) {
        String domain = extractDomain(email);

        List<String> allowedDomains = org.getAllowedDomains();
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            log.warn("No allowed domains configured for organization: {}", org.getSlug());
            throw new ValidationException("No allowed domains configured for organization");
        }

        boolean isAllowed = allowedDomains.stream()
                .anyMatch(allowed -> matchesDomain(domain, allowed));

        if (!isAllowed) {
            log.warn("Email domain not allowed: {} for org: {}", domain, org.getSlug());
            throw new DomainNotAllowedException(
                    "Email domain '" + domain + "' is not allowed. Allowed domains: " +
                            String.join(", ", allowedDomains)
            );
        }
    }

    public String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Invalid email format");
        }
        return email.substring(email.indexOf("@") + 1).toLowerCase().trim();
    }

    private boolean matchesDomain(String emailDomain, String allowedDomain) {
        String normalizedAllowed = allowedDomain.toLowerCase().trim();

        // Remove leading @ if present
        if (normalizedAllowed.startsWith("@")) {
            normalizedAllowed = normalizedAllowed.substring(1);
        }

        // Exact match
        if (emailDomain.equals(normalizedAllowed)) {
            return true;
        }

        // Wildcard subdomain match (e.g., *.company.com)
        if (normalizedAllowed.startsWith("*.")) {
            String baseDomain = normalizedAllowed.substring(2);
            return emailDomain.equals(baseDomain) ||
                    emailDomain.endsWith("." + baseDomain);
        }

        return false;
    }
}
