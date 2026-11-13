package com.example.bankingapp;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Uses installed PostgreSQL binaries; never connects to the developer's database. */
final class IsolatedPostgres implements AutoCloseable {
    private final Path directory;
    private final Path data;
    private final String binaries;
    private final int port;
    final String password = UUID.randomUUID().toString();
    final String username = "bank_test";

    IsolatedPostgres() {
        try {
            Files.createDirectories(Path.of("target"));
            directory = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "postgres-it-");
            data = directory.resolve("data");
            String installed = System.getProperty("os.name").startsWith("Windows")
                    ? "C:/Program Files/PostgreSQL/18/bin" : "";
            binaries = System.getProperty("bank.test.postgres-bin", installed);
            try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
            var passwordFile = directory.resolve("password.txt");
            Files.writeString(passwordFile, password);
            run("initdb", "-D", data.toString(), "-U", username, "-A", "scram-sha-256",
                    "--pwfile=" + passwordFile, "--encoding=UTF8", "--locale=C");
            Files.delete(passwordFile);
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
            start();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot start isolated PostgreSQL. Install PostgreSQL or set "
                    + "-Dbank.test.postgres-bin=/path/to/bin. No application database is used.", ex);
        }
    }

    String url() { return "jdbc:postgresql://127.0.0.1:" + port + "/postgres"; }

    void restart() throws Exception {
        run("pg_ctl", "-D", data.toString(), "-m", "fast", "-w", "stop");
        start();
    }

    private void start() throws Exception {
        run("pg_ctl", "-D", data.toString(), "-l", directory.resolve("server.log").toString(),
                "-o", "-h 127.0.0.1 -p " + port, "-w", "-t", "30", "start");
        try (var connection = DriverManager.getConnection(url(), username, password)) {
            if (!connection.isValid(5)) throw new IllegalStateException("Test PostgreSQL is unavailable");
        }
    }

    private void run(String executable, String... arguments) throws IOException, InterruptedException {
        var command = new ArrayList<String>();
        command.add(binaries.isEmpty() ? executable : Path.of(binaries, executable).toString());
        command.addAll(List.of(arguments));
        var log = directory.resolve(executable + ".log");
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException(executable + " timed out; see " + log);
        }
        if (process.exitValue() != 0) throw new IllegalStateException(executable + " failed: " + Files.readString(log));
    }

    @Override public synchronized void close() {
        if (!Files.exists(data.resolve("postmaster.pid"))) return;
        try { run("pg_ctl", "-D", data.toString(), "-m", "fast", "-w", "stop"); }
        catch (Exception ex) { System.err.println("Could not stop test PostgreSQL at " + data + ": " + ex.getMessage()); }
    }
}
