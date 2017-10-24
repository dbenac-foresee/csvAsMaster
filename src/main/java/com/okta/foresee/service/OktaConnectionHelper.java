package com.okta.foresee.service;

import com.okta.foresee.config.AppProperties;
import com.okta.foresee.domain.ApiResponse;
import com.okta.foresee.domain.ForeseeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.okta.foresee.domain.ForeseeConstants.*;

@Slf4j
@Component
public class OktaConnectionHelper {
    //per minute api rate limits
    private static final int LIMIT_FOR_USER_CREATE = 600;
    private static final int LIMIT_FOR_USER_UPDATE = 600;
    private static final int LIMIT_FOR_GROUP_CREATE = 500;
    private static final int LIMIT_FOR_GROUP_UPDATE = 1000;
    private static AtomicInteger RATE_LIMIT_USER_CREATE = new AtomicInteger(LIMIT_FOR_USER_CREATE);
    private static AtomicInteger RATE_LIMIT_USER_UPDATE = new AtomicInteger(LIMIT_FOR_USER_UPDATE);
    private static AtomicInteger RATE_LIMIT_GROUP_CREATE = new AtomicInteger(LIMIT_FOR_GROUP_CREATE);
    private static AtomicInteger RATE_LIMIT_GROUP_UPDATE = new AtomicInteger(LIMIT_FOR_GROUP_UPDATE);
    private static AtomicLong RATE_RESET_USER_CREATE = new AtomicLong();
    private static AtomicLong RATE_RESET_USER_UPDATE = new AtomicLong();
    private static AtomicLong RATE_RESET_GROUP_CREATE = new AtomicLong();
    private static AtomicLong RATE_RESET_GROUP_UPDATE = new AtomicLong();

    @Autowired
    private AppProperties appProperties;

    public ApiResponse get(String resource) {
        checkRateLimit(resource);
        ApiResponse response = new ApiResponse();
        HttpURLConnection conn;

        try {
            URL url = new URL(resource);

            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + appProperties.getOktaApiKey());
            conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
            conn.setRequestProperty(ForeseeConstants.ACCEPT, ForeseeConstants.APP_JSON);
            conn.setRequestMethod(ForeseeConstants.METHOD_GET);

            response.responseCode = conn.getResponseCode();

            if (response.responseCode == 200) {
                response.output = streamToString(conn.getInputStream());
                if (response.output.equals("[]")) {
                    response.emptyOutput = Boolean.TRUE;
                } else {
                    response.emptyOutput = Boolean.FALSE;
                }
            } else {
                response.errorOutput = streamToString(conn.getErrorStream());
            }
            updateRateLimit(conn, resource);
            conn.disconnect();
        } catch (IOException e) {
            response.exception = e.getMessage();
        }
        return response;
    }


    public ApiResponse put(String resource, String data) {
        checkRateLimit(resource);
        ApiResponse response = new ApiResponse();
        try {
            URL url = new URL(resource);
            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            if (data != null) {
                conn.setDoOutput(true);
            }
            conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + appProperties.getOktaApiKey());
            conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
            conn.setRequestMethod(METHOD_PUT);

            response.responseCode = conn.getResponseCode();
            if (response.responseCode == 201 || response.responseCode == 200) {
                response.output = streamToString(conn.getInputStream());
            } else if (response.responseCode != 204) {
                response.errorOutput = streamToString(conn.getErrorStream());
            }
            updateRateLimit(conn, resource);
            conn.disconnect();
        } catch (IOException e) {
            response.exception = e.getMessage();
        }
        return response;
    }

    public ApiResponse delete(String resource, String data) {
        checkRateLimit(resource);
        ApiResponse response = new ApiResponse();
        try {
            URL url = new URL(resource);

            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setDoOutput(true);
            conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + appProperties.getOktaApiKey());
            conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
            conn.setRequestMethod(METHOD_DELETE);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            response.responseCode = conn.getResponseCode();

            if (response.responseCode == 201 || response.responseCode == 200) {
                response.output = streamToString(conn.getInputStream());
                log.debug("------------ret.output-------" + response.output);
            } else if (response.responseCode != 204) {
                response.errorOutput = streamToString(conn.getErrorStream());
                log.debug("------------error==" + response.errorOutput);
            }
            updateRateLimit(conn, resource);
            conn.disconnect();

        } catch (IOException e) {
            response.exception = e.getMessage();
        }
        return response;
    }

    public ApiResponse post(String resource, String data) {
        checkRateLimit(resource);
        ApiResponse response = new ApiResponse();
        try {
            URL url = new URL(resource);

            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setDoOutput(true);
            conn.setRequestProperty(ForeseeConstants.AUTHZ_PROPERTY, ForeseeConstants.SSWS + appProperties.getOktaApiKey());
            conn.setRequestProperty(ForeseeConstants.CONTENT_TYPE, ForeseeConstants.APP_JSON);
            conn.setRequestMethod(ForeseeConstants.METHOD_POST);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            response.responseCode = conn.getResponseCode();

			log.debug("------------ret.responseCode -------"+response.responseCode );

            if (response.responseCode == 201 || response.responseCode == 200) {
                response.output = streamToString(conn.getInputStream());
                log.debug("------------ret.output-------" + response.output);
            } else {
                response.errorOutput = streamToString(conn.getErrorStream());
                log.debug("------------error==" + response.errorOutput);
            }
            updateRateLimit(conn, resource);
            conn.disconnect();
        } catch (IOException e) {
            response.exception = e.getMessage();
        }
        return response;
    }

    private String streamToString(InputStream s) throws IOException {
        if (s != null) {
            String line;
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(s))) {
                StringBuilder sb = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
        return null;
    }

    private synchronized void checkRateLimit(String urlCalled) {
        log.debug("Rate limits - User Create: {}, User Update: {}, Group Create: {}, Group Update: {}",
                RATE_LIMIT_USER_CREATE.get(),
                RATE_LIMIT_USER_UPDATE.get(),
                RATE_LIMIT_GROUP_CREATE.get(),
                RATE_LIMIT_GROUP_UPDATE.get());

        int rateLimitBuffer = 20;
        if (urlCalled.contains("/v1/users?") && RATE_LIMIT_USER_CREATE.get() <= rateLimitBuffer) {
            sleepThread(RATE_RESET_USER_CREATE.get());
            RATE_LIMIT_USER_CREATE.set(LIMIT_FOR_USER_CREATE);
        } else if (urlCalled.endsWith("/api/v1/groups") && RATE_LIMIT_GROUP_CREATE.get() <= rateLimitBuffer) {
            sleepThread(RATE_RESET_GROUP_CREATE.get());
            RATE_LIMIT_GROUP_CREATE.set(LIMIT_FOR_GROUP_CREATE);
        } else if (urlCalled.contains(URL_SUFFIX_USERS) && RATE_LIMIT_USER_UPDATE.get() <= rateLimitBuffer) {
            sleepThread(RATE_RESET_USER_UPDATE.get());
            RATE_LIMIT_USER_UPDATE.set(LIMIT_FOR_USER_UPDATE);
        } else if (urlCalled.contains(URL_SUFFIX_GROUPS) && RATE_LIMIT_GROUP_UPDATE.get() <= rateLimitBuffer) {
            sleepThread(RATE_RESET_GROUP_UPDATE.get());
            RATE_LIMIT_GROUP_UPDATE.set(LIMIT_FOR_GROUP_UPDATE);
        }
    }

    private synchronized void updateRateLimit(HttpURLConnection conn, String urlCalled) {
        String newRateLimit = conn.getHeaderField(ForeseeConstants.RATE_LIMIT_REMAINING);
        Long resetTime = Long.valueOf(conn.getHeaderField(ForeseeConstants.RATE_LIMIT_RESET));
        if (newRateLimit != null && newRateLimit.length() > 0) {
            Integer rateLimit = Integer.parseInt(newRateLimit);
            if (urlCalled.contains("/v1/users?") && rateLimit < RATE_LIMIT_USER_CREATE.get()) {
                RATE_LIMIT_USER_CREATE.set(rateLimit);
                RATE_RESET_USER_CREATE.set(resetTime);
            } else if (urlCalled.endsWith("/api/v1/groups") && rateLimit < RATE_LIMIT_GROUP_CREATE.get()) {
                RATE_LIMIT_GROUP_CREATE.set(rateLimit);
                RATE_RESET_GROUP_CREATE.set(resetTime);
            } else if (urlCalled.contains(URL_SUFFIX_USERS) && rateLimit < RATE_LIMIT_USER_UPDATE.get()) {
                RATE_LIMIT_USER_UPDATE.set(rateLimit);
                RATE_RESET_USER_UPDATE.set(resetTime);
            } else if (urlCalled.contains(URL_SUFFIX_GROUPS) && rateLimit < RATE_LIMIT_GROUP_UPDATE.get()) {
                RATE_LIMIT_GROUP_UPDATE.set(rateLimit);
                RATE_RESET_GROUP_UPDATE.set(resetTime);
            }
        }
    }

    private void sleepThread(long resetTime) {

        long timeOut = resetTime * 1000 - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() + 2000;
        if (timeOut < 0) {
            timeOut = 1000;
        }
        log.debug("Rate limit exceeded. Waiting for " + timeOut + " millis for rate limit reset");
        try {
            Thread.sleep(timeOut);
        } catch (InterruptedException e) {
            log.error("Exception thrown trying to sleep thread {}", Thread.currentThread().getName(), e);
        }
    }

}
