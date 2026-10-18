package api;

public class FrankfurterApi implements ApiClient {

    @Override
    public String getData() {
        String query = "https://api.frankfurter.app/latest?from=USD&to=CNY,EUR";
        return messenger.getBody(query);
    }

    @Override
    public String getName() {
        return "frankfurter";
    }
}
