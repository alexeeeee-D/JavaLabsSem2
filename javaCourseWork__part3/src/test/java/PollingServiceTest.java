import api.ApiClient;
import format.Formatter;
import model.SaveMode;
import org.junit.jupiter.api.Test;
import service.DataService;
import service.PollingService;
import service.ThreadService;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PollingServiceTest {

    @Test
    void startPollingShouldCreateTaskForEachApi() {

        ThreadService threadService = mock(ThreadService.class);
        DataService dataService = mock(DataService.class);
        Formatter formatter = mock(Formatter.class);

        ScheduledFuture future = mock(ScheduledFuture.class);

        when(threadService.scheduleWithFixedDelay(
                any(Runnable.class),
                eq(0L),
                eq(10L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(future);

        ApiClient api1 = mock(ApiClient.class);
        ApiClient api2 = mock(ApiClient.class);

        PollingService pollingService =
                new PollingService(threadService, dataService);

        pollingService.startPolling(
                List.of(api1, api2),
                formatter,
                Path.of("test.json"),
                SaveMode.APPEND,
                10
        );

        verify(threadService, times(2)).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(0L),
                eq(10L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void stopPollingShouldCancelTasks() {

        ThreadService threadService = mock(ThreadService.class);
        DataService dataService = mock(DataService.class);
        Formatter formatter = mock(Formatter.class);

        ScheduledFuture future = mock(ScheduledFuture.class);

        when(threadService.scheduleWithFixedDelay(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)
        )).thenReturn(future);

        ApiClient api = mock(ApiClient.class);

        PollingService pollingService =
                new PollingService(threadService, dataService);

        pollingService.startPolling(
                List.of(api),
                formatter,
                Path.of("test.json"),
                SaveMode.APPEND,
                10
        );

        pollingService.stopPolling();

        verify(future).cancel(false);
    }
}
