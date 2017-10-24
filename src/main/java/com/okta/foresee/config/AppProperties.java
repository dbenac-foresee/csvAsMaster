package com.okta.foresee.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@EnableConfigurationProperties
@Component
@Getter
public class AppProperties {

    @Autowired
    private Environment env;

    private String oktaUrl;
    private String oktaApiKey;

    private int queueSize;
    private int numWorkers;

    private String workLocation;
    private String logLocation;
    private String csvFile;
    private String runMode;

    @PostConstruct
    private void init() {
        oktaUrl = env.getProperty("okta.url");
        oktaApiKey = env.getProperty("okta.apiKey");
        queueSize = Integer.valueOf(env.getProperty("app.queueSize", "10000"));
        numWorkers = Integer.valueOf(env.getProperty("app.numWorkers", "10"));
        csvFile = env.getProperty("app.csvFile");
        runMode = env.getProperty("app.runMode", "save");
        workLocation = env.getProperty("app.workLocation");
        logLocation = env.getProperty("app.logLocation");

        File f = new File(workLocation);
        if (!f.exists()) {
            f.mkdirs();
        }
        f = new File(logLocation);
        if (!f.exists()) {
            f.mkdirs();
        }
    }
}
