package ru.klimovich.catalog_service.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggableAspect {
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(loggable)")
    public void loggableMethods(Loggable loggable) {
    }

    @Around(value = "loggableMethods(loggable)", argNames = "joinPoint,loggable")
    public Object collectLogs(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        if (loggable.logArgs()) {
            log.info("Entering method: {} with arguments: {}",
                    methodName, formatArguments(joinPoint.getArgs()));
        }
        try {
            Object result = joinPoint.proceed();
            log.info("Exiting method: {} | Status: SUCCESS | Time: {}ms | Result: {}",
                    methodName,
                    System.currentTimeMillis() - start,
                    loggable.logResult() ? " result= " + formatResult(result) : "");
            return result;

        } catch (Throwable ex) {

            log.error("Exiting method: {} | Status: FAILED | Time: {}ms | Exception: {}",
                    methodName,
                    System.currentTimeMillis() - start,
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(this::safeToString)
                .collect(Collectors.joining(", "));
    }

    private String formatResult(Object result) {
        if (result == null) return "null";
        return safeToString(result);
    }

    private String safeToString(Object value) {
        try {
            if (value instanceof MultipartFile file) {
                return "MultipartFile{name=" + file.getOriginalFilename() + "}";
            }
            String json = objectMapper.writeValueAsString(value);
            return json.length() > 500
                    ? json.substring(0, 500) + "...[TRUNCATED]"
                    : json;

        } catch (Exception e) {
            return value.toString();
        }
    }
}