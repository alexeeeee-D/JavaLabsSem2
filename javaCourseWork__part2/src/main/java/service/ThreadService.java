package service;

import java.util.concurrent.*;

public class ThreadService {
    private final ScheduledExecutorService executor;
    public ThreadService(int maxThreads) {
        this.executor = Executors.newScheduledThreadPool(maxThreads);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable task,
            long initialDelay,
            long delay,
            TimeUnit unit
    ) {
        return executor.scheduleWithFixedDelay(
                task,
                initialDelay,
                delay,
                unit
        );
    }

    public void shutdown() {
        executor.shutdown();
    }
}
