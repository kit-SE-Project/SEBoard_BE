package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class Requests {
    private Requests() {}
    public enum Band { MORNING, AFTERNOON, ALL_DAY }
    public record Slot(@NotNull Instant startAt,@NotNull Instant endAt) {}
    public record Rule(@NotNull LocalDate from,@NotNull LocalDate to,
        @NotNull @Size(min=1,max=7) Set<DayOfWeek> weekdays,
        @NotNull @Size(min=1,max=3) Set<Band> bands) {}
    public record Reservation(@Positive long serverId,@NotBlank @Size(max=2000) String purpose,
        @Size(min=1,max=256) List<@Valid Slot> slots,@Valid Rule rule) {}
    public record Change(@NotNull @PositiveOrZero Long expectedVersion,@Size(max=500) String reason) {}
    public record PrepareNow(@NotNull @PositiveOrZero Long expectedVersion,@Positive long serverId,
        @Positive long memberId,@NotBlank @Size(max=500) String reason) {}
    public record CategoryInput(@NotBlank @Size(max=100) String name,@Size(max=2000) String description) {}
    public record CategoryEdit(@NotBlank @Size(max=100) String name,@Size(max=2000) String description,
        @NotNull @PositiveOrZero Long expectedVersion) {}
    public record CategoryDelete(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record ServerInput(@Positive long categoryId,@NotBlank @Size(max=100) String displayName,
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,100}") String nodeName,
        @NotBlank @Size(max=200) String cpuModel,@Positive int memoryGb,@NotBlank @Size(max=200) String gpuModel,
        Boolean gpuEnabled) {}
    public record ServerStatus(@NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Pattern(regexp="AVAILABLE|MAINTENANCE|QUARANTINED|UNKNOWN") String status,
        @NotBlank @Size(max=500) String reason) {}
    public record ServerEdit(@NotNull @PositiveOrZero Long expectedVersion,@NotNull @Positive Long categoryId,
        @NotBlank @Size(max=100) String displayName,@NotBlank @Size(max=200) String cpuModel,
        @NotNull @Positive Integer memoryGb,@NotBlank @Size(max=200) String gpuModel,
        @NotBlank @Size(max=500) String reason) {}
    public record ServerDelete(@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=500) String reason) {}
    public record Claim(@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{1,100}") String workerId) {}
    public record Heartbeat(@NotBlank String workerId,@NotBlank String commandId) {}
    public record DispatchDeferred(@NotBlank String workerId,@NotBlank String commandId,
        @NotNull Instant observedAt,@NotNull @AssertTrue Boolean dispatchNotStarted,
        @NotBlank @Pattern(regexp="[A-Z0-9_]{1,100}") String reasonCode) {}
    public record Verification(@NotBlank String workerId,@NotNull @PositiveOrZero Long desiredRevision,
        @NotBlank String commandId,
        @NotBlank @Size(max=200) String operationId,@Size(max=500) String upid,
        @NotNull Instant observedAt,@NotNull @Size(min=5,max=5) List<@Valid VerificationItem> items) {}
    public record VerificationItem(@NotBlank @Pattern(regexp="ACCESS_BLOCKED|VM_ABSENT|VOLUMES_ABSENT|SNIPPETS_ABSENT|INGRESS_ABSENT") String kind,
        Boolean passed,@NotNull Map<String,Object> evidence) {}
    public record CredentialApplied(@NotBlank String workerId,@Positive int credentialVersion,
        @NotBlank String commandId,
        @NotNull @PositiveOrZero Long desiredRevision,@NotBlank @Size(max=200) String operationId,
        @Size(max=500) String upid,Boolean passed) {}
    public record Observation(@NotBlank String workerId,@NotBlank String commandId,
        @NotNull @PositiveOrZero Long desiredRevision,@Positive int vmid,
        @NotBlank @Pattern(regexp="[0-9.]{7,15}") String ip,
        @NotNull @PositiveOrZero Long gpuGeneration,@NotNull Instant observedAt,
        @NotBlank @Pattern(regexp="RUNNING|STOPPED|UNKNOWN") String vmState,
        @NotNull Boolean healthPassed,@NotNull Boolean accessEnabled,
        @Size(max=200) String hostKeyFingerprint,@NotBlank @Size(max=200) String operationId,@Size(max=500) String upid,
        Boolean gpuDetached) {}
    public record Inventory(@NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank @Pattern(regexp="[0-9./]{9,18}") String vmSubnet,
        @NotBlank @Pattern(regexp="[0-9.]{7,15}") String ipPoolStart,
        @NotBlank @Pattern(regexp="[0-9.]{7,15}") String ipPoolEnd,
        @Min(100) @Max(999999999) int vmidStart,@Min(100) @Max(999999999) int vmidEnd,
        Boolean gpuEnabled, @Size(max=200) String gpuMappingRef,
        @NotBlank @Pattern(regexp="[A-Za-z0-9.-]{1,253}") String publicSshHost,
        @Min(1) @Max(65535) int publicSshPort,@NotBlank @Size(max=500) String reason) {}
    public record Template(@Min(100) @Max(999999999) int templateVmid,
        @NotBlank @Size(max=200) String imageHash,@NotBlank @Size(max=200) String driverVersion,
        @NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=500) String reason) {}
    public record ServerObservation(@NotNull Instant observedAt,
        @NotBlank @Pattern(regexp="AVAILABLE|MAINTENANCE|QUARANTINED|UNKNOWN") String status) {}
    public record Allocate(@NotBlank String workerId,@NotBlank String commandId) {}
    public record Manifest(@NotBlank String workerId,@NotBlank String commandId,
        @NotNull @Size(max=512) List<@NotBlank @Size(max=300) String> volumeIds,
        @NotNull @Size(max=512) List<@NotBlank @Size(max=300) String> snippetIds,
        @NotNull @Size(max=512) List<@NotBlank @Size(max=300) String> ingressRuleIds) {}
    public record Failure(@NotBlank String workerId,@NotBlank String commandId,
        @NotNull @PositiveOrZero Long desiredRevision,@NotBlank @Size(max=200) String operationId,
        @NotBlank @Pattern(regexp="[A-Z0-9_]{1,100}") String reasonCode,
        @NotNull Instant observedAt,@NotNull Boolean outcomeKnown,@NotNull Boolean accessBlocked) {}
    public record Reconcile(@NotNull @PositiveOrZero Long expectedVersion,@NotBlank String commandId,
        @NotNull Instant observedAt,@NotNull Boolean terminalConfirmed,@NotNull Boolean accessBlocked,
        @NotBlank @Size(max=200) String operationId,@NotBlank @Size(max=500) String reason) {}
    public record ResourceRecoveryEvidence(@NotNull Instant observedAt,
        @NotBlank @Size(max=200) String recoveryReference,
        @NotBlank @Pattern(regexp="ABSENT|STOPPED|RUNNING") String vmState,
        @NotNull Boolean ownershipConfirmed,@NotNull @PositiveOrZero Integer activeTaskCount,
        @NotNull @PositiveOrZero Integer unknownExecutionCount,@NotNull Boolean accessBlocked,
        @NotBlank @Size(max=2000) String details) {}
    public record ResourceLockReconcile(@NotNull @PositiveOrZero Long expectedVersion,
        @Positive long serverId,@Positive long jobId,@NotBlank @Size(max=200) String commandId,
        @Positive long allocationId,@Min(100) int vmid,@Positive long ipLeaseId,
        @NotBlank @Pattern(regexp="[0-9.]{7,15}") String ip,
        @NotNull @PositiveOrZero Long gpuGeneration,@NotNull @Valid ResourceRecoveryEvidence evidence,
        @NotBlank @Size(max=500) String reason) {}
    public record VmHealth(@NotNull @PositiveOrZero Long desiredRevision,@Min(100) int vmid,
        @NotNull @PositiveOrZero Long gpuGeneration,@NotNull Instant observedAt,
        @NotBlank @Pattern(regexp="RUNNING|STOPPED|UNKNOWN") String vmState,
        @NotNull Boolean healthPassed,@NotNull Boolean accessEnabled,
        @Size(max=200) String hostKeyFingerprint) {}
}
