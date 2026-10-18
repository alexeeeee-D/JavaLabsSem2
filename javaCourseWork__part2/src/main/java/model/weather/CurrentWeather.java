package model.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CurrentWeather(
        String time,
        int interval,

        @JsonProperty("temperature_2m")
        double temperature
) {}
