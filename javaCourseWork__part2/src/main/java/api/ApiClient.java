package api;

public interface ApiClient {
    Messenger messenger = new Messenger();
    String getData();
    String getName();
}
