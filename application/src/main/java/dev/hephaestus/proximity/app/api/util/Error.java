package dev.hephaestus.proximity.app.api.util;

import dev.hephaestus.proximity.app.api.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class Error implements Iterable<StackTraceElement> {
    private final int indentation;
    private final String message;
    private final List<StackTraceElement> stackTrace;

    public Error(String message, List<StackTraceElement> stackTrace) {
        this(0, message, stackTrace);
    }

    public Error(int indentation, String message, List<StackTraceElement> stackTrace) {
        this.indentation = indentation;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return this.message;
    }

    public List<StackTraceElement> getStackTrace() {
        return this.stackTrace;
    }

    @NotNull
    @Override
    public Iterator<StackTraceElement> iterator() {
        return this.stackTrace.iterator();
    }

    public Error indent() {
        return new Error(this.indentation + 1, this.message, this.stackTrace);
    }

    public void print(Log log) {
        log.print("ERROR" + "\t".repeat(this.indentation) + this.message);

        for (StackTraceElement stackTraceElement : this.stackTrace) {
            log.print("ERROR" + "\t".repeat(this.indentation + 1) + stackTraceElement.toString());
        }
    }
}
