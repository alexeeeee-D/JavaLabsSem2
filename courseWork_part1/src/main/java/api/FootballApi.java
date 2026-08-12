package api;

public class FootballApi implements ApiClient {

    @Override
    public String getData() {
        String query =
                "https://api.sstats.net/games/list" +
                        "?upcoming=true" +
                        "&team=529" +
                        "&limit=10";

        return messenger.getBody(query);
    }

    @Override
    public String getName() {
        return "football";
    }
}