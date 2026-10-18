package model.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrentWeatherTest {

    @Test
    void shouldCreateCurrentWeather() {
        CurrentWeather weather = new CurrentWeather(
                "2026-09-22T12:00",
                900,
                20.5
        );

        assertEquals("2026-09-22T12:00", weather.time());
        assertEquals(900, weather.interval());
        assertEquals(20.5, weather.temperature());
    }
}