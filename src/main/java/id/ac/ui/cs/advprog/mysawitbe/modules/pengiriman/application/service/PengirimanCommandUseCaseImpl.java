package id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.service;

import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.kebun.application.port.in.KebunQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.dto.PanenDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.panen.application.port.in.PanenQueryUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.dto.PengirimanDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.event.PengirimanApprovedByMandorEvent;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.in.PengirimanCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.application.port.out.PengirimanRepositoryPort;
import id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.domain.PengirimanStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PengirimanCommandUseCaseImpl implements PengirimanCommandUseCase {

    static final int MAX_TOTAL_WEIGHT_GRAMS = 400_000;

    private final PengirimanRepositoryPort repository;
    private final KebunQueryUseCase kebunQueryUseCase;
    private final PanenQueryUseCase panenQueryUseCase;
    @SuppressWarnings("unused")
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PengirimanDTO assignSupirForDelivery(UUID mandorId, UUID supirId, List<UUID> panenIds) {
        if (mandorId == null) {
            throw new IllegalArgumentException("Mandor ID is required");
        }
        if (supirId == null) {
            throw new IllegalArgumentException("Supir ID is required");
        }

        List<UUID> normalizedPanenIds = normalizePanenIds(panenIds);
        ensureSupirBelongsToMandorKebun(mandorId, supirId);

        UUID kebunId = kebunQueryUseCase.findKebunIdByMandorId(mandorId);
        if (kebunId == null) {
            throw new IllegalStateException("Mandor belum memiliki kebun.");
        }

        Map<UUID, PanenDTO> approvedPanen = panenQueryUseCase.getApprovedPanenByKebun(kebunId).stream()
                .collect(Collectors.toMap(PanenDTO::panenId, panen -> panen));

        List<UUID> missingPanenIds = normalizedPanenIds.stream()
                .filter(panenId -> !approvedPanen.containsKey(panenId))
                .toList();
        if (!missingPanenIds.isEmpty()) {
            throw new IllegalArgumentException("Semua panen harus sudah disetujui dan berasal dari kebun mandor.");
        }

        List<UUID> alreadyAssignedPanenIds = repository.findAssignedPanenIds(normalizedPanenIds);
        if (!alreadyAssignedPanenIds.isEmpty()) {
            throw new IllegalStateException("Sebagian panen sudah pernah dimasukkan ke pengiriman lain.");
        }

        int totalWeight = normalizedPanenIds.stream()
                .map(approvedPanen::get)
                .mapToInt(PanenDTO::weight)
                .sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Total berat pengiriman harus lebih dari 0.");
        }
        if (totalWeight > MAX_TOTAL_WEIGHT_GRAMS) {
            throw new IllegalArgumentException("Total berat pengiriman tidak boleh melebihi 400 kg.");
        }

        return repository.save(new PengirimanDTO(
                null,
                supirId,
                null,
                mandorId,
                null,
                PengirimanStatus.ASSIGNED.name(),
                totalWeight,
                0,
                null,
                normalizedPanenIds,
                LocalDateTime.now()
        ));
    }

    @Override
    public PengirimanDTO updateDeliveryStatus(UUID pengirimanId, UUID supirId, PengirimanStatus newStatus) {
        throw new UnsupportedOperationException("Milestone 50 supir status update belum diaktifkan di commit ini.");
    }

    @Override
    public PengirimanDTO mandorApproveDelivery(UUID pengirimanId, UUID mandorId) {
        PengirimanDTO current = requirePengiriman(pengirimanId);
        ensureMandorOwnership(current, mandorId);
        ensureStatus(current, PengirimanStatus.TIBA, "Mandor hanya bisa menyetujui pengiriman yang sudah TIBA.");

        PengirimanDTO saved = saveUpdated(current, builder -> {
            builder.status(PengirimanStatus.APPROVED_MANDOR.name());
            builder.acceptedWeight(0);
            builder.statusReason(null);
        });

        eventPublisher.publishEvent(new PengirimanApprovedByMandorEvent(
                saved.pengirimanId(),
                saved.supirId(),
                saved.totalWeight()
        ));
        return saved;
    }

    @Override
    public PengirimanDTO mandorRejectDelivery(UUID pengirimanId, UUID mandorId, String reason) {
        String normalizedReason = normalizeRequiredReason(reason);
        PengirimanDTO current = requirePengiriman(pengirimanId);
        ensureMandorOwnership(current, mandorId);
        ensureStatus(current, PengirimanStatus.TIBA, "Mandor hanya bisa menolak pengiriman yang sudah TIBA.");

        return saveUpdated(current, builder -> {
            builder.status(PengirimanStatus.REJECTED_MANDOR.name());
            builder.acceptedWeight(0);
            builder.statusReason(normalizedReason);
        });
    }

    @Override
    public PengirimanDTO adminProcessDelivery(
            UUID pengirimanId,
            UUID adminId,
            int acceptedWeight,
            PengirimanStatus status,
            String reason
    ) {
        throw new UnsupportedOperationException("Milestone 50 admin processing belum diaktifkan di commit ini.");
    }

    private void ensureSupirBelongsToMandorKebun(UUID mandorId, UUID supirId) {
        boolean exists = kebunQueryUseCase.getSupirListByMandorId(mandorId).stream()
                .map(UserDTO::userId)
                .anyMatch(supirId::equals);
        if (!exists) {
            throw new IllegalArgumentException("Supir tidak terdaftar di kebun mandor ini.");
        }
    }

    private PengirimanDTO requirePengiriman(UUID pengirimanId) {
        PengirimanDTO dto = repository.findById(pengirimanId);
        if (dto == null) {
            throw new EntityNotFoundException("Pengiriman not found: " + pengirimanId);
        }
        return dto;
    }

    private void ensureMandorOwnership(PengirimanDTO current, UUID mandorId) {
        if (mandorId == null || !mandorId.equals(current.mandorId())) {
            throw new IllegalArgumentException("Mandor tidak berhak memproses pengiriman ini.");
        }
    }

    private void ensureStatus(PengirimanDTO current, PengirimanStatus expected, String message) {
        if (parseStatus(current.status()) != expected) {
            throw new IllegalStateException(message);
        }
    }

    private PengirimanDTO saveUpdated(PengirimanDTO current, Consumer<PengirimanBuilder> customizer) {
        PengirimanBuilder builder = PengirimanBuilder.from(current);
        customizer.accept(builder);
        return repository.save(builder.build());
    }

    private List<UUID> normalizePanenIds(List<UUID> panenIds) {
        if (panenIds == null || panenIds.isEmpty()) {
            throw new IllegalArgumentException("Panen IDs are required");
        }
        Set<UUID> uniquePanenIds = new LinkedHashSet<>();
        for (UUID panenId : panenIds) {
            if (panenId == null) {
                throw new IllegalArgumentException("Panen ID cannot be null.");
            }
            uniquePanenIds.add(panenId);
        }
        return List.copyOf(uniquePanenIds);
    }

    private PengirimanStatus parseStatus(String status) {
        try {
            return PengirimanStatus.valueOf(status);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unknown pengiriman status: " + status);
        }
    }

    private String normalizeRequiredReason(String reason) {
        String normalized = normalizeOptionalReason(reason);
        if (normalized == null) {
            throw new IllegalArgumentException("Reason is required.");
        }
        return normalized;
    }

    private String normalizeOptionalReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class PengirimanBuilder {
        private final UUID pengirimanId;
        private final UUID supirId;
        private final String supirName;
        private final UUID mandorId;
        private final String mandorName;
        private String status;
        private final int totalWeight;
        private int acceptedWeight;
        private String statusReason;
        private final List<UUID> panenIds;
        private final LocalDateTime timestamp;

        private PengirimanBuilder(PengirimanDTO source) {
            this.pengirimanId = source.pengirimanId();
            this.supirId = source.supirId();
            this.supirName = source.supirName();
            this.mandorId = source.mandorId();
            this.mandorName = source.mandorName();
            this.status = source.status();
            this.totalWeight = source.totalWeight();
            this.acceptedWeight = source.acceptedWeight();
            this.statusReason = source.statusReason();
            this.panenIds = source.panenIds();
            this.timestamp = source.timestamp();
        }

        static PengirimanBuilder from(PengirimanDTO source) {
            return new PengirimanBuilder(source);
        }

        void status(String status) {
            this.status = status;
        }

        void acceptedWeight(int acceptedWeight) {
            this.acceptedWeight = acceptedWeight;
        }

        void statusReason(String statusReason) {
            this.statusReason = statusReason;
        }

        PengirimanDTO build() {
            return new PengirimanDTO(
                    pengirimanId,
                    supirId,
                    supirName,
                    mandorId,
                    mandorName,
                    status,
                    totalWeight,
                    acceptedWeight,
                    statusReason,
                    panenIds,
                    timestamp
            );
        }
    }
}
