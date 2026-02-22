package com.example.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backend")
public class AppConfig {
  private String ragBaseUrl;
  private String apiKeyHeader;
  private String requestIdHeader;
  private Http http = new Http();
  private Auth auth = new Auth();
  private Ingest ingest = new Ingest();
  private String tenantHeader;

  public String getTenantHeader() {
    return tenantHeader;
  }

  public void setTenantHeader(String tenantHeader) {
    this.tenantHeader = tenantHeader;
  }

  public static class Http {
    private int timeoutMs = 60000;

    public int getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
      this.timeoutMs = timeoutMs;
    }
  }

  public static class Auth {
    private List<ApiKeyEntry> keys;

    public List<ApiKeyEntry> getKeys() {
      return keys;
    }

    public void setKeys(List<ApiKeyEntry> keys) {
      this.keys = keys;
    }
  }

  public static class ApiKeyEntry {
    private String key;
    private String tenantId;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getTenantId() {
      return tenantId;
    }

    public void setTenantId(String tenantId) {
      this.tenantId = tenantId;
    }
  }

  public static class Ingest {
    private long pollIntervalMs = 1000;
    private int maxConcurrent = 2;

    public long getPollIntervalMs() {
      return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
      this.pollIntervalMs = pollIntervalMs;
    }

    public int getMaxConcurrent() {
      return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
      this.maxConcurrent = maxConcurrent;
    }
  }

  public String getRagBaseUrl() {
    return ragBaseUrl;
  }

  public void setRagBaseUrl(String ragBaseUrl) {
    this.ragBaseUrl = ragBaseUrl;
  }

  public String getApiKeyHeader() {
    return apiKeyHeader;
  }

  public void setApiKeyHeader(String apiKeyHeader) {
    this.apiKeyHeader = apiKeyHeader;
  }

  public String getRequestIdHeader() {
    return requestIdHeader;
  }

  public void setRequestIdHeader(String requestIdHeader) {
    this.requestIdHeader = requestIdHeader;
  }

  public Http getHttp() {
    return http;
  }

  public void setHttp(Http http) {
    this.http = http;
  }

  public Auth getAuth() {
    return auth;
  }

  public void setAuth(Auth auth) {
    this.auth = auth;
  }

  public Ingest getIngest() {
    return ingest;
  }

  public void setIngest(Ingest ingest) {
    this.ingest = ingest;
  }
}
