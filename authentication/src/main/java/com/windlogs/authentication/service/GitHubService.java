package com.windlogs.authentication.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API_URL = "https://api.github.com/repos";
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token:}")
    private String githubToken;

    /**
     * Create HTTP headers with GitHub token authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isEmpty()) {
            headers.set("Authorization", "token " + githubToken);
            logger.debug("Using GitHub token authentication");
        } else {
            logger.warn("No GitHub token configured - using anonymous access (rate limited)");
        }
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "WindLogs-Application");
        return headers;
    }

    /**
     * Make authenticated GET request to GitHub API
     */
    private ResponseEntity<String> makeAuthenticatedRequest(String url) {
        HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    /**
     * Make authenticated GET request to GitHub API returning JsonNode
     */
    private JsonNode makeAuthenticatedJsonRequest(String url) {
        try {
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to make authenticated JSON request to: {}", url, e);
            throw new RuntimeException("GitHub API request failed: " + e.getMessage());
        }
    }

    /**
     * Fetch file contents from a GitHub repository.
     * @param owner The repository owner (user/org)
     * @param repo The repository name
     * @param path The file path in the repo
     * @param branch The branch name (optional)
     * @return The raw file content as a String (base64-encoded if not a text file)
     */
    public String fetchFileContent(String owner, String repo, String path, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/contents/" + path)
                .queryParam("ref", branch != null ? branch : "main")
                .toUriString();

        try {
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            String body = response.getBody();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);

            if (root.has("encoding") && "base64".equals(root.get("encoding").asText()) && root.has("content")) {
                String base64Content = root.get("content").asText().replaceAll("\\n", "");
                byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
                return new String(decodedBytes);
            } else {
                return body;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch file content for {}/{}/{}", owner, repo, path, e);
            throw new RuntimeException("Failed to fetch file content: " + e.getMessage());
        }
    }

    /**
     * Fetch list of branches for a GitHub repository.
     */
    public String fetchBranches(String owner, String repo) {
        String url = GITHUB_API_URL + "/" + owner + "/" + repo + "/branches";
        try {
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to fetch branches for {}/{}", owner, repo, e);
            throw new RuntimeException("Failed to fetch branches: " + e.getMessage());
        }
    }

    /**
     * Fetch recent commits for a GitHub repository.
     */
    public String fetchCommits(String owner, String repo, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/commits")
                .queryParam("sha", branch != null ? branch : "main")
                .queryParam("per_page", "20") // Limit to 20 recent commits
                .toUriString();

        try {
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to fetch commits for {}/{}", owner, repo, e);
            throw new RuntimeException("Failed to fetch commits: " + e.getMessage());
        }
    }

    /**
     * List files and folders in a given path of a GitHub repository.
     * @param owner The repository owner (user/org)
     * @param repo The repository name
     * @param path The folder path in the repo (empty or null for root)
     * @param branch The branch name (optional)
     * @return JSON array as String listing files/folders
     */
    public String listFilesAndFolders(String owner, String repo, String path, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/contents" +
                        (path != null && !path.isEmpty() ? "/" + path : ""))
                .queryParam("ref", branch != null ? branch : "main")
                .toUriString();

        try {
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            String body = response.getBody();

            ObjectMapper mapper = new ObjectMapper();
            List<JsonNode> nodes = mapper.readValue(body, new TypeReference<List<JsonNode>>(){});
            List<Map<String, Object>> simplified = new ArrayList<>();

            for (JsonNode node : nodes) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", node.path("name").asText());
                item.put("type", node.path("type").asText());
                item.put("path", node.path("path").asText());
                item.put("html_url", node.path("html_url").asText());
                if (node.has("size")) {
                    item.put("size", node.path("size").asInt());
                }
                simplified.add(item);
            }

            return mapper.writeValueAsString(simplified);
        } catch (Exception e) {
            logger.error("Failed to list files and folders for {}/{}/{}", owner, repo, path, e);
            throw new RuntimeException("Failed to list files and folders: " + e.getMessage());
        }
    }

    /**
     * Build a non-recursive tree structure for a given path in a GitHub repository.
     * This method provides detailed information including repository info, branch info, and file details.
     */
    public String getRepositoryTree(String owner, String repo, String branch, String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String defaultBranch = branch != null ? branch : "main";

            // 1. Get repository info
            String repoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
            JsonNode repoInfo = makeAuthenticatedJsonRequest(repoUrl);

            // 2. Get branch info
            String branchUrl = repoUrl + "/branches/" + defaultBranch;
            JsonNode branchInfo = makeAuthenticatedJsonRequest(branchUrl);

            // 3. Get latest commit for branch
            String commitsUrl = repoUrl + "/commits?sha=" + defaultBranch + "&per_page=1";
            JsonNode latestCommitArr = makeAuthenticatedJsonRequest(commitsUrl);
            JsonNode latestCommit = latestCommitArr.isArray() && latestCommitArr.size() > 0 ?
                    latestCommitArr.get(0) : null;

            // 4. Get commit count (simplified approach)
            int commitsCount = getCommitCount(owner, repo, defaultBranch);

            // 5. Get directory contents
            String contentsUrl = repoUrl + "/contents" +
                    (path != null && !path.isEmpty() ? "/" + path : "") + "?ref=" + defaultBranch;
            JsonNode contents = makeAuthenticatedJsonRequest(contentsUrl);

            // 6. Build tree array with last commit info for each item
            List<Map<String, Object>> tree = buildTreeWithCommitInfo(owner, repo, defaultBranch, contents);

            // 7. Build repository info
            Map<String, Object> repoMap = buildRepositoryInfo(repoInfo, branchInfo, latestCommit, commitsCount);

            // 8. Build final result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("repository", repoMap);
            result.put("tree", tree);
            result.put("path", path != null ? path : "");
            result.put("totalItems", tree.size());

            return mapper.writeValueAsString(result);

        } catch (Exception e) {
            logger.error("Failed to build repository tree for {}/{}/{}", owner, repo, path, e);
            return "{\"error\":\"Failed to build repository tree: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Get commit count for a branch (simplified approach)
     */
    private int getCommitCount(String owner, String repo, String branch) {
        try {
            String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repo +
                    "/commits?sha=" + branch + "&per_page=1";
            ResponseEntity<String> response = makeAuthenticatedRequest(commitsUrl);

            // Try to get count from Link header
            if (response.getHeaders().containsKey("Link")) {
                String linkHeader = response.getHeaders().getFirst("Link");
                if (linkHeader != null && linkHeader.contains("rel=\"last\"")) {
                    return parseLinkHeaderForCount(linkHeader);
                }
            }

            // Fallback: use a reasonable estimate or make additional API call
            return 1; // At least one commit exists

        } catch (Exception e) {
            logger.warn("Failed to get commit count, using default", e);
            return 1;
        }
    }

    /**
     * Parse Link header to extract commit count
     */
    private int parseLinkHeaderForCount(String linkHeader) {
        try {
            String[] parts = linkHeader.split(",");
            for (String part : parts) {
                if (part.contains("rel=\"last\"")) {
                    String urlPart = part.substring(part.indexOf('<') + 1, part.indexOf('>'));
                    String[] params = urlPart.split("&page=");
                    if (params.length > 1) {
                        return Integer.parseInt(params[1]);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse link header for count", e);
        }
        return 1;
    }

    /**
     * Build tree structure with commit information for each file/folder
     */
    private List<Map<String, Object>> buildTreeWithCommitInfo(String owner, String repo,
                                                              String branch, JsonNode contents) {
        List<Map<String, Object>> tree = new ArrayList<>();

        for (JsonNode node : contents) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", node.path("name").asText());
            item.put("type", node.path("type").asText().equals("dir") ? "tree" : "blob");
            item.put("path", node.path("path").asText());

            if (node.path("type").asText().equals("file")) {
                item.put("size", node.path("size").asInt());
            }

            // Get last commit for this file/folder (with error handling)
            try {
                Map<String, Object> lastCommit = getLastCommitForPath(owner, repo, branch,
                        node.path("path").asText());
                if (lastCommit != null) {
                    item.put("lastCommit", lastCommit);
                }
            } catch (Exception e) {
                logger.warn("Failed to get last commit for path: {}", node.path("path").asText(), e);
                // Continue without commit info
            }

            tree.add(item);
        }

        return tree;
    }

    /**
     * Get last commit information for a specific path
     */
    private Map<String, Object> getLastCommitForPath(String owner, String repo, String branch, String path) {
        try {
            String fileCommitsUrl = "https://api.github.com/repos/" + owner + "/" + repo +
                    "/commits?sha=" + branch + "&path=" + path + "&per_page=1";
            JsonNode fileCommitsArr = makeAuthenticatedJsonRequest(fileCommitsUrl);

            if (fileCommitsArr.isArray() && fileCommitsArr.size() > 0) {
                JsonNode fileCommit = fileCommitsArr.get(0);
                Map<String, Object> lastCommit = new LinkedHashMap<>();
                lastCommit.put("sha", fileCommit.path("sha").asText().substring(0, 7)); // Short SHA
                lastCommit.put("message", fileCommit.path("commit").path("message").asText());
                lastCommit.put("date", fileCommit.path("commit").path("committer").path("date").asText());
                lastCommit.put("author", fileCommit.path("commit").path("committer").path("name").asText());
                return lastCommit;
            }
        } catch (Exception e) {
            logger.debug("Could not fetch commit info for path: {}", path, e);
        }
        return null;
    }

    /**
     * Build repository information object
     */
    private Map<String, Object> buildRepositoryInfo(JsonNode repoInfo, JsonNode branchInfo,
                                                    JsonNode latestCommit, int commitsCount) {
        Map<String, Object> repoMap = new LinkedHashMap<>();
        repoMap.put("name", repoInfo.path("name").asText());
        repoMap.put("fullName", repoInfo.path("full_name").asText());
        repoMap.put("description", repoInfo.path("description").asText());
        repoMap.put("private", repoInfo.path("private").asBoolean());
        repoMap.put("defaultBranch", repoInfo.path("default_branch").asText());
        repoMap.put("branch", branchInfo.path("name").asText());

        Map<String, Object> commitMap = new LinkedHashMap<>();
        if (latestCommit != null) {
            commitMap.put("sha", latestCommit.path("sha").asText());
            commitMap.put("shortSha", latestCommit.path("sha").asText().substring(0, 7));
            commitMap.put("message", latestCommit.path("commit").path("message").asText());
            commitMap.put("author", latestCommit.path("commit").path("committer").path("name").asText());
            commitMap.put("date", latestCommit.path("commit").path("committer").path("date").asText());
            commitMap.put("commitsCount", commitsCount);
        }
        repoMap.put("commit", commitMap);

        return repoMap;
    }

    /**
     * Get repository information
     */
    public String getRepositoryInfo(String owner, String repo) {
        try {
            String repoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
            JsonNode repoInfo = makeAuthenticatedJsonRequest(repoUrl);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", repoInfo.path("name").asText());
            result.put("fullName", repoInfo.path("full_name").asText());
            result.put("description", repoInfo.path("description").asText());
            result.put("language", repoInfo.path("language").asText());
            result.put("private", repoInfo.path("private").asBoolean());
            result.put("defaultBranch", repoInfo.path("default_branch").asText());
            result.put("createdAt", repoInfo.path("created_at").asText());
            result.put("updatedAt", repoInfo.path("updated_at").asText());
            result.put("size", repoInfo.path("size").asInt());
            result.put("stargazersCount", repoInfo.path("stargazers_count").asInt());
            result.put("forksCount", repoInfo.path("forks_count").asInt());

            return mapper.writeValueAsString(result);

        } catch (Exception e) {
            logger.error("Failed to get repository info for {}/{}", owner, repo, e);
            return "{\"error\":\"Failed to get repository info: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Check rate limit status
     */
    public String getRateLimitStatus() {
        try {
            String url = "https://api.github.com/rate_limit";
            ResponseEntity<String> response = makeAuthenticatedRequest(url);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to get rate limit status", e);
            return "{\"error\":\"Failed to get rate limit status: " + e.getMessage() + "\"}";
        }
    }
}