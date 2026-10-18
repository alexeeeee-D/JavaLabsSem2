package model.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherResponseTest {

    @Test
    void shouldCreateWeatherResponse() {
        CurrentWeather current = new CurrentWeather(
                "2026-09-22T12:00",
                900,
                20.5
        );

        WeatherResponse response = new WeatherResponse(
                55.75,
                37.61,
                0.15,
                10800,
                "Europe/Moscow",
                "MSK",
                156.0,
                current
        );

        assertEquals(55.75, response.latitude());
        assertEquals(37.61, response.longitude());
        assertEquals(0.15, response.generationTimeMs());
        assertEquals(10800, response.utcOffsetSeconds());
        assertEquals("Europe/Moscow", response.timezone());
        assertEquals("MSK", response.timezoneAbbreviation());
        assertEquals(156.0, response.elevation());
        assertSame(current, response.current());
    }
}