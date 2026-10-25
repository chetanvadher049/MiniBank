package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class ReportGenerator {
    private ReportGenerator() {
    }

    public static void generate(Path logsFolder, Path reportFile) throws IOException {
        long totalDeposits = 0;
        long totalWithdrawals = 0;
        StringBuilder report = new StringBuilder("End-of-day transaction report\n");
        report.append("============================\n");

        try (var files = Files.newDirectoryStream(logsFolder, "*.log")) {
            for (Path file : files) {
                if (Files.size(file) == 0) {
                    continue;
                }

                long fileDeposits = 0;
                long fileWithdrawals = 0;
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            long amount = Long.parseLong(parts[1]);
                            if ("DEPOSIT".equals(parts[0])) {
                                totalDeposits += amount;
                                fileDeposits += amount;
                            } else if ("WITHDRAW".equals(parts[0])) {
                                totalWithdrawals += amount;
                                fileWithdrawals += amount;
                            }
                        }
                    }
                }

                BasicFileAttributes attributes = Files.readAttributes(
                        file, BasicFileAttributes.class);
                report.append(file.getFileName())
                        .append(" deposits=").append(fileDeposits)
                        .append(" withdrawals=").append(fileWithdrawals)
                        .append(" size=").append(attributes.size())
                        .append(" modified=").append(attributes.lastModifiedTime())
                        .append('\n');
            }
        }

        report.append("Total deposits: ").append(totalDeposits).append('\n');
        report.append("Total withdrawals: ").append(totalWithdrawals).append('\n');
        Path parent = reportFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                reportFile, StandardCharsets.UTF_8)) {
            writer.write(report.toString());
        }
    }
}
