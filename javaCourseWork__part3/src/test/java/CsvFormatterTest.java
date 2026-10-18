import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import format.CsvFormatter;
import model.ApiRecord;
import model.SaveMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CsvFormatterTest {

    @TempDir
    Path tempDir;

    private final CsvFormatter formatter = new CsvFormatter();
    private final ObjectMapper mapper = new ObjectMapper();

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUp() {
        outputCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapture, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String getOutput() {
        return outputCapture.toString(StandardCharsets.UTF_8);
    }

    private ApiRecord record(String source, String json) throws Exception {
        JsonNode data = mapper.readTree(json);
        return new ApiRecord(UUID.randomUUID(), source, Instant.now(), data);
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void createShouldCreateFile() {
        Path path = tempDir.resolve("test.csv");

        formatter.create(path);

        assertTrue(Files.exists(path));
    }

    @Test
    void createShouldWriteHeader() throws Exception {
        Path path = tempDir.resolve("test.csv");

        formatter.create(path);

        String content = Files.readString(path);
        assertTrue(content.contains("id"));
        assertTrue(content.contains("source"));
        assertTrue(content.contains("timestamp"));
        assertTrue(content.contains("data"));
    }

    @Test
    void createShouldThrowIfFileExists() throws Exception {
        Path path = tempDir.resolve("test.csv");
        Files.createFile(path);

        assertThrows(RuntimeException.class, () -> formatter.create(path));
    }

    // ------------------------------------------------------------------
    // save — CREATE_NEW
    // ------------------------------------------------------------------

    @Test
    void saveCreateNewShouldWriteRecord() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r = record("test-api", "{\"name\":\"Alex\",\"age\":25}");

        formatter.save(r, path, SaveMode.CREATE_NEW);

        String content = Files.readString(path);
        assertTrue(content.contains("test-api"));
        assertTrue(content.contains(r.id().toString()));
    }

    @Test
    void saveCreateNewShouldThrowIfFileExists() throws Exception {
        Path path = tempDir.resolve("test.csv");
        Files.createFile(path);
        ApiRecord r = record("test-api", "{}");

        assertThrows(RuntimeException.class,
                () -> formatter.save(r, path, SaveMode.CREATE_NEW));
    }

    // ------------------------------------------------------------------
    // save — APPEND
    // ------------------------------------------------------------------

    @Test
    void saveShouldWriteRecord() throws Exception {
        Path path = tempDir.resolve("test.csv");
        formatter.create(path);

        ApiRecord r = record("test-api", "{\"name\":\"Alex\",\"age\":25}");

        formatter.save(r, path, SaveMode.APPEND);

        String content = Files.readString(path);
        assertTrue(content.contains("test-api"));
    }

    @Test
    void appendShouldAddSecondRecord() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r1 = record("weather", "{\"temp\":10}");
        ApiRecord r2 = record("frankfurter", "{\"EUR\":1.08}");

        formatter.save(r1, path, SaveMode.CREATE_NEW);
        formatter.save(r2, path, SaveMode.APPEND);

        String content = Files.readString(path);
        assertTrue(content.contains("weather"));
        assertTrue(content.contains("frankfurter"));
        assertTrue(content.contains(r1.id().toString()));
        assertTrue(content.contains(r2.id().toString()));
    }

    @Test
    void appendShouldThrowIfFileDoesNotExist() throws Exception {
        Path path = tempDir.resolve("missing.csv");
        ApiRecord r = record("weather", "{}");

        assertThrows(RuntimeException.class,
                () -> formatter.save(r, path, SaveMode.APPEND));
    }

    // ------------------------------------------------------------------
    // print
    // ------------------------------------------------------------------

    @Test
    void printShouldThrowWhenFileDoesNotExist() {
        Path path = tempDir.resolve("missing.csv");

        assertThrows(RuntimeException.class, () -> formatter.print(path));
    }

    @Test
    void printShouldPrintHeaderAndRows() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r1 = record("weather", "{\"temp\":10}");
        ApiRecord r2 = record("frankfurter", "{\"EUR\":1.08}");

        formatter.save(r1, path, SaveMode.CREATE_NEW);
        formatter.save(r2, path, SaveMode.APPEND);

        formatter.print(path);

        String output = getOutput();
        assertTrue(output.contains("id"));
        assertTrue(output.contains("source"));
        assertTrue(output.contains("weather"));
        assertTrue(output.contains("frankfurter"));
        assertTrue(output.contains(r1.id().toString()));
        assertTrue(output.contains(r2.id().toString()));
    }

    @Test
    void printEmptyFileShouldNotFail() throws Exception {
        Path path = tempDir.resolve("empty.csv");
        Files.writeString(path, "");

        formatter.print(path);

        assertEquals("", getOutput().trim());
    }

    // ------------------------------------------------------------------
    // printBySource
    // ------------------------------------------------------------------

    @Test
    void printBySourceShouldThrowWhenFileDoesNotExist() {
        Path path = tempDir.resolve("missing.csv");

        assertThrows(RuntimeException.class,
                () -> formatter.printBySource(path, "weather"));
    }

    @Test
    void printBySourceShouldPrintOnlyMatchingRows() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r1 = record("weather", "{\"temp\":10}");
        ApiRecord r2 = record("frankfurter", "{\"EUR\":1.08}");
        ApiRecord r3 = record("weather", "{\"temp\":20}");

        formatter.save(r1, path, SaveMode.CREATE_NEW);
        formatter.save(r2, path, SaveMode.APPEND);
        formatter.save(r3, path, SaveMode.APPEND);

        formatter.printBySource(path, "weather");

        String output = getOutput();
        assertTrue(output.contains("weather"));
        assertTrue(output.contains(r1.id().toString()));
        assertTrue(output.contains(r3.id().toString()));
        assertFalse(output.contains(r2.id().toString()));
        assertFalse(output.contains("frankfurter"));
    }

    @Test
    void printBySourceShouldAlwaysPrintHeader() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r1 = record("weather", "{\"temp\":10}");

        formatter.save(r1, path, SaveMode.CREATE_NEW);

        formatter.printBySource(path, "unknownSource");

        String output = getOutput();
        assertTrue(output.contains("id"));
        assertTrue(output.contains("source"));
        assertFalse(output.contains(r1.id().toString()));
    }

    @Test
    void printBySourceShouldHandleEmptyFile() throws Exception {
        Path path = tempDir.resolve("empty.csv");
        Files.writeString(path, "");

        formatter.printBySource(path, "weather");

        assertEquals("", getOutput().trim());
    }

    // ------------------------------------------------------------------
    // round-trip
    // ------------------------------------------------------------------

    @Test
    void savedRecordShouldBeReadableBack() throws Exception {
        Path path = tempDir.resolve("test.csv");
        ApiRecord r = record("weather", "{\"temp\":10}");

        formatter.save(r, path, SaveMode.CREATE_NEW);
        formatter.print(path);

        String output = getOutput();
        assertTrue(output.contains(r.id().toString()));
        assertTrue(output.contains("weather"));
    }
}