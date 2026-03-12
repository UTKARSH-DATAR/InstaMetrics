package com.InstaMetrics.service;

import com.InstaMetrics.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.InstaMetrics.util.ZipParser;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ZipService {

    public Map<String, Object> parse(MultipartFile file) {
        // Delegates ZIP parsing to the utility class and returns a map with keys:
        // "followers", "following", "pendingRequests" -> List<Account>
        return ZipParser.extractData(file);
    }

    public List<Account> findNonFollowers(Map<String, Object> parsedData) {
        // Be defensive: treat missing lists as empty instead of failing with NPE
        List<Account> followers = parsedData.get("followers") instanceof List ? (List<Account>) parsedData.get("followers") : Collections.emptyList();
        List<Account> following = parsedData.get("following") instanceof List ? (List<Account>) parsedData.get("following") : Collections.emptyList();

        // Build a set of normalized follower usernames (lowercase, trimmed) skipping blanks
        Set<String> followerUsernames = followers.stream()
                .filter(Objects::nonNull)
                .map(acc -> {
                    String u = acc.getUsername();
                    if (u == null || u.trim().isEmpty()) {
                        u = acc.getProfileUrl();
                        if (u != null) u = extractUsernameFromUrl(u);
                    }
                    return normalizeUsername(u);
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (following.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate following by normalized username. Use TreeMap for deterministic alphabetic order.
        Map<String, Account> uniqFollowing = new TreeMap<>();
        for (Account acc : following) {
            if (acc == null) continue;
            String raw = acc.getUsername();
            String href = acc.getProfileUrl();
            // If username is missing, try to extract from href
            if ((raw == null || raw.trim().isEmpty()) && href != null) {
                raw = extractUsernameFromUrl(href);
            }
            if (raw == null) continue;
            String uname = normalizeUsername(raw);
            if (uname.isEmpty()) continue;

            if (!uniqFollowing.containsKey(uname)) {
                // store a copy or the original account - prefer preserving profileUrl/timestamp
                uniqFollowing.put(uname, acc);
            } else {
                // Prefer the entry that has more information (timestamp/profileUrl)
                Account existing = uniqFollowing.get(uname);
                boolean existingHasInfo = (existing.getTimestamp() != null && existing.getTimestamp() > 0) || (existing.getProfileUrl() != null && !existing.getProfileUrl().isEmpty());
                boolean accHasInfo = (acc.getTimestamp() != null && acc.getTimestamp() > 0) || (acc.getProfileUrl() != null && !acc.getProfileUrl().isEmpty());
                if (!existingHasInfo && accHasInfo) {
                    uniqFollowing.put(uname, acc);
                }
            }
        }

        // Filter out those who follow back and return a deterministic sorted list (TreeMap preserves key order)
        List<Account> result = new ArrayList<>();
        for (Map.Entry<String, Account> e : uniqFollowing.entrySet()) {
            String normalizedKey = e.getKey(); // already normalized username
            if (!followerUsernames.contains(normalizedKey)) {
                result.add(e.getValue());
            }
        }
        // uniqFollowing is a TreeMap keyed by normalized username, so result is already deterministic
        return result;
    }

    public List<Account> findPendingRequests(Map<String, Object> parsedData) {
        List<Account> pending = parsedData.get("pendingRequests") instanceof List ? (List<Account>) parsedData.get("pendingRequests") : Collections.emptyList();

        // For pending requests we only need to return the parsed list as‑is
        return pending;
    }

    // Normalize username: trim, lowercase, remove leading '@', and strip URL parts if present
    private static String normalizeUsername(String s) {
        if (s == null) return "";
        s = s.trim().toLowerCase();
        if (s.startsWith("@")) s = s.substring(1);
        // If it looks like a URL, extract last path segment
        if (s.contains("/")) {
            s = extractUsernameFromUrl(s);
        }
        // Remove any query params or fragments
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        int hash = s.indexOf('#'); if (hash >= 0) s = s.substring(0, hash);
        return s.trim();
    }

    // Extract username from profile URL or path-like string
    private static String extractUsernameFromUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        // Remove protocol
        if (u.startsWith("http://")) u = u.substring(7);
        else if (u.startsWith("https://")) u = u.substring(8);
        // Remove leading @
        if (u.startsWith("@")) u = u.substring(1);
        // Remove trailing slash
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        // Split on slashes and take last segment
        String[] parts = u.split("/");
        String last = parts.length > 0 ? parts[parts.length - 1] : u;
        // Remove query or fragment
        int q = last.indexOf('?'); if (q >= 0) last = last.substring(0, q);
        int hash = last.indexOf('#'); if (hash >= 0) last = last.substring(0, hash);
        // If the hostname remains (e.g., instagram.com), return empty
        if (last.contains(".")) return last; // still may be username like 'user.name'
        return last.trim().toLowerCase();
    }
}