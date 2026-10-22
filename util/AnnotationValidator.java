// 25CE079
package util;

import model.annotation.MaxLength;
import model.annotation.Positive;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class AnnotationValidator {

    public static String[] validate(Object obj) {
        ArrayList<String> errors = new ArrayList<>();
        Class<?> clazz = obj.getClass();

        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (field.isAnnotationPresent(Positive.class)) {
                        Positive p = field.getAnnotation(Positive.class);
                        if (value instanceof Number && ((Number) value).longValue() <= 0) {
                            errors.add(field.getName() + ": " + p.message());
                        }
                    }
                    if (field.isAnnotationPresent(MaxLength.class)) {
                        MaxLength ml = field.getAnnotation(MaxLength.class);
                        if (value instanceof String && ((String) value).length() > ml.value()) {
                            errors.add(field.getName() + ": max length is " + ml.value());
                        }
                    }
                } catch (IllegalAccessException e) {
                    errors.add(field.getName() + ": cannot access field");
                }
            }
            clazz = clazz.getSuperclass();
        }

        return errors.toArray(new String[0]);
    }
}
