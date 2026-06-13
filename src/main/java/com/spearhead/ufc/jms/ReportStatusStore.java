package com.spearhead.ufc.jms;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry that tracks the lifecycle of every queued report request.
 * Entries are kept until the caller explicitly removes them (e.g. after download).
 */
@Component
public class ReportStatusStore {

    public enum Status { PENDING, IN_PROGRESS, COMPLETED, FAILED }

    public static class ReportResult {
        private Status status;
        private String filePath;
        private String filename;
        private String downloadUrl;
        private String errorMessage;
        private final long createdAt = System.currentTimeMillis();

        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public long getCreatedAt() { return createdAt; }
    }

    private final Map<String, ReportResult> store = new ConcurrentHashMap<>();

    public void setPending(String requestId) {
        ReportResult r = new ReportResult();
        r.setStatus(Status.PENDING);
        store.put(requestId, r);
    }

    public void setInProgress(String requestId) {
        store.computeIfPresent(requestId, (k, r) -> { r.setStatus(Status.IN_PROGRESS); return r; });
    }

    public void setCompleted(String requestId, String filePath, String filename) {
        store.computeIfPresent(requestId, (k, r) -> {
            r.setStatus(Status.COMPLETED);
            r.setFilePath(filePath);
            r.setFilename(filename);
            r.setDownloadUrl("/dashboardSummary/download-report/" + filename);
            return r;
        });
    }

    public void setFailed(String requestId, String errorMessage) {
        store.computeIfPresent(requestId, (k, r) -> {
            r.setStatus(Status.FAILED);
            r.setErrorMessage(errorMessage);
            return r;
        });
    }

    public ReportResult get(String requestId) {
        return store.get(requestId);
    }

    public void remove(String requestId) {
        store.remove(requestId);
    }
}
