package io.github.ashwithpoojary98.llmops_eval.auth.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.AcceptInvitationRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.AcceptInvitationResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.InvitationDetailsResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/{token}")
    public ResponseEntity<APIResponse<InvitationDetailsResponse>> getInvitationDetails(
            @PathVariable String token
    ) {
        InvitationDetailsResponse details = invitationService.getInvitationDetails(token);

        return ResponseEntity.ok(APIResponse.<InvitationDetailsResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(details)
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<APIResponse<AcceptInvitationResponse>> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request
    ) {
        AcceptInvitationResponse response = invitationService.acceptInvitation(token, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<AcceptInvitationResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Account created successfully. You can now login.")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }
}
