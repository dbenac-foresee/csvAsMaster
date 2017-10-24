package com.okta.foresee;

import com.google.common.base.Stopwatch;
import com.okta.foresee.csv.CsvImporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ComponentScan(value = "com.okta.foresee")
@Slf4j
public class Application  {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);

        Environment env = ctx.getBean(Environment.class);

        log.debug("CSVImportsMain Starting...");
        Stopwatch stopwatch = Stopwatch.createStarted();

        CsvImporter ci = ctx.getBean(CsvImporter.class);
        ci.executeImport();

        log.debug( "Took {} to complete", stopwatch);
        System.exit(0);
    }

}
