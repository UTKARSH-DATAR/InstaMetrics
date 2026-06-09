package com.InstaMetrics.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.InstaMetrics.model.Account;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipParser {

    public static Map<String, Object> extractData(MultipartFile file) {
        Map<String, Object> data = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        File tempFile = null;

        try {
            // Save MultipartFile to a temporary file so it can be consumed by java.util.zip.ZipFile
            tempFile = File.createTempFile("insta", ".zip");
            file.transferTo(tempFile);

            try (ZipFile zipFile = new ZipFile(tempFile)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String path = entry.getName();

                    if (path.toLowerCase().contains("followers_1.json")) {
                        List<Account> followers = parseAccounts(zipFile.getInputStream(entry), mapper);
                        data.put("followers", followers);
                    } else if (path.toLowerCase().contains("following.json")) {
                        List<Account> following = parseAccounts(zipFile.getInputStream(entry), mapper);
                        data.put("following", following);
                    } else if (path.toLowerCase().contains("pending_follow_requests.json")) {
                        List<Account> pending = parseAccounts(zipFile.getInputStream(entry), mapper);
                        data.put("pendingRequests", pending);
                    }
                }
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error parsing ZIP file. Make sure it is a valid Instagram data export.", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return data;
    }

    private static List<Account> parseAccounts(InputStream is, ObjectMapper mapper) throws IOException {
        List<Account> accounts = new ArrayList<>();

        Object root = mapper.readValue(is, Object.class);

        if (root instanceof List) {
            // followers.json – file is a top‑level JSON array
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) root;
            for (Map<String, Object> obj : rawList) {
                extractAccount(obj, accounts);
            }
        } else if (root instanceof Map) {
            Map<String, Object> rootMap = (Map<String, Object>) root;

            // pending_follow_requests.json (legacy) – accounts under "relationships_follow_requests_sent"
            Object requests = rootMap.get("relationships_follow_requests_sent");
            if (requests instanceof List) {
                List<Map<String, Object>> rawList = (List<Map<String, Object>>) requests;
                for (Map<String, Object> obj : rawList) {
                    extractAccount(obj, accounts);
                }
            }

            // following.json – accounts held under "relationships_following"
            Object following = rootMap.get("relationships_following");
            if (following instanceof List) {
                List<Map<String, Object>> rawList = (List<Map<String, Object>>) following;
                for (Map<String, Object> obj : rawList) {
                    extractAccount(obj, accounts);
                }
            }
        }

        return accounts;
    }

    @SuppressWarnings("unchecked")
    private static void extractAccount(Map<String, Object> obj, List<Account> accounts) {
        String username = null;
        String href = null;
        Long timestamp = null;

        // Case 1: followers.json / pending requests: data in "string_list_data"
        List<Map<String, Object>> stringList = (List<Map<String, Object>>) obj.get("string_list_data");
        if (stringList != null && !stringList.isEmpty()) {
            Map<String, Object> entry = stringList.get(0);
            if (entry.get("value") != null) {
                username = ((String) entry.get("value")).toLowerCase();
            }
            href = (String) entry.get("href");
            timestamp = entry.get("timestamp") != null ? ((Number) entry.get("timestamp")).longValue() : null;
        }

        // Case 2: following.json (username in "title")
        if (obj.containsKey("title")) {
            username = ((String) obj.get("title")).toLowerCase();
            if (stringList != null && !stringList.isEmpty()) {
                Map<String, Object> entry = stringList.get(0);
                href = (String) entry.get("href");
                timestamp = entry.get("timestamp") != null ? ((Number) entry.get("timestamp")).longValue() : null;
            }
        }

        // Case 3: pending_follow_requests.json (new format) – data in "label_values"
        List<Map<String, Object>> labelValues = (List<Map<String, Object>>) obj.get("label_values");
        if (labelValues != null && !labelValues.isEmpty()) {
            for (Map<String, Object> lv : labelValues) {
                String label = (String) lv.get("label");
                String value = (String) lv.get("value");
                if (label == null || value == null) continue;
                switch (label) {
                    case "Username":
                        if (!value.trim().isEmpty()) {
                            username = value.toLowerCase();
                        }
                        break;
                    case "URL":
                        if (!value.trim().isEmpty()) {
                            href = value;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (obj.get("timestamp") != null) {
                timestamp = ((Number) obj.get("timestamp")).longValue();
            }
        }

        // If username is empty or missing, try to derive it from href
        if ((username == null || username.trim().isEmpty()) && href != null && !href.trim().isEmpty()) {
            String derived = extractUsernameFromHref(href);
            if (derived != null && !derived.trim().isEmpty()) {
                username = derived.toLowerCase();
            }
        }

        // New pending-request exports often leave URL empty; build profile link from username
        if ((href == null || href.trim().isEmpty()) && username != null && !username.trim().isEmpty()) {
            href = "https://www.instagram.com/" + username + "/";
        }

        // Final fallback: if still missing, create a unique placeholder so frontend trackBy
        // never receives duplicate/empty keys
        if (username == null || username.trim().isEmpty()) {
            if (timestamp != null) {
                username = "pending_" + timestamp; // deterministic and unique when timestamp exists
            } else {
                username = "pending_" + UUID.randomUUID().toString().substring(0, 8);
            }
        }

        if (username != null) {
            accounts.add(new Account(username, href, timestamp));
        }
    }

    // Extract username from an href/url-like string (e.g., https://instagram.com/username/)
    private static String extractUsernameFromHref(String href) {
        if (href == null) return "";
        String u = href.trim();
        // remove protocol
        if (u.startsWith("http://")) u = u.substring(7);
        else if (u.startsWith("https://")) u = u.substring(8);
        // remove leading @ if present
        if (u.startsWith("@")) u = u.substring(1);
        // remove trailing slash
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        // take last path segment
        String[] parts = u.split("/");
        String last = parts.length > 0 ? parts[parts.length - 1] : u;
        // strip query/fragment
        int q = last.indexOf('?'); if (q >= 0) last = last.substring(0, q);
        int hash = last.indexOf('#'); if (hash >= 0) last = last.substring(0, hash);
        return last.trim();
    }
}