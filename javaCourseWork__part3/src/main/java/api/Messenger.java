package api;

import exceptions.ApiException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.time.Duration;

public class Messenger {

    private final OkHttpClient client;

    public Messenger() {
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMinutes(1))
                .build();
    }

    public String getBody(String url) {

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ApiException("Сервер вернул код: " + response.code());
            }
            if (response.body() == null) {
                throw new ApiException("Сервер вернул пустой ответ");
            }
            return response.body().string();
        } catch (IOException e) {
            throw new ApiException("Ошибка при выполнении HTTP-запроса", e);
        }
    }
}
