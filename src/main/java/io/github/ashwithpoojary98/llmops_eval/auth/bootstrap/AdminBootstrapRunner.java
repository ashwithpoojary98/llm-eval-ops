package io.github.ashwithpoojary98.llmops_eval.auth.bootstrap;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.OrganizationRepository;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    @Value("${llmops.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${llmops.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${llmops.bootstrap.org-name:Default Organization}")
    private String orgName;

    @Value("${llmops.bootstrap.allowed-domains:}")
    private String allowedDomainsStr;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("No bootstrap admin configured. Skipping admin creation.");
            return;
        }

        // Check if any admin already exists
        if (userRepository.existsByRole(LLMOPsRole.ADMIN)) {
            log.info("Admin user already exists. Skipping bootstrap.");
            return;
        }

        // Parse allowed domains
        List<String> allowedDomains = List.of();
        if (allowedDomainsStr != null && !allowedDomainsStr.isBlank()) {
            allowedDomains = Arrays.stream(allowedDomainsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        // If no domains specified, extract from admin email
        if (allowedDomains.isEmpty() && adminEmail.contains("@")) {
            String domain = adminEmail.substring(adminEmail.indexOf("@") + 1);
            allowedDomains = List.of(domain);
        }

        // Create or get organization
        String slug = generateSlug(orgName);
        List<String> finalAllowedDomains = allowedDomains;
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseGet(() -> {
                    Organization newOrg = Organization.builder()
                            .name(orgName)
                            .slug(slug)
                            .allowedDomains(finalAllowedDomains)
                            .build();
                    log.info("Creating organization: {} with allowed domains: {}", orgName, finalAllowedDomains);
                    return organizationRepository.save(newOrg);
                });

        // Create admin user
        User admin = User.builder()
                .email(adminEmail.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("System Administrator")
                .role(LLMOPsRole.ADMIN)
                .organization(organization)
                .emailVerified(true)
                .isActive(true)
                .mustChangePassword(true)
                .build();

        userRepository.save(admin);

        log.info("===========================================");
        log.info("Bootstrap admin created successfully!");
        log.info("Email: {}", adminEmail);
        log.info("Organization: {}", orgName);
        log.info("Allowed domains: {}", allowedDomains);
        log.info("NOTE: Please change the password on first login.");
        log.info("===========================================");
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
