package api;

public class WeatherApi implements ApiClient {
    @Override
    public String getData() {
        String query = "https://api.open-meteo.com/v1/forecast?latitude=59.55&longitude=30.20&current=temperature_2m";
        return messenger.getBody(query);
    }

    @Override
    public String getName() {
        return "weather";
    }
}
