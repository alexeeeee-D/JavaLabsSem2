package model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ApiRecord(
        UUID id,
        String source,
        Instant timestamp,
        JsonNode data
) {}