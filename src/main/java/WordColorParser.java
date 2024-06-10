import org.apache.poi.xwpf.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordColorParser {
    public static class ParseResult {
        private final Map<String, String> colorDurationMap;
        private final List<String> colorFormattingErrors;
        private final List<String> otherErrors;

        public ParseResult(Map<String, String> colorDurationMap, List<String> colorFormattingErrors, List<String> otherErrors) {
            this.colorDurationMap = colorDurationMap;
            this.colorFormattingErrors = colorFormattingErrors;
            this.otherErrors = otherErrors;
        }

        public Map<String, String> getColorDurationMap() {
            return colorDurationMap;
        }

        public List<String> getColorFormattingErrors() {
            return colorFormattingErrors;
        }

        public List<String> getOtherErrors() {
            return otherErrors;
        }
    }

    public static ParseResult getDuration(String filePath, String timeFormat) {
        // Map to store the total duration for each color
        Map<String, Long> colorDurationMap = new HashMap<>();
        List<String> colorFormattingErrors = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();

        try {
            // Load the Word document
            InputStream fis = Files.newInputStream(Paths.get(filePath));
            XWPFDocument doc = new XWPFDocument(fis);

            // Define the regex pattern
            String regex = "\\d{2}:\\d{2}:\\d{2},\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2},\\d{3}";
            Pattern pattern = Pattern.compile(regex);

            // Traverse tables
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    XWPFTableCell cell = row.getCell(0); // Get the first column
                    if (cell != null) {
                        String cellText = cell.getText();
                        Matcher matcher = pattern.matcher(cellText);

                        if (matcher.find()) {
                            StringBuilder highlightedText = new StringBuilder();
                            String highlightColor = null;

                            // Traverse paragraphs within the cell
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                for (XWPFRun run : paragraph.getRuns()) {
                                    String text = run.getText(0);
                                    if (text != null) {
                                        // Append run text if it's part of the paragraph text
                                        highlightedText.append(text);
                                        // Get the highlight color value
                                        if (highlightColor == null) {
                                            highlightColor = String.valueOf(run.getTextHighlightColor());
                                        }
                                    }
                                }
                            }

                            String[] timestamps = cellText.split("\\s+-->\\s+");
                            if (timestamps.length == 2 && highlightColor != null && !highlightColor.equals("none")) {
                                long duration = calculateDuration(timestamps[0], timestamps[1]);

                                // Add duration to the corresponding color in the map
                                colorDurationMap.put(highlightColor, colorDurationMap.getOrDefault(highlightColor, 0L) + duration);
                            } else {
                                // Print error if no highlight color is found
                                colorFormattingErrors.add(String.valueOf(highlightedText));
                                System.err.println("Error: Text matches regex but has no highlight color: " + highlightedText);
                            }
                        }
                    }
                }
            }

            // Close the document
            doc.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            otherErrors.add(e.getMessage());
        }

        Map<String, String> colorMap = new HashMap<>();
        for (Map.Entry<String, Long> entry : colorDurationMap.entrySet()) {
            String formattedDuration = formatDuration(entry.getValue(), timeFormat);
            colorMap.put(entry.getKey(), formattedDuration);
            System.out.println("Color: " + entry.getKey() + " -> " + formattedDuration);
        }

        return new ParseResult(colorMap, colorFormattingErrors, otherErrors);
    }

    // Method to calculate the duration between two timestamps
    private static long calculateDuration(String startTimestamp, String endTimestamp) {
        String[] startParts = startTimestamp.split("[:,]");
        String[] endParts = endTimestamp.split("[:,]");

        long startMillis = toMillis(startParts);
        long endMillis = toMillis(endParts);

        return endMillis - startMillis;
    }

    // Convert timestamp parts to milliseconds
    private static long toMillis(String[] parts) {
        long hours = Long.parseLong(parts[0]) * 3600000;
        long minutes = Long.parseLong(parts[1]) * 60000;
        long seconds = Long.parseLong(parts[2]) * 1000;
        long milliseconds = Long.parseLong(parts[3]);

        return hours + minutes + seconds + milliseconds;
    }

    // Method to format the duration in "hours h minutes m seconds s"
    private static String formatDuration(long millis, String timeFormat) {
        long seconds = (long) Math.ceil((double) millis / 1000);
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format(timeFormat, hours, minutes, seconds);
    }
}
