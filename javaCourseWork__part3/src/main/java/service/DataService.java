package service;

import api.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.ApiException;
import format.Formatter;
import model.ApiRecord;
import model.SaveMode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DataService {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ApiRecord fetch(ApiClient api){
        String json = api.getData();
        try {
            JsonNode data = mapper.readTree(json);
            return new ApiRecord(
                    UUID.randomUUID(),
                    api.getName(),
                    Instant.now(),
                    data
            );
        } catch (IOException e) {
            throw new ApiException("Ошибка обработки полученного ответа API", e);
        }
    }

    public void save(ApiClient api, Formatter formatter, Path path, SaveMode mode) {
        ApiRecord record = fetch(api);
        formatter.save(record, path, mode);
    }

}
