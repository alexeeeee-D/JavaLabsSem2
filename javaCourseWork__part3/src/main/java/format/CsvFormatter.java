package format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import exceptions.FileProcessingException;
import model.ApiRecord;
import model.SaveMode;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CsvFormatter implements Formatter {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public synchronized void create(Path path) {
        try {
            if (Files.exists(path)) {
                throw new FileProcessingException(
                        "Файл " + path + " уже существует"
                );
            }
            try (CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()))) {
                writeHeader(writer);
            }
        } catch(IOException e) {
            throw new FileProcessingException("Ошибка создания Csv-файла");
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
            throw new FileProcessingException("Ошибка записи CSV", e);
        }
    }

    private List<ApiRecord> read(Path path) {
        if (!Files.exists(path)) {
            throw new FileProcessingException("Файл не существует");
        }
        List<ApiRecord> records = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            reader.readNext();
            String[] row;
            while ((row = reader.readNext()) != null) {
                ApiRecord record =
                        new ApiRecord(
                                UUID.fromString(row[0]),
                                row[1],
                                Instant.parse(row[2]),
                                mapper.readTree(row[3])
                        );
                records.add(record);
            }
            return records;
        }
        catch (Exception e) {
            throw new FileProcessingException("Ошибка чтения CSV", e);
        }
    }

    @Override
    public void print(Path path) {
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                for (String value : row) {
                    System.out.printf("%-36s", value);
                }
                System.out.println();
            }
        } catch (Exception e) {
            throw new FileProcessingException("Ошибка чтения CSV");
        }
    }

    @Override
    public void printBySource(Path path, String source) {
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                return;
            }
            System.out.println(
                    String.join(";", headers)
            );
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length > 1 &&
                        source.equals(row[1])) {
                    System.out.println(
                            String.join(";", row)
                    );
                }
            }
        } catch (Exception e) {
            throw new FileProcessingException("Ошибка чтения CSV-файла");
        }
    }

    private void createNew(ApiRecord record, Path path) throws IOException {
        if (Files.exists(path)) {
            throw new FileProcessingException("Файл уже существует");
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()))) {
            writeHeader(writer);
            writeRecord(writer, record);
        }
    }

    private void append(ApiRecord record, Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileProcessingException("Файл не существует");
        }
        List<ApiRecord> records = read(path);
        records.add(record);
        rewriteFile(records, path);
    }

    private void rewriteFile(List<ApiRecord> records, Path path) throws IOException {
        try(CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()))) {
            writeHeader(writer);
            for(ApiRecord record :records){
                writeRecord(writer, record);
            }
        }
    }

    private void writeHeader(CSVWriter writer) {
        writer.writeNext(
                new String[]{
                        "id",
                        "source",
                        "timestamp",
                        "data"
                }
        );
    }

    private void writeRecord(CSVWriter writer, ApiRecord record) {
        writer.writeNext(
                new String[]{
                        record.id().toString(),
                        record.source(),
                        record.timestamp().toString(),
                        record.data().toString()
                }
        );
    }
}