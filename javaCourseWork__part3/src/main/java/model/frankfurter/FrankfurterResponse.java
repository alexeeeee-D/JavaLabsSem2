package model.frankfurter;

public record FrankfurterResponse(
        double amount,
        String base,
        String date,
        Rates rates
) {}
