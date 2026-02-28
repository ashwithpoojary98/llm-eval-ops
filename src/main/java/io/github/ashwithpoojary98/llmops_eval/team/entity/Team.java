package io.github.ashwithpoojary98.llmops_eval.team.entity;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void addMember(User user, TeamMemberRole role, User addedBy) {
        TeamMember member = TeamMember.builder()
                .team(this)
                .user(user)
                .role(role)
                .addedBy(addedBy)
                .build();
        members.add(member);
    }

    public void removeMember(User user) {
        members.removeIf(m -> m.getUser().getId().equals(user.getId()));
    }

    public boolean hasMember(UUID userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));
    }

    public int getMemberCount() {
        return members.size();
    }
}
