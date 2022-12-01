package dev.hephaestus.proximity.app.impl.logging;


import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.logging.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public final class LogImpl implements Log {
    private final String name;
    private final BufferedWriter logWriter;
    private final PrintWriter printWriter;

    public LogImpl(String name, BufferedWriter logWriter) {
        this.name = name;
        this.logWriter = logWriter;
        this.printWriter = new PrintWriter(this.logWriter);
    }

    public LogImpl(String name) {
        this.name = name;
        this.logWriter = null;
        this.printWriter = null;
    }

    private String format(String name, String message) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        return String.format("[%02d:%02d:%02d.%03d] (%s) %s", hour, minute, second, millis, name, message);
    }

    @Override
    public Log derive(String name) {
        return new NamedLog(this, name);
    }

    @Override
    public synchronized void print(String message, Object... args) {
        this.print(this.name, message, false, args);
    }

    @Override
    public synchronized void print(Throwable error) {
        this.print(this.name, error, false);
    }

    @Override
    public void print(String message, Throwable error) {
        this.print(this.name, message, error, false);
    }

    @Override
    public void write(String message, Object... args) {
        this.print(this.name, message, true, args);
    }

    @Override
    public void write(String message, Throwable error) {
        this.print(this.name, message, error, true);
    }

    @Override
    public void write(Throwable error) {
        this.print(this.name, error, true);
    }

    synchronized void print(String name, String message, boolean quiet, Object... args) {
        message = this.format(name, args.length > 0 ? String.format(message, args) : message);

        if (!quiet) {
            System.out.println(message);
        }

        if (this.logWriter != null) {
            try {
                this.logWriter.write(message + '\n');
                this.logWriter.flush();
            } catch (Exception e) {
                System.out.println("ERROR - Failed to write to log:" + ExceptionUtil.getErrorMessage(e));
            }
        }
    }

    synchronized void print(String name, Throwable error, boolean quiet) {
        if (!quiet) {
            System.out.println(this.format(name, ExceptionUtil.getErrorMessage(error)));
        }

        if (this.printWriter != null) {
            try {
                error.printStackTrace(this.printWriter);
                this.printWriter.flush();
            } catch (Exception e) {
                System.out.println(this.format(name, "ERROR - Failed to write to log:" + ExceptionUtil.getErrorMessage(e)));
            }
        }
    }

    private synchronized void print(String name, String message, Throwable error, boolean quiet) {
        if (!quiet) {
            System.out.println(this.format(name, message + ": " + ExceptionUtil.getErrorMessage(error)));
        }

        if (this.logWriter != null) {
            try {
                this.logWriter.write(this.format(name, message + ": " + ExceptionUtil.getErrorMessage(error)) + '\n');
                this.logWriter.flush();
            } catch (IOException e) {
                System.out.println(this.format(name, "ERROR - Failed to write to log:" + ExceptionUtil.getErrorMessage(e)));
            }
        }

        if (this.printWriter != null) {
            try {
                error.printStackTrace(this.printWriter);
                this.printWriter.flush();
            } catch (Exception e) {
                System.out.println(this.format(name, "ERROR - Failed to write to log:" + ExceptionUtil.getErrorMessage(e)));
            }
        }
    }

    public void close() throws IOException {
        if (this.logWriter != null) {
            this.logWriter.close();
        }
    }
}
