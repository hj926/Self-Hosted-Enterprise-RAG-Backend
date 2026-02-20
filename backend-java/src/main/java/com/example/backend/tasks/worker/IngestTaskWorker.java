package com.example.backend.tasks.worker;

import com.example.backend.config.AppConfig;
import com.example.backend.tasks.entity.IngestTaskEntity;
import com.example.backend.tasks.repo.IngestTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class IngestTaskWorker {

  private final IngestTaskRepository repo;
  private final AppConfig cfg;
  private final ObjectMapper om = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .version(HttpClient.Version.HTTP_1_1)
      .build();

  public IngestTaskWorker(IngestTaskRepository repo, AppConfig cfg) {
    this.repo = repo;
    this.cfg = cfg;
  }

  @Scheduled(fixedDelayString = "${backend.ingest.poll-interval-ms:1000}")
  public void poll() {
    List<IngestTaskEntity> pending = repo.findTop10ByStatusOrderByTaskIdAsc(IngestTaskEntity.Status.PENDING.name());

    for (IngestTaskEntity t : pending) {
      try {
        t.setStatus(IngestTaskEntity.Status.RUNNING);
        repo.save(t);

        String docId = ingestToRag(t.getFilename(), t.getFileBytes());

        t.setDocId(docId);
        t.setErrorCode(null);
        t.setMessage(null);
        t.setStatus(IngestTaskEntity.Status.SUCCEEDED);
        repo.save(t);

      } catch (Exception e) {
        t.setStatus(IngestTaskEntity.Status.FAILED);
        t.setErrorCode("RAG_INGEST_FAILED");
        t.setMessage("RAG ingest failed: " + safeMsg(e));
        repo.save(t);
      }
    }
  }

  private String ingestToRag(String filename, byte[] fileBytes) throws Exception {
    String base = cfg.getRagBaseUrl();
    if (base == null || base.isBlank())
      throw new IllegalStateException("backend.ragBaseUrl is empty");
    String url = base.endsWith("/") ? base + "documents" : base + "/documents";

    String boundary = "----Boundary" + UUID.randomUUID();
    byte[] body = buildMultipart(boundary, filename, fileBytes);

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMinutes(5))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        .build();

    HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

    String text = resp.body() == null ? "" : new String(resp.body(), StandardCharsets.UTF_8);

    if (resp.statusCode() / 100 != 2) {
      throw new RuntimeException("rag-service status=" + resp.statusCode() + " body=" + text);
    }

    String docId = extractDocIdFromJson(text);
    if (docId == null || docId.isBlank()) {
      throw new RuntimeException("rag-service response missing doc_id: " + text);
    }
    return docId;
  }

  private String extractDocIdFromJson(String json) {
    try {
      JsonNode root = om.readTree(json);

      JsonNode v = root.get("doc_id");
      if (v != null && v.isTextual())
        return v.asText();

      v = root.get("docId");
      if (v != null && v.isTextual())
        return v.asText();

      JsonNode doc = root.get("document");
      if (doc != null && doc.isObject()) {
        v = doc.get("doc_id");
        if (v != null && v.isTextual())
          return v.asText();

        v = doc.get("docId");
        if (v != null && v.isTextual())
          return v.asText();
      }

      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private static byte[] buildMultipart(String boundary, String filename, byte[] fileBytes) {
    String header = "--" + boundary + "\r\n" +
        "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
        "Content-Type: application/pdf\r\n\r\n";

    String footer = "\r\n--" + boundary + "--\r\n";

    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
    byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

    byte[] out = new byte[headerBytes.length + fileBytes.length + footerBytes.length];

    System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
    System.arraycopy(fileBytes, 0, out, headerBytes.length, fileBytes.length);
    System.arraycopy(footerBytes, 0, out, headerBytes.length + fileBytes.length, footerBytes.length);

    return out;
  }

  private static String safeMsg(Throwable t) {
    String m = t.getMessage();
    if (m == null)
      return t.getClass().getSimpleName();
    m = m.replaceAll("[\\r\\n\\t]+", " ").trim();
    return m.length() > 500 ? m.substring(0, 500) : m;
  }
}