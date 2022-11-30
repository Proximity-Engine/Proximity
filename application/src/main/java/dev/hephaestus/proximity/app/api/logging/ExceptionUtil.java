package dev.hephaestus.proximity.app.api.logging;

public final class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static String getErrorMessage(Throwable e) {
        Throwable original = e;
        String message = e.getMessage();

        while (message == null && e.getCause() != null) {
            e = e.getCause();
            message = e.getMessage();
        }

        if (message == null) {
            original.printStackTrace();
            message = e.toString();
        }

        return e.getClass().getSimpleName() + ": " + message;
    }
}
