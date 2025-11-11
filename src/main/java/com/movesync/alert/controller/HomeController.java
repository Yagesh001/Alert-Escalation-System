package com.movesync.alert.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for serving the frontend application
 * Serves index.html at root path without interfering with API routes
 */
@Controller
public class HomeController {

    /**
     * Serve the main frontend page at root
     * Works in both JAR and file system deployments
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public byte[] index() throws IOException {
        Resource resource = new ClassPathResource("static/index.html");
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}


