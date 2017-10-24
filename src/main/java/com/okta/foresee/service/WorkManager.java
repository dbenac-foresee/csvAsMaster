package com.okta.foresee.service;

import com.okta.foresee.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class WorkManager {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private CsvRowHandler rowHandler;

    private BlockingQueue<CSVRecord> workQueue;

    private ExecutorService service;


    @PostConstruct
    private void init() {
        int numWorkers = appProperties.getNumWorkers();
        workQueue = new LinkedBlockingQueue<>(appProperties.getQueueSize());
        service = Executors.newFixedThreadPool(numWorkers);
        for (int i = 0; i < numWorkers; i++) {
            service.execute(new Worker(workQueue, rowHandler, appProperties.getRunMode()));
        }
    }

    public int workLeft() {
        return workQueue.size();
    }

    public void enqueue(CSVRecord userRow) {
        try {
            workQueue.put(userRow);
        } catch (InterruptedException ex) {
            log.error("Thread {} interrupted.", Thread.currentThread().getName(), ex);
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.debug("Shutting down executor service");
        service.shutdown();
    }
}
