package api;

public class FootballApi implements ApiClient {

    @Override
    public String getData() {
        String query = "https://api.fc-barcelona.app/api/next-match";;
        return messenger.getBody(query);
    }

    @Override
    public String getName() {
        return "football";
    }
}