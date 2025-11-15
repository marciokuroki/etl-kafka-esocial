package com.esocial.consumer.controller;

import com.esocial.consumer.model.entity.DlqEvent;
import com.esocial.consumer.model.entity.ValidationError;
import com.esocial.consumer.repository.DlqEventRepository;
import com.esocial.consumer.repository.ValidationErrorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/validation")
public class ValidationReportController {
    
    private final ValidationErrorRepository validationErrorRepository;
    private final DlqEventRepository dlqEventRepository;
    
    public ValidationReportController(
            ValidationErrorRepository validationErrorRepository,
            DlqEventRepository dlqEventRepository) {
        this.validationErrorRepository = validationErrorRepository;
        this.dlqEventRepository = dlqEventRepository;
    }
    
    /**
     * Lista todos os erros de validação
     */
    @GetMapping("/errors")
    public ResponseEntity<List<ValidationError>> getAllErrors() {
        return ResponseEntity.ok(validationErrorRepository.findAll());
    }
    
    /**
     * Lista erros recentes (últimas 24 horas)
     */
    @GetMapping("/errors/recent")
    public ResponseEntity<List<ValidationError>> getRecentErrors(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(validationErrorRepository.findRecentErrors(since));
    }
    
    /**
     * Lista erros por severidade
     */
    @GetMapping("/errors/severity/{severity}")
    public ResponseEntity<List<ValidationError>> getErrorsBySeverity(
            @PathVariable String severity) {
        return ResponseEntity.ok(validationErrorRepository.findBySeverity(severity));
    }
    
    /**
     * Estatísticas de erros por regra
     */
    @GetMapping("/errors/stats")
    public ResponseEntity<Map<String, Object>> getErrorStats() {
        List<Object[]> stats = validationErrorRepository.countErrorsByRule();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalErrors", validationErrorRepository.count());
        result.put("errorsByRule", stats);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Lista eventos na DLQ
     */
    @GetMapping("/dlq")
    public ResponseEntity<List<DlqEvent>> getDlqEvents() {
        return ResponseEntity.ok(dlqEventRepository.findAll());
    }
    
    /**
     * Lista eventos DLQ por status
     */
    @GetMapping("/dlq/status/{status}")
    public ResponseEntity<List<DlqEvent>> getDlqEventsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(dlqEventRepository.findByStatus(status));
    }
    
    /**
     * Dashboard com estatísticas gerais
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Erros de validação
        long totalErrors = validationErrorRepository.count();
        long errorSeverity = validationErrorRepository.findBySeverity("ERROR").size();
        long warningSeverity = validationErrorRepository.findBySeverity("WARNING").size();
        
        // DLQ
        long totalDlq = dlqEventRepository.count();
        long pendingDlq = dlqEventRepository.findByStatus("PENDING").size();
        
        dashboard.put("validation", Map.of(
                "totalErrors", totalErrors,
                "errors", errorSeverity,
                "warnings", warningSeverity
        ));
        
        dashboard.put("dlq", Map.of(
                "total", totalDlq,
                "pending", pendingDlq
        ));
        
        return ResponseEntity.ok(dashboard);
    }
}
