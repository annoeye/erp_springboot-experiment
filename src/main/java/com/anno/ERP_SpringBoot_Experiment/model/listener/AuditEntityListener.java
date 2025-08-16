package com.anno.ERP_SpringBoot_Experiment.model.listener;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class AuditEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        AuditInfo audit = extractAuditInfo(entity);
        if (audit == null) return;

        String username = getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);
        audit.setCreatedBy(username);
        audit.setUpdatedBy(username);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        AuditInfo audit = extractAuditInfo(entity);
        if (audit == null) return;

        audit.setUpdatedAt(LocalDateTime.now());
        audit.setUpdatedBy(getCurrentUsername());
    }

    @PreRemove
    public void preRemove(Object entity) {
        AuditInfo audit = extractAuditInfo(entity);
        if (audit == null) return;

        audit.setDeletedAt(LocalDateTime.now());
        audit.setDeletedBy(getCurrentUsername());
    }

    private AuditInfo extractAuditInfo(Object entity) {
        try {
            Method method = entity.getClass().getMethod("getAuditInfo");
            return (AuditInfo) method.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SYSTEM";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

}
