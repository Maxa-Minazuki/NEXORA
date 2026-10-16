package kz.jyly.controller;

import kz.jyly.exception.BusinessException;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.validation.BindingResult;

/** Сервистен келген бизнес-қатені форманың тиісті өрісіне байлау. */
final class FormErrors {

    private FormErrors() {
    }

    static void apply(BindingResult result, BusinessException e) {
        if (e.getField() != null) {
            try {
                result.rejectValue(e.getField(), "business", e.getMessage());
                return;
            } catch (NotReadablePropertyException | IllegalArgumentException ignored) {
                // Өріс формада жоқ болса, жалпы қате ретінде көрсетеміз (төменде).
            }
        }
        result.reject("business", e.getMessage());
    }
}
