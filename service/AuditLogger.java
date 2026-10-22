// 25CE079
package service;

import java.lang.reflect.Method;
import model.annotation.Audit;

public class AuditLogger {

    public static void logCall(Class<?> clazz, String methodName) {
        try {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(Audit.class)) {
                    Audit audit = method.getAnnotation(Audit.class);
                    System.out.println("[AUDIT LOG] Action '" + audit.value() + "' executed via " + clazz.getSimpleName() + "." + methodName + "()");
                    return;
                }
            }
        } catch (Exception ignored) {}
    }
}
