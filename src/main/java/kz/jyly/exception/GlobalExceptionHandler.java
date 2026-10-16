package kz.jyly.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * HTML беттеріне арналған ортақ қателерді өңдеу.
 * RestController-лер бұл advice-қа кірмейді (annotations сүзгісі), олар JSON қайтарады.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, MethodArgumentTypeMismatchException.class})
    public String notFound(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("status", 404);
        model.addAttribute("title", "Бет табылмады");
        model.addAttribute("message", ex instanceof NotFoundException ? ex.getMessage()
                : "Сұралған бет немесе жазба табылмады.");
        return "error";
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String concurrentUpdate(Exception ex, Model model, HttpServletResponse response) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        response.setStatus(HttpStatus.CONFLICT.value());
        model.addAttribute("status", 409);
        model.addAttribute("title", "Деректер жаңарды");
        model.addAttribute("message", "Бұл жазбаны басқа біреу дәл қазір өзгертті. Бетті жаңартып, қайта көріңіз.");
        return "error";
    }
}
