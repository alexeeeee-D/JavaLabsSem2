package model.football;

import java.util.List;

public record FootballMatch(
        Integer id,
        String flashId,
        String date,
        Long dateUtc,
        Integer status,
        List<String> periods,
        String statusName,
        Integer elapsed,
        Integer extraMinutes
) {
}