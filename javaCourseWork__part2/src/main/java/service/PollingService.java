package service;

import api.ApiClient;
import format.Formatter;
import model.SaveMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PollingService {

    private final ThreadService threadService;
    private final DataService dataService;
    private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();

    public PollingService(ThreadService threadService, DataService dataService) {
        this.threadService = threadService;
        this.dataService = dataService;
    }

    public void startPolling(
            List<ApiClient> apis,
            Formatter formatter,
            Path path,
            SaveMode mode,
            long intervalSeconds
    ) {
        if (mode == SaveMode.CREATE_NEW) {
            formatter.create(path);
        }

        for (ApiClient api : apis) {
            ScheduledFuture<?> future = threadService.scheduleWithFixedDelay(
                    () -> {
                        try {
                            dataService.save(
                                    api,
                                    formatter,
                                    path,
                                    SaveMode.APPEND
                            );
                        } catch(RuntimeException e) {
                            System.err.println("Ошибка при опросе api: " + api.getName() + ":" + e.getMessage());
                        }
                    },
                    0,
                    intervalSeconds,
                    TimeUnit.SECONDS
            );

            scheduledTasks.add(future);
        }
    }

    public void stopPolling() {
        for (ScheduledFuture<?> task : scheduledTasks) {
            task.cancel(false);
        }
        scheduledTasks.clear();
    }

    public void shutdown() {
        stopPolling();
        threadService.shutdown();
    }
}