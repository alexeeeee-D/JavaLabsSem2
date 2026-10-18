import api.ApiClient;
import exceptions.ApiException;
import format.Formatter;
import model.SaveMode;
import org.junit.jupiter.api.Test;
import service.DataService;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

public class DataServiceTest {

    @Test
    void saveShouldCallApiAndFormatter() {

        ApiClient api = mock(ApiClient.class);
        Formatter formatter = mock(Formatter.class);

        when(api.getData()).thenReturn(
                "{\"name\":\"Alex\",\"age\":25}"
        );

        when(api.getName()).thenReturn("test-api");

        DataService service = new DataService();

        Path path = Path.of("test.json");

        service.save(
                api,
                formatter,
                path,
                SaveMode.APPEND
        );

        verify(api).getData();
        verify(api).getName();

        verify(formatter).save(
                any(),
                eq(path),
                eq(SaveMode.APPEND)
        );
    }

    @Test
    void saveShouldThrowExceptionForInvalidJson() {

        ApiClient api = mock(ApiClient.class);
        Formatter formatter = mock(Formatter.class);

        when(api.getData()).thenReturn("invalid json");

        DataService service = new DataService();

        assertThrows(
                ApiException.class,
                () -> service.save(
                        api,
                        formatter,
                        Path.of("test.json"),
                        SaveMode.APPEND
                )
        );

        verify(formatter, never()).save(
                any(),
                any(),
                any()
        );
    }
}