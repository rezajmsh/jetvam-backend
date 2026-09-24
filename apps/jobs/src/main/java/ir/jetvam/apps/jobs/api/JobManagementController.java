package ir.jetvam.apps.jobs.api;

import ir.jetvam.apps.jobs.infrastructure.service.CreateJobDefinitionCommand;
import ir.jetvam.apps.jobs.infrastructure.service.JobDefinitionView;
import ir.jetvam.apps.jobs.infrastructure.service.JobExecutionView;
import ir.jetvam.apps.jobs.infrastructure.service.JobManagementService;
import ir.jetvam.apps.jobs.infrastructure.service.JobStatisticsView;
import ir.jetvam.apps.jobs.infrastructure.service.UpdateJobDefinitionCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Secured operational API for job catalog management and execution history.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobManagementController {

    private final JobManagementService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:read')")
    public List<JobDefinitionView> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:read')")
    public JobDefinitionView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/handlers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:read')")
    public List<String> handlers() {
        return service.registeredHandlerKeys();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('jobs:write')")
    public JobDefinitionView create(@Valid @RequestBody CreateJobRequest request) {
        return service.create(new CreateJobDefinitionCommand(
                request.code(), request.displayName(), request.description(), request.handlerKey(),
                request.cronExpression(), request.timeZone(), request.enabled()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('jobs:write')")
    public JobDefinitionView update(@PathVariable UUID id, @Valid @RequestBody UpdateJobRequest request) {
        return service.update(id, new UpdateJobDefinitionCommand(
                request.displayName(), request.description(), request.handlerKey(),
                request.cronExpression(), request.timeZone(), request.enabled()
        ));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('jobs:write')")
    public JobDefinitionView setEnabled(
            @PathVariable UUID id,
            @RequestBody SetJobEnabledRequest request
    ) {
        return service.setEnabled(id, request.enabled());
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:read')")
    public Page<JobExecutionView> history(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.history(id, pageable);
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:read')")
    public JobStatisticsView statistics(@PathVariable UUID id) {
        return service.statistics(id);
    }

    @PostMapping("/{id}/executions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('jobs:execute')")
    public JobExecutionView execute(@PathVariable UUID id, Authentication authentication) {
        return service.executeManually(id, authentication.getName());
    }
}
