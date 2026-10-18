package model.football;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FootballTest {

    @Test
    void shouldCreateFootballMatch() {
        List<String> periods = List.of("1", "2");

        FootballMatch match = new FootballMatch(
                123,
                "flash-123",
                "2026-09-22",
                1758542400L,
                1,
                periods,
                "Finished",
                90,
                3
        );

        assertEquals(123, match.id());
        assertEquals("flash-123", match.flashId());
        assertEquals("2026-09-22", match.date());
        assertEquals(1758542400L, match.dateUtc());
        assertEquals(1, match.status());
        assertEquals(periods, match.periods());
        assertEquals("Finished", match.statusName());
        assertEquals(90, match.elapsed());
        assertEquals(3, match.extraMinutes());
    }

    @Test
    void shouldCreateFootballResponse() {
        FootballMatch match = new FootballMatch(
                123,
                "flash-123",
                "2026-09-22",
                1758542400L,
                1,
                List.of("1", "2"),
                "Finished",
                90,
                3
        );

        List<FootballMatch> matches = List.of(match);

        FootballResponse response = new FootballResponse(matches);

        assertEquals(matches, response.matches());
        assertEquals(1, response.matches().size());
        assertSame(match, response.matches().get(0));
    }
}