package dev.skymetron.infrastructure.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(auditLogEntry)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLogEntry) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth != null ? auth.getName() : "anonymous";
        String action = auditLogEntry.action();
        String resource = auditLogEntry.resource();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            auditLog.info("user={} action={} resource={} status=success duration={}ms", user, action, resource, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditLog.warn("user={} action={} resource={} status=failure duration={}ms error={}",
                    user, action, resource, elapsed, e.getMessage());
            throw e;
        }
    }
}
