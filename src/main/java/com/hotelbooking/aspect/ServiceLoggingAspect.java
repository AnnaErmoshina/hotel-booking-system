package com.hotelbooking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP pattern (Spring AOP / AspectJ): cross-cutting logging around every
 * public method of every *ServiceImpl bean — execution time, and whether it
 * succeeded or threw — without adding logging code to each service class
 * individually. See PROJECT_STATUS.md section 8: "Spring AOP" was pulled in
 * via spring-boot-starter-aop but had no @Aspect class before this.
 */
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.hotelbooking.service.impl.*ServiceImpl.*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long startedAt = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.info("{} completed in {} ms", signature, System.currentTimeMillis() - startedAt);
            return result;
        } catch (Throwable ex) {
            log.warn("{} failed after {} ms: {}", signature, System.currentTimeMillis() - startedAt, ex.getMessage());
            throw ex;
        }
    }
}
