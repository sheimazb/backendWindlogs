package com.windlogs.tickets.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FeignErrorDecoder implements ErrorDecoder {
    private static final Logger logger = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        logger.error("Error occurred while calling Feign client: {} status: {}", methodKey, response.status());
        
        String responseBody = getResponseBody(response);
        logger.error("Response body: {}", responseBody);
        
        switch (response.status()) {
            case 400:
                return new ResponseStatusException(HttpStatus.BAD_REQUEST, responseBody);
            case 401:
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
            case 403:
                return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            case 404:
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
            case 500:
                return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }

    private String getResponseBody(Response response) {
        if (response.body() == null) {
            return "No response body available";
        }
        
        try (InputStream bodyIs = response.body().asInputStream()) {
            byte[] bodyBytes = bodyIs.readAllBytes();
            return new String(bodyBytes, StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            logger.error("Error reading response body", e);
            return "Error reading response body";
        }
    }
} 