package com.uber.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
// service/KeycloakUserService.java
import org.springframework.http.*;

import java.net.URI;

@Service
public class UserService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String keycloakBaseUrl = "http://localhost:9090"; 
    private final String realm = "myrealm"; 

    public void registerUser(String username, String email, String password , String role) {
        // 1. Create user
        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("enabled", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminAccessToken());
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        try {
    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
    System.out.println("Response: " + response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCodeValue() != 201) {
            throw new RuntimeException("Failed to create user in Keycloak: " + response);
        }

        // 2. Extract new userId from Location header
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new RuntimeException("Keycloak did not return user location");
        }
        String userId = location.getPath().substring(location.getPath().lastIndexOf("/") + 1);
        
        // 3. Set password
        setPassword(userId, password, headers);
        assignRoleToUser(userId, role,headers);
    }
        catch (HttpClientErrorException | HttpServerErrorException ex) {
            System.out.println("Status code: " + ex.getStatusCode());
            System.out.println("Response body: " + ex.getResponseBodyAsString());
        }
    }

    private void setPassword(String userId, String password, HttpHeaders headers) {
        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(credential, headers);
        restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
    }
    public void assignRoleToUser(String userId, String roleName, HttpHeaders headers) {

        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        ResponseEntity<Map> roleResponse = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> role = roleResponse.getBody();

        String assignUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        HttpEntity<Object> request = new HttpEntity<>(List.of(role), headers);
        restTemplate.postForEntity(assignUrl, request, Void.class);
        
    }
    public String login(String username, String password) {
        String url = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "myclient"); 
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Login failed: " + response);
        }

        return response.getBody();
    }

    private String getAdminAccessToken() {
        String url = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "password"); // from docker-compose env
        body.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return (String) response.getBody().get("access_token");
    }
}
