package com.InstaMetrics.controller;

import com.InstaMetrics.model.Account;
import com.InstaMetrics.service.ZipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/instametrics")
// CORS is now configured globally in WebConfig using an environment-backed property.
public class MainController {

    private final ZipService zipService;

    // Inject the ZIP processing service via constructor injection
    public MainController(ZipService zipService) {
        this.zipService = zipService;
    }

    /**
     * Accepts the Instagram data ZIP export, parses it, and stores the parsed result in the session
     * so that subsequent requests can reuse it without re‑uploading the file.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadZip(@RequestParam("file") MultipartFile file, HttpSession session) {
        Map<String, Object> parsedData = zipService.parse(file);
        session.setAttribute("parsedData", parsedData);
        return ResponseEntity.ok("File uploaded successfully");
    }

    /**
     * Returns the list of accounts you follow that do NOT follow you back.
     * Requires that the ZIP has already been uploaded for the current session.
     */
    @GetMapping("/nonfollowers")
    public ResponseEntity<?> getNonFollowers(HttpSession session) {
        Map<String, Object> parsedData = (Map<String, Object>) session.getAttribute("parsedData");
        if (parsedData == null) {
            return ResponseEntity.badRequest().body("No data found. Please upload ZIP first.");
        }
        List<Account> nonFollowers = zipService.findNonFollowers(parsedData);
        return ResponseEntity.ok(nonFollowers);
    }

    /**
     * Returns the list of accounts that you have sent follow requests to,
     * which are still pending.
     */
    @GetMapping("/pendingRequests")
    public ResponseEntity<?> getPendingRequests(HttpSession session) {
        Map<String, Object> parsedData = (Map<String, Object>) session.getAttribute("parsedData");
        if (parsedData == null) {
            return ResponseEntity.badRequest().body("No data found. Please upload ZIP first.");
        }
        List<Account> pendingRequests = zipService.findPendingRequests(parsedData);
        return ResponseEntity.ok(pendingRequests);
    }
}