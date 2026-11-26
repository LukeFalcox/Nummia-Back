package com.devfalco.nummia.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/** Registro de operações relevantes (segurança e rastreabilidade). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String action, String entityType, UUID entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDetails(details != null && details.length() > 500 ? details.substring(0, 500) : details);
            entry.setIpAddress(clientIp());
            entry.setCreatedAt(Instant.now());
            repository.save(entry);
        } catch (RuntimeException e) {
            // Auditoria nunca deve interromper o fluxo principal.
            log.warn("Falha ao registrar auditoria {}: {}", action, e.getMessage());
        }
    }

    public void log(UUID userId, String action) {
        log(userId, action, null, null, null);
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
