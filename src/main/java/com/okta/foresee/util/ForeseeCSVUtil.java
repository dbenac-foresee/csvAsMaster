/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.foresee.util;

import com.google.common.collect.Maps;
import com.okta.foresee.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

import static com.okta.foresee.domain.ForeseeConstants.*;

/**
 * @author Sivaji Sabbineni
 */
@Slf4j
@Component
public class ForeseeCSVUtil {

    private static Map<String, Object> FILE_LOCKS = Maps.newHashMap();

    static {
        FILE_LOCKS.put(OPERATION_SUCCESS_FILE, new Object());
        FILE_LOCKS.put(USER_SAVE_SUCCESS_FILE, new Object());
        FILE_LOCKS.put(USER_FAILURE_FILE, new Object());
        FILE_LOCKS.put(USER_ID_LOG_FILE, new Object());
        FILE_LOCKS.put(OTHER_FAILURE_FILE, new Object());
    }

    @Autowired
    private AppProperties appProperties;

    public void writeToFile (String message, String fileName) {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(new File(appProperties.getLogLocation() + "/" + fileName), true))) {
            synchronized (FILE_LOCKS.get(fileName)) {
                pw.println(message + ", " + new Date());
            }
        } catch (Exception e) {
            log.error("Failed to record success. Trying to record {}", message, e);
        }
    }

    public boolean isValidUserNameFormat(String login) {
//        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        String ePattern = "[\\p{L}\\p{Digit}\\-\\!\\#\\$\\%\\&\\*\\+\\-\\/\\=\\?\\^\\_\\{\\|\\}]+(?:\\.[\\p{L}\\p{Digit}\\-\\!\\#\\$\\%\\&\\*\\+\\-\\/\\=\\?\\^\\_\\{\\|\\}]+)*@(?:[\\p{L}\\p{Digit}](?:[\\p{L}\\p{Digit}-]*[\\p{L}\\p{Digit}])?\\.)+[\\p{L}]{2,20}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(login);
        return m.matches();
    }

}