package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import model.annotation.MaxLength;
import model.annotation.Positive;

public final class AnnotationValidator {
    private AnnotationValidator() {
    }

    public static String[] validate(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object to validate cannot be null.");
        }

        List<String> errors = new ArrayList<>();
        Class<?> currentClass = object.getClass();
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(object);
                    Positive positive = field.getAnnotation(Positive.class);
                    if (positive != null && value instanceof Number number
                            && number.longValue() <= 0) {
                        errors.add(field.getName() + " " + positive.message());
                    }

                    MaxLength maxLength = field.getAnnotation(MaxLength.class);
                    if (maxLength != null && value instanceof String text
                            && text.length() > maxLength.value()) {
                        errors.add(field.getName() + " must have at most "
                                + maxLength.value() + " characters");
                    }
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException(
                            "Unable to validate field " + field.getName(), exception);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return errors.toArray(String[]::new);
    }
}
