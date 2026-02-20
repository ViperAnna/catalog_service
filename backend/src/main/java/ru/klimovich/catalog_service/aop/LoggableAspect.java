package ru.klimovich.catalog_service.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.request.ProductRequest;

import java.io.IOException;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggableAspect {
    private final ObjectMapper objectMapper;
    private static final int MAX_LOG_SIZE = 500;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void allRestControllerMethods() {
    }

    @Around("allRestControllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.info("--->Entering method: [{}]", methodName);

        logArgument(joinPoint);

        try {
            Object result = joinPoint.proceed();
            log.info(
                    "<--- Exiting method: [{}] | [{}] ms | resultType=[{}] | result={}",
                    methodName,
                    System.currentTimeMillis() - start,
                    result != null ? result.getClass().getSimpleName() : "null",
                    safeToString(result)
            );
            return result;

        } catch (Throwable e) {
            log.error(
                    " <--- FAILED| Exiting method: {} | {} ms | exception={}",
                    methodName,
                    System.currentTimeMillis() - start,
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    private void logArgument(ProceedingJoinPoint joinPoint) throws IOException {
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null) {
                log.info("Method without argument.");
                continue;
            }
            if (arg instanceof CategoryRequest request) {
                logMultipartFile(request.getImage());
            } else if (arg instanceof ProductRequest productRequest) {
                productRequest.getImages().forEach(this::logMultipartFile);
            }
            log.info(
//                    название аргумента
                    "Argument: [{}]]", safeToString(arg)
            );
        }
    }

    private void logMultipartFile(MultipartFile file) {
        try {

            long sizeInBytes = file.getSize();
            double sizeInKB = sizeInBytes / 1024.0;
            log.info("File info: name= [{}]," +
                            "size= [{}] KB," +
                            "sha-256= [{}]",
                    file.getOriginalFilename(),
                    String.format("%.2f", sizeInKB),
                    DigestUtils.sha256Hex(file.getInputStream()));
        } catch (IOException e) {
            log.warn("Failed to log multipart file [{}]: {}", file.getOriginalFilename(), e.getMessage());
        }
    }

    private String safeToString(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() > MAX_LOG_SIZE) {
                json = json.substring(0, MAX_LOG_SIZE) + "...[TRUNCATED]";
            }
            json = json.replace("{", "\\{")
                    .replace("}", "\\}")
                    .replace("[", "\\[")
                    .replace("]", "\\]");
            return json;
        } catch (Exception e) {
            return value.toString();
        }
    }
}















