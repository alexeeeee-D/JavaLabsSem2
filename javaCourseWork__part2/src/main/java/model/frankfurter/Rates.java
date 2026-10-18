package model.frankfurter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Rates(

        @JsonProperty("EUR")
        double eur,

        @JsonProperty("CNY")
        double cny

) {}
