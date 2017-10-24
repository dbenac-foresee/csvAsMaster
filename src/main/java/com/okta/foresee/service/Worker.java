package com.okta.foresee.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.util.concurrent.BlockingQueue;

@Slf4j
public class Worker implements Runnable {

    private final BlockingQueue<CSVRecord> workQueue;
    private final CsvRowHandler rowHandler;
    private final String runMode;

    public Worker(BlockingQueue<CSVRecord> workQueue, CsvRowHandler rowHandler, String runMode) {
        this.workQueue = workQueue;
        this.rowHandler = rowHandler;
        this.runMode = runMode;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CSVRecord userRow = workQueue.take();
                    if ("delete".equals(runMode)) {
                        rowHandler.deleteUser(userRow);
                    } else {
                        rowHandler.saveUser(userRow);
                    }
                } catch (InterruptedException ex) {
                    log.error("Thread {} was interrupted", Thread.currentThread().getName(), ex);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Exception occurred in worker", e);
        }
    }
}
