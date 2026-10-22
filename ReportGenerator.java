import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;

public class ReportGenerator {

    public static void generateReport(Path logsDir, Path reportFile) throws IOException {
        long totalDeposits = 0;
        long totalWithdrawals = 0;
        int fileCount = 0;

        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("=====================================================\n");
        reportBuilder.append("           MINIBANK END-OF-DAY SUMMARY REPORT        \n");
        reportBuilder.append("=====================================================\n\n");
        reportBuilder.append("Processed Log Files Details:\n");
        reportBuilder.append("-----------------------------------------------------\n");

        if (Files.exists(logsDir) && Files.isDirectory(logsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        fileCount++;
                        BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                        long size = attrs.size();
                        FileTime lastModified = attrs.lastModifiedTime();

                        reportBuilder.append(String.format("File: %-20s | Size: %6d bytes | Last Modified: %s\n",
                                entry.getFileName().toString(), size, lastModified.toString()));

                        try (BufferedReader reader = Files.newBufferedReader(entry)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String trimmed = line.trim();
                                if (trimmed.isEmpty()) continue;
                                String[] tokens = trimmed.split("[,\\s]+");
                                for (int i = 0; i < tokens.length; i++) {
                                    if (tokens[i].equalsIgnoreCase("DEPOSIT") && i + 1 < tokens.length) {
                                        try {
                                            totalDeposits += Long.parseLong(tokens[i + 1]);
                                        } catch (NumberFormatException ignored) {}
                                    } else if (tokens[i].equalsIgnoreCase("WITHDRAW") && i + 1 < tokens.length) {
                                        try {
                                            totalWithdrawals += Long.parseLong(tokens[i + 1]);
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        reportBuilder.append("-----------------------------------------------------\n");
        reportBuilder.append("Total Files Processed : ").append(fileCount).append("\n");
        reportBuilder.append("Total Deposits        : Rs.").append(totalDeposits).append("\n");
        reportBuilder.append("Total Withdrawals     : Rs.").append(totalWithdrawals).append("\n");
        reportBuilder.append("Net Balance Change    : Rs.").append(totalDeposits - totalWithdrawals).append("\n");
        reportBuilder.append("=====================================================\n");

        if (reportFile.getParent() != null) {
            Files.createDirectories(reportFile.getParent());
        }

        Files.write(reportFile, Collections.singletonList(reportBuilder.toString()),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
