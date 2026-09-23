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

/** Secured operational API for job catalog management and execution history. */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobManagementController {

    private final JobManagementService service;

    @GetMapping
    @PreAuthorize("hasAuthority('jobs:read') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public List<JobDefinitionView> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('jobs:read') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobDefinitionView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/handlers")
    @PreAuthorize("hasAuthority('jobs:read') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public List<String> handlers() {
        return service.registeredHandlerKeys();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('jobs:write') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobDefinitionView create(@Valid @RequestBody CreateJobRequest request) {
        return service.create(new CreateJobDefinitionCommand(
                request.code(), request.displayName(), request.description(), request.handlerKey(),
                request.cronExpression(), request.timeZone(), request.enabled()
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('jobs:write') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobDefinitionView update(@PathVariable UUID id, @Valid @RequestBody UpdateJobRequest request) {
        return service.update(id, new UpdateJobDefinitionCommand(
                request.displayName(), request.description(), request.handlerKey(),
                request.cronExpression(), request.timeZone(), request.enabled()
        ));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAuthority('jobs:write') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobDefinitionView setEnabled(
            @PathVariable UUID id,
            @RequestBody SetJobEnabledRequest request
    ) {
        return service.setEnabled(id, request.enabled());
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAuthority('jobs:read') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public Page<JobExecutionView> history(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        return service.history(id, pageable);
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasAuthority('jobs:read') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobStatisticsView statistics(@PathVariable UUID id) {
        return service.statistics(id);
    }

    @PostMapping("/{id}/executions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('jobs:execute') or hasAnyRole('SYSTEM_OPERATOR', 'UAA_ADMIN')")
    public JobExecutionView execute(@PathVariable UUID id, Authentication authentication) {
        return service.executeManually(id, authentication.getName());
    }
}
