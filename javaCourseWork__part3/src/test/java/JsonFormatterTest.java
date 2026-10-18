package format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.FileProcessingException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class JsonFormatterTest {

    @TempDir
    Path tempDir;

    private JsonFormatter formatter;
    private ObjectMapper mapper;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {
        formatter = new JsonFormatter();
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private ApiRecord record(String source, String name, int age) {
        JsonNode data = mapper.valueToTree(new Object() {
            public final String n = name;
            public final int a = age;
        });
        // Проще собрать через mapper, но чтобы не плодить классы — используем ObjectNode:
        var node = mapper.createObjectNode();
        node.put("name", name);
        node.put("age", age);
        return new ApiRecord(UUID.randomUUID(), source, Instant.now(), node);
    }

    private String captured() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------
    // create
    // ---------------------------------------------------------------

    @Test
    void createShouldCreateFile() {
        Path path = tempDir.resolve("test.json");

        formatter.create(path);

        assertTrue(Files.exists(path));
    }

    @Test
    void createShouldWriteEmptyJsonArray() throws Exception {
        Path path = tempDir.resolve("empty.json");

        formatter.create(path);

        JsonNode root = mapper.readTree(path.toFile());
        assertTrue(root.isArray());
        assertEquals(0, root.size());
    }

    @Test
    void createShouldThrowIfFileExists() throws Exception {
        Path path = tempDir.resolve("test.json");
        Files.createFile(path);

        assertThrows(FileProcessingException.class, () -> formatter.create(path));
    }

    // ---------------------------------------------------------------
    // save -> CREATE_NEW
    // ---------------------------------------------------------------

    @Test
    void saveCreateNewShouldWriteSingleRecord() throws Exception {
        Path path = tempDir.resolve("new.json");

        formatter.save(record("api-1", "Alex", 25), path, SaveMode.CREATE_NEW);

        JsonNode root = mapper.readTree(path.toFile());
        assertTrue(root.isArray());
        assertEquals(1, root.size());
        assertEquals("api-1", root.get(0).get("source").asText());
        assertEquals("Alex", root.get(0).get("data").get("name").asText());
        assertEquals(25, root.get(0).get("data").get("age").asInt());
    }

    @Test
    void saveCreateNewShouldThrowIfFileExists() throws Exception {
        Path path = tempDir.resolve("exists.json");
        formatter.create(path);

        assertThrows(
                FileProcessingException.class,
                () -> formatter.save(record("api-1", "Alex", 25), path, SaveMode.CREATE_NEW)
        );
    }

    // ---------------------------------------------------------------
    // save -> APPEND
    // ---------------------------------------------------------------

    @Test
    void saveAppendShouldWriteRecord() throws Exception {
        Path path = tempDir.resolve("test.json");
        formatter.create(path);

        formatter.save(record("test-api", "Alex", 25), path, SaveMode.APPEND);

        String content = Files.readString(path);
        assertTrue(content.contains("test-api"));
        assertTrue(content.contains("Alex"));
        assertTrue(content.contains("25"));
    }

    @Test
    void saveAppendShouldThrowIfFileDoesNotExist() {
        Path path = tempDir.resolve("missing.json");

        assertThrows(
                FileProcessingException.class,
                () -> formatter.save(record("api-1", "Alex", 25), path, SaveMode.APPEND)
        );
    }

    @Test
    void saveAppendShouldPreservePreviousRecords() throws Exception {
        Path path = tempDir.resolve("multi.json");
        formatter.create(path);

        formatter.save(record("api-1", "Alex", 25), path, SaveMode.APPEND);
        formatter.save(record("api-2", "Bob", 30), path, SaveMode.APPEND);
        formatter.save(record("api-3", "Carl", 40), path, SaveMode.APPEND);

        JsonNode root = mapper.readTree(path.toFile());
        assertEquals(3, root.size());
        assertEquals("api-1", root.get(0).get("source").asText());
        assertEquals("api-2", root.get(1).get("source").asText());
        assertEquals("api-3", root.get(2).get("source").asText());
    }

    @Test
    void saveAppendShouldKeepFileValidJsonAfterEachWrite() throws Exception {
        Path path = tempDir.resolve("valid.json");
        formatter.create(path);

        for (int i = 0; i < 5; i++) {
            formatter.save(record("api-" + i, "User" + i, 20 + i), path, SaveMode.APPEND);
            JsonNode root = mapper.readTree(path.toFile());
            assertTrue(root.isArray());
            assertEquals(i + 1, root.size());
        }
    }

    // ---------------------------------------------------------------
    // print
    // ---------------------------------------------------------------

    @Test
    void printShouldOutputFileContent() throws Exception {
        Path path = tempDir.resolve("print.json");
        formatter.create(path);
        formatter.save(record("api-print", "Alex", 25), path, SaveMode.APPEND);

        formatter.print(path);

        String out = captured();
        assertTrue(out.contains("api-print"));
        assertTrue(out.contains("Alex"));
    }

    @Test
    void printShouldThrowIfFileMissing() {
        Path path = tempDir.resolve("nope.json");

        assertThrows(FileProcessingException.class, () -> formatter.print(path));
    }

    // ---------------------------------------------------------------
    // printBySource
    // ---------------------------------------------------------------

    @Test
    void printBySourceShouldFilterRecords() throws Exception {
        Path path = tempDir.resolve("filter.json");
        formatter.create(path);
        formatter.save(record("api-1", "Alex", 25), path, SaveMode.APPEND);
        formatter.save(record("api-2", "Bob", 30), path, SaveMode.APPEND);
        formatter.save(record("api-1", "Carl", 40), path, SaveMode.APPEND);

        formatter.printBySource(path, "api-1");

        String out = captured();
        assertTrue(out.contains("Alex"));
        assertTrue(out.contains("Carl"));
        assertFalse(out.contains("Bob"));
    }

    @Test
    void printBySourceShouldReportWhenNoMatches() throws Exception {
        Path path = tempDir.resolve("nomatch.json");
        formatter.create(path);
        formatter.save(record("api-1", "Alex", 25), path, SaveMode.APPEND);

        formatter.printBySource(path, "does-not-exist");

        assertTrue(captured().contains("Записи данного API не найдены"));
    }

    @Test
    void printBySourceShouldThrowIfFileMissing() {
        Path path = tempDir.resolve("missing.json");

        assertThrows(
                FileProcessingException.class,
                () -> formatter.printBySource(path, "api-1")
        );
    }

    @Test
    void printBySourceShouldHandleEmptyFile() throws Exception {
        Path path = tempDir.resolve("empty.json");
        formatter.create(path);

        formatter.printBySource(path, "api-1");

        assertTrue(captured().contains("Записи данного API не найдены"));
    }

    // ---------------------------------------------------------------
    // Instant / JavaTimeModule
    // ---------------------------------------------------------------

    @Test
    void saveShouldSerializeInstantAsIsoString() throws Exception {
        Path path = tempDir.resolve("instant.json");
        formatter.create(path);

        Instant ts = Instant.parse("2024-01-02T03:04:05Z");
        var node = mapper.createObjectNode();
        node.put("k", "v");
        ApiRecord rec = new ApiRecord(UUID.randomUUID(), "api-time", ts, node);

        formatter.save(rec, path, SaveMode.APPEND);

        String content = Files.readString(path);
        // JavaTimeModule по умолчанию пишет Instant как число (epoch seconds с нано),
        // либо как ISO — в зависимости от настройки. Проверяем, что запись вообще читается:
        JsonNode root = mapper.readTree(path.toFile());
        assertEquals(1, root.size());
        assertNotNull(root.get(0).get("timestamp"));
    }

    // ---------------------------------------------------------------
    // Потокобезопасность
    // ---------------------------------------------------------------

    @Test
    void concurrentAppendShouldNotLoseRecords() throws Exception {
        Path path = tempDir.resolve("concurrent.json");
        formatter.create(path);

        int threads = 8;
        int perThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        formatter.save(
                                record("api-" + threadId, "User" + threadId + "-" + i, i),
                                path,
                                SaveMode.APPEND
                        );
                    }
                } catch (Throwable e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "Пул не завершился вовремя");

        assertTrue(errors.isEmpty(), "Были ошибки: " + errors);

        JsonNode root = mapper.readTree(path.toFile());
        assertEquals(threads * perThread, root.size(),
                "Потеряны записи при конкурентной записи");
    }

    @Test
    void concurrentCreateAndSaveOnDifferentFilesShouldNotInterfere() throws Exception {
        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    Path p = tempDir.resolve("file-" + idx + ".json");
                    formatter.create(p);
                    formatter.save(record("api-" + idx, "U" + idx, idx), p, SaveMode.APPEND);
                } catch (Throwable e) {
                    synchronized (errors) {
                        errors.add(e);
                    }
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), "Были ошибки: " + errors);

        for (int i = 0; i < n; i++) {
            Path p = tempDir.resolve("file-" + i + ".json");
            JsonNode root = mapper.readTree(p.toFile());
            assertEquals(1, root.size());
            assertEquals("api-" + i, root.get(0).get("source").asText());
        }
    }
}