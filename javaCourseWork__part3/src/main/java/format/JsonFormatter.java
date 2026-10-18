package format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.FileProcessingException;
import model.ApiRecord;
import model.SaveMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class JsonFormatter implements Formatter {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public synchronized void create(Path path) {
        try {
            if (Files.exists(path)) {
                throw new FileProcessingException(
                        "Файл " + path + " уже существует"
                );
            }
            ArrayNode arr = mapper.createArrayNode();
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), arr);
        } catch (IOException e) {
            throw new FileProcessingException("Ошибка создания json-файла");
        }
    }

    @Override
    public synchronized void save(ApiRecord record, Path path, SaveMode mode) {
        try {
            switch (mode) {
                case CREATE_NEW:
                    createNew(record, path);
                    break;
                case APPEND:
                    append(record, path);
                    break;
                default:
                    throw new FileProcessingException("Неизвестный режим сохранения");
            }
        } catch (IOException e) {
            throw new FileProcessingException("Ошибка записи JSON-файла", e);
        }
    }

    private void createNew(ApiRecord record, Path path) throws IOException {
        if (Files.exists(path)) {
            throw new FileProcessingException("Файл уже существует: " + path);
        }
        ArrayNode array = mapper.createArrayNode();
        array.add(mapper.valueToTree(record));
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), array);
    }

    private void append(ApiRecord record, Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileProcessingException("Файл не существует: " + path);
        }
        ArrayNode array = (ArrayNode) mapper.readTree(path.toFile());
        array.add(mapper.valueToTree(record));
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), array);
    }

    private List<ApiRecord> read(Path path) {
        try {
            if (!Files.exists(path)) {
                throw new FileProcessingException("Файл не существует");
            }
            ApiRecord[] records = mapper.readValue(path.toFile(), ApiRecord[].class);
            return Arrays.asList(records);
        } catch (IOException e) {
            throw new FileProcessingException("Ошибка чтения JSON", e);
        }
    }

    @Override
    public void print(Path path) {
        try {
            String content = Files.readString(path);
            System.out.println(content);
        }
        catch (IOException e) {
            throw new FileProcessingException("Ошибка чтения файла");
        }
    }

    @Override
    public void printBySource(Path path, String source) {
        try {
            List<ApiRecord> records = read(path);
            List<ApiRecord> filtered = records.stream()
                    .filter(record -> record.source().equals(source))
                    .toList();
            if (filtered.isEmpty()) {
                System.out.println("Записи данного API не найдены.");
                return;
            }
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(filtered);
            System.out.println(json);
        } catch (IOException e) {
            throw new FileProcessingException("Ошибка вывода JSON-файла");
        }
    }
}