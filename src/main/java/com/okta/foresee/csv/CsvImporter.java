package com.okta.foresee.csv;

import com.google.common.collect.Maps;
import com.okta.foresee.config.AppProperties;
import com.okta.foresee.domain.ForeseeConstants;
import com.okta.foresee.domain.okta.OktaGroup;
import com.okta.foresee.service.OktaService;
import com.okta.foresee.service.WorkManager;
import com.okta.foresee.util.ForeseeCSVUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.okta.foresee.domain.ForeseeConstants.*;

/**
 * @author Sivaji Sabbineni
 */
@Slf4j
@Component
public class CsvImporter {
    public static Map<String, OktaGroup> groupsMap = Maps.newConcurrentMap();
    private static List<String> processedUserNames = new ArrayList<String>();

    @Autowired
    private WorkManager workManager;

    @Autowired
    private OktaService oktaService;

    @Autowired
    private ForeseeCSVUtil foreseeCSVUtil;

    @Autowired
    private AppProperties appProperties;

    public void executeImport() {

        try (Reader in = new FileReader(appProperties.getCsvFile())){

            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

            groupsMap = oktaService.getAllGroups();
            log.debug("Existing Groups: {} ", groupsMap);

            int recCount = 0;
            for (CSVRecord record : records) {
                recCount++;
                String id = record.get(FORESEE_ID);
                String username = oktaService.getOktaLogin(record.get(USERNAME), record.get(USERNAME_SUFFIX));

                if (processedUserNames.contains(username)) {
                    log.debug("Skipping user {}. Username has already been processed", username);
                    foreseeCSVUtil.writeToFile(id + "," + username +
                                    " , user with this username has already been processed"
                            , USER_FAILURE_FILE);
                    continue;
                } else {
                    processedUserNames.add(username);
                }
                String groupName = ForeseeConstants.GROUP_PREFIX + record.get(CLIENT_ID);

                String groupDescription = "Group for " + record.get(NAME).trim();

                if (!groupsMap.containsKey(groupName)) {
                    OktaGroup updatedOktaGroup = oktaService.createGroup(groupName, groupDescription);
                    if (updatedOktaGroup != null) {
                        groupsMap.put(groupName, updatedOktaGroup);
                    }
                }
                workManager.enqueue(record);
            }
            log.debug("CSV contains {} lines.", recCount);
        } catch (Exception e) {
            log.error("Exception trying to process csv.", e);
            throw new RuntimeException(e);
        }

        while (true) {
            int owl = workManager.workLeft();
            log.debug("There are {} rows left to process", owl);
            if (owl == 0) {
                sleepThread(10 ); // let the current processing finish
                File f = new File(USER_FAILURE_FILE);
                File f2 = new File(OTHER_FAILURE_FILE);
                if (f.exists() || f2.exists()) {
                    log.debug("Errors exist. Check failure logs!");
                } else {
                    log.debug("No errors");
                }
                log.debug("Load Completed!");
                break;
            }
            sleepThread(10);
        }
    }

    private void sleepThread(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (Exception e) {
            log.error("Exception trying to sleep thread {}", Thread.currentThread().getName(), e);
        }
    }


}