package dev.hephaestus.proximity.app.api.util;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.proximity.app.api.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class Error implements Iterable<StackTraceElement> {
    private final int indentation;
    private final String message;
    private final ImmutableList<StackTraceElement> stackTrace;

    public Error(String message, ImmutableList<StackTraceElement> stackTrace) {
        this(0, message, stackTrace);
    }

    public Error(int indentation, String message, ImmutableList<StackTraceElement> stackTrace) {
        this.indentation = indentation;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return this.message;
    }

    public ImmutableList<StackTraceElement> getStackTrace() {
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
