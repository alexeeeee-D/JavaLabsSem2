package org;

import exceptions.ApiException;
import exceptions.FileProcessingException;
import format.Formatter;
import model.SaveMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.DataService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsoleUITest {

    @Mock
    private DataService dataService;

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;

    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUp() {
        outputCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapture, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private void setInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    private String getOutput() {
        return outputCapture.toString(StandardCharsets.UTF_8);
    }

    @Test
    void runAutoMode_shouldPrintUsage_whenTooFewArgs() {
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"weather"});

        assertTrue(getOutput().contains("Неверное количество параметров"));
        verifyNoInteractions(dataService);
    }

    @Test
    void runAutoMode_shouldSaveApi_whenValidArgs(@TempDir Path tempDir) {
        Path file = tempDir.resolve("result.json");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"weather", file.toString(), "new"});

        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.CREATE_NEW));
        assertTrue(getOutput().contains("сохранён"));
    }

    @Test
    void runAutoMode_shouldSwitchToAppend_whenMultipleApis(@TempDir Path tempDir) {
        Path file = tempDir.resolve("result.json");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"weather", "frankfurter", file.toString(), "new"});

        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.CREATE_NEW));
        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.APPEND));
    }

    @Test
    void runAutoMode_shouldPrintUnknownApi_whenInvalidApiName(@TempDir Path tempDir) {
        Path file = tempDir.resolve("result.json");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"unknownApi", file.toString(), "new"});

        assertTrue(getOutput().contains("Неизвестный API"));
    }

    @Test
    void runAutoMode_shouldPrintError_whenUnknownMode(@TempDir Path tempDir) {
        Path file = tempDir.resolve("result.json");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"weather", file.toString(), "badmode"});

        assertTrue(getOutput().contains("Неизвестный режим"));
        verifyNoInteractions(dataService);
    }

    @Test
    void runAutoMode_shouldPrintApiError_whenServiceThrows(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("result.json");
        doThrow(new ApiException("boom"))
                .when(dataService).save(any(), any(Formatter.class), eq(file), any(SaveMode.class));
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runAutoMode(new String[]{"weather", file.toString(), "new"});

        assertTrue(getOutput().contains("Ошибка API: boom"));
    }

    @Test
    void runAutoMode_shouldThrowFileProcessingException_whenBadExtension(@TempDir Path tempDir) {
        Path file = tempDir.resolve("result.txt");
        ConsoleUI ui = new ConsoleUI(dataService);

        assertThrows(FileProcessingException.class,
                () -> ui.runAutoMode(new String[]{"weather", file.toString(), "new"}));
    }

    @Test
    void runInteractiveMode_shouldExit_whenChoiceIs4() {
        setInput("4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Выберите действие"));
        verifyNoInteractions(dataService);
    }

    @Test
    void runInteractiveMode_shouldPrintError_whenInvalidMenuChoice() {
        setInput("99\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Неверный пункт меню"));
    }

    @Test
    void runInteractiveMode_shouldSaveViaApi_whenValidInput(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("out.json");
        setInput("1\n1\n" + file + "\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.CREATE_NEW));
        assertTrue(getOutput().contains("Успешно сохранено"));
    }

    @Test
    void runInteractiveMode_shouldGoBack_whenChoiceApiIs4() {
        setInput("1\n4\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        verifyNoInteractions(dataService);
    }

    @Test
    void runInteractiveMode_showFile_shouldPrintAllContent(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.json");
        Files.writeString(file, "[]");
        setInput("3\n" + file + "\n1\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertFalse(getOutput().contains("Ошибка:"));
    }

    @Test
    void runInteractiveMode_showFile_shouldGoBack_whenChoice3() {
        setInput("3\ndata.json\n3\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        verifyNoInteractions(dataService);
    }

    @Test
    void runInteractiveMode_shouldReprompt_whenInputIsNotNumber() {
        setInput("abc\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Введите число!"));
    }

    @Test
    void parallelPoll_shouldRejectNonPositiveThreads() {
        setInput("2\n0\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Количество потоков должно быть больше нуля"));
    }

    @Test
    void parallelPoll_shouldRejectNegativeInterval(@TempDir Path tempDir) {
        Path file = tempDir.resolve("poll.json");
        setInput("2\n1\n-5\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Интервал не может быть отрицательным"));
    }

    @Test
    void runInteractiveMode_shouldAskAppend_whenFileExists_andAnswerYes(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("existing.json");
        Files.writeString(file, "[]");
        setInput("1\n1\n" + file + "\nда\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.APPEND));
    }

    @Test
    void runInteractiveMode_shouldCancel_whenFileExists_andAnswerNo(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("existing.json");
        Files.writeString(file, "[]");
        setInput("1\n1\n" + file + "\nнет\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        verifyNoInteractions(dataService);
    }

    @Test
    void runInteractiveMode_shouldReprompt_whenAnswerNotYesNo(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("existing.json");
        Files.writeString(file, "[]");
        setInput("1\n1\n" + file + "\nmaybe\nда\n4\n");
        ConsoleUI ui = new ConsoleUI(dataService);

        ui.runInteractiveMode();

        assertTrue(getOutput().contains("Введите да или нет"));
        verify(dataService).save(any(), any(Formatter.class), eq(file), eq(SaveMode.APPEND));
    }
}