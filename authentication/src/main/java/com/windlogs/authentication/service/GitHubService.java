package com.windlogs.authentication.service;

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

@Service
public class GitHubService {
    private static final String GITHUB_API_URL = "https://api.github.com/repos";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch file contents from a GitHub repository (public repos only).
     * @param owner The repository owner (user/org)
     * @param repo The repository name
     * @param path The file path in the repo
     * @param branch The branch name (optional)
     * @return The raw file content as a String (base64-encoded if not a text file)
     */
    public String fetchFileContent(String owner, String repo, String path, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/contents/" + path)
                .queryParam("ref", branch)
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String body = response.getBody();
        try {
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
            // If parsing or decoding fails, return the original response
            return body;
        }
    }

    /**
     * Fetch list of branches for a GitHub repository.
     */
    public String fetchBranches(String owner, String repo) {
        String url = GITHUB_API_URL + "/" + owner + "/" + repo + "/branches";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }

    /**
     * Fetch recent commits for a GitHub repository.
     */
    public String fetchCommits(String owner, String repo, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/commits")
                .queryParam("sha", branch)
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }

    /**
     * List files and folders in a given path of a GitHub repository (public repos only).
     * @param owner The repository owner (user/org)
     * @param repo The repository name
     * @param path The folder path in the repo (empty or null for root)
     * @param branch The branch name (optional)
     * @return JSON array as String listing files/folders
     */
    public String listFilesAndFolders(String owner, String repo, String path, String branch) {
        String url = UriComponentsBuilder.fromHttpUrl(GITHUB_API_URL + "/" + owner + "/" + repo + "/contents" + (path != null && !path.isEmpty() ? "/" + path : ""))
                .queryParam("ref", branch)
                .toUriString();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String body = response.getBody();
        try {
            ObjectMapper mapper = new ObjectMapper();
            // The response is a JSON array
            List<JsonNode> nodes = mapper.readValue(body, new TypeReference<List<JsonNode>>(){});
            List<Map<String, Object>> simplified = new ArrayList<>();
            for (JsonNode node : nodes) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", node.path("name").asText());
                item.put("type", node.path("type").asText());
                item.put("path", node.path("path").asText());
                item.put("html_url", node.path("html_url").asText());
                simplified.add(item);
            }
            return mapper.writeValueAsString(simplified);
        } catch (Exception e) {
            // If parsing fails, return the original response
            return body;
        }
    }

    /**
     * Build a non-recursive tree structure for a given path in a GitHub repository, including repo info and last commit for each file/folder.
     * Returns a JSON object matching the requested format, for a single directory level.
     */
    public String getRepositoryTree(String owner, String repo, String branch, String path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 1. Get repo info
            String repoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
            JsonNode repoInfo = restTemplate.getForObject(repoUrl, JsonNode.class);
            // 2. Get branch info
            String branchUrl = repoUrl + "/branches/" + branch;
            JsonNode branchInfo = restTemplate.getForObject(branchUrl, JsonNode.class);
            // 3. Get latest commit for branch
            String commitsUrl = repoUrl + "/commits?sha=" + branch + "&per_page=1";
            JsonNode latestCommitArr = restTemplate.getForObject(commitsUrl, JsonNode.class);
            JsonNode latestCommit = latestCommitArr.isArray() && latestCommitArr.size() > 0 ? latestCommitArr.get(0) : null;
            // 4. Get commit count
            String commitCountUrl = repoUrl + "/commits?sha=" + branch + "&per_page=1";
            ResponseEntity<String> commitCountResp = restTemplate.getForEntity(commitCountUrl, String.class);
            int commitsCount = 0;
            if (commitCountResp.getHeaders().containsKey("Link")) {
                String linkHeader = commitCountResp.getHeaders().getFirst("Link");
                if (linkHeader != null && linkHeader.contains("rel=\"last\"")) {
                    String[] parts = linkHeader.split(",");
                    for (String part : parts) {
                        if (part.contains("rel=\"last\"")) {
                            String urlPart = part.substring(part.indexOf('<') + 1, part.indexOf('>'));
                            String[] params = urlPart.split("&page=");
                            if (params.length > 1) {
                                commitsCount = Integer.parseInt(params[1]);
                            }
                        }
                    }
                } else {
                    commitsCount = 1;
                }
            } else if (latestCommitArr.isArray()) {
                commitsCount = latestCommitArr.size();
            }
            // 5. Get directory contents
            String contentsUrl = repoUrl + "/contents" + (path != null && !path.isEmpty() ? "/" + path : "") + "?ref=" + branch;
            JsonNode contents = restTemplate.getForObject(contentsUrl, JsonNode.class);
            // 6. Build tree array
            List<Map<String, Object>> tree = new ArrayList<>();
            for (JsonNode node : contents) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", node.path("name").asText());
                item.put("type", node.path("type").asText().equals("dir") ? "tree" : "blob");
                item.put("path", node.path("path").asText());
                if (node.path("type").asText().equals("file")) {
                    item.put("size", node.path("size").asInt());
                }
                // Get last commit for this file/folder
                String fileCommitsUrl = repoUrl + "/commits?sha=" + branch + "&path=" + node.path("path").asText() + "&per_page=1";
                JsonNode fileCommitsArr = restTemplate.getForObject(fileCommitsUrl, JsonNode.class);
                JsonNode fileCommit = fileCommitsArr.isArray() && fileCommitsArr.size() > 0 ? fileCommitsArr.get(0) : null;
                if (fileCommit != null) {
                    Map<String, Object> lastCommit = new LinkedHashMap<>();
                    lastCommit.put("message", fileCommit.path("commit").path("message").asText());
                    lastCommit.put("date", fileCommit.path("commit").path("committer").path("date").asText());
                    lastCommit.put("author", fileCommit.path("commit").path("committer").path("name").asText());
                    item.put("lastCommit", lastCommit);
                }
                tree.add(item);
            }
            // 7. Build repository info
            Map<String, Object> repoMap = new LinkedHashMap<>();
            repoMap.put("name", repoInfo.path("name").asText());
            repoMap.put("branch", branchInfo.path("name").asText());
            Map<String, Object> commitMap = new LinkedHashMap<>();
            if (latestCommit != null) {
                commitMap.put("sha", latestCommit.path("sha").asText());
                commitMap.put("message", latestCommit.path("commit").path("message").asText());
                commitMap.put("author", latestCommit.path("commit").path("committer").path("name").asText());
                commitMap.put("date", latestCommit.path("commit").path("committer").path("date").asText());
                commitMap.put("commitsCount", commitsCount);
            }
            repoMap.put("commit", commitMap);
            // 8. Build final result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("repository", repoMap);
            result.put("tree", tree);
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"Failed to build repository tree: " + e.getMessage() + "\"}";
        }
    }
} 