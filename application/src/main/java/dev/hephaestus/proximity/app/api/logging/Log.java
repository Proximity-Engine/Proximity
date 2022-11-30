package dev.hephaestus.proximity.app.api.logging;

import dev.hephaestus.proximity.app.impl.logging.LogImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;

public interface Log {
    Log derive(String name);

    /**
     * Writes the given message to the log file and prints it to stdout.
     *
     * @param message some message to log
     * @param args some args to be passed to String.format(message, args)
     */
    void print(String message, Object... args);

    /**
     * Writes the given error to the log file and prints it to stdout.
     *
     * @param error some error to log
     */
    void print(Throwable error);

    /**
     * Writes the given message to the log file without writing it to stdout.
     *
     * @param message some message to log
     */
    void write(String message, Object... args);

    /**
     * Writes the given message to the log file without writing it to stdout.
     *
     * @param error some error to log
     */
    void write(Throwable error);

    static Log create(String name, Path outputDir) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Path log = outputDir.resolve(timestamp.toString().replace(":", "") + ".txt");

        try {
            Files.createDirectories(outputDir);
            return new LogImpl(name, Files.newBufferedWriter(log, StandardOpenOption.CREATE));
        } catch (IOException e) {
            System.out.printf("Exception creating logger: %s%n", ExceptionUtil.getErrorMessage(e));
            e.printStackTrace();

            return new LogImpl(name);
        }
    }
}
