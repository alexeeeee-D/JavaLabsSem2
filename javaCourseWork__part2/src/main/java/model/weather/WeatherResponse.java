package model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherResponse(
        double latitude,
        double longitude,

        @JsonProperty("generationtime_ms")
        double generationTimeMs,

        @JsonProperty("utc_offset_seconds")
        int utcOffsetSeconds,

        String timezone,

        @JsonProperty("timezone_abbreviation")
        String timezoneAbbreviation,

        double elevation,

        CurrentWeather current
) {}
