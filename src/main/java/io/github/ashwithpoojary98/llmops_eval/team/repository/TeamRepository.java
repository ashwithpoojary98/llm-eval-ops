package io.github.ashwithpoojary98.llmops_eval.team.repository;

import io.github.ashwithpoojary98.llmops_eval.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByName(String name);

    Optional<Team> findByName(String name);

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.members WHERE t.id = :id")
    Optional<Team> findByIdWithMembers(@Param("id") UUID id);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.user.id = :userId")
    List<Team> findByMemberUserId(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT t FROM Team t JOIN t.members m WHERE m.user.id = :userId")
    Page<Team> findByMemberUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT t FROM Team t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Team> searchByNameOrDescription(@Param("search") String search, Pageable pageable);
}
