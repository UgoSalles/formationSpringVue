package platform.common.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import platform.common.annotation.DefaultSort;
import platform.common.dto.SortOrder;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@DefaultSort(field = "id", order = SortOrder.ASC)
public abstract class BaseEntity {

    // uuid_v7 : triable par insertion (comme l'ancien ULID) mais type natif (colonne Postgres `uuid`).
    // Généré côté application, jamais délégué à Postgres (< 18 plafonne à uuid_v5 → perte du v7).
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;
}
