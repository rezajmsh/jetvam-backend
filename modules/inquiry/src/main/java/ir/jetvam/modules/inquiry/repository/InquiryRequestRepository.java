package ir.jetvam.modules.inquiry.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.inquiry.model.AsyncInquiryStatus;
import ir.jetvam.modules.inquiry.model.InquiryCallbackStatus;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Claims due inquiry and callback work with database locks across clustered job nodes.
 * Callback identity also provides idempotent request submission for consumers.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface InquiryRequestRepository extends JetvamJpaRepository<InquiryRequestEntity, UUID> {

    Optional<InquiryRequestEntity> findByCallbackTransportAndCallbackDestinationAndCallbackCorrelationId(
            String transport,
            String destination,
            String correlationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from InquiryRequestEntity request
            where request.status in :statuses and request.nextAttemptAt <= :now
            order by request.nextAttemptAt, request.createdAt
            """)
    List<InquiryRequestEntity> findDue(
            Collection<AsyncInquiryStatus> statuses,
            Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from InquiryRequestEntity request
            where request.status = :status and request.processingStartedAt < :before
            order by request.processingStartedAt
            """)
    List<InquiryRequestEntity> findStale(
            AsyncInquiryStatus status,
            Instant before,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from InquiryRequestEntity request
            where request.callbackStatus = :status and request.callbackNextAttemptAt <= :now
            order by request.callbackNextAttemptAt, request.updatedAt
            """)
    List<InquiryRequestEntity> findCallbackDue(
            InquiryCallbackStatus status,
            Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from InquiryRequestEntity request
            where request.callbackStatus = :status and request.callbackProcessingStartedAt < :before
            order by request.callbackProcessingStartedAt
            """)
    List<InquiryRequestEntity> findStaleCallbacks(
            InquiryCallbackStatus status,
            Instant before,
            Pageable pageable
    );
}
