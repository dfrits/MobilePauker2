package com.daniel.mobilepauker2.model;

public class SaveResult {
    private final boolean successful;
    private final String errorMessage;

    public SaveResult(boolean successful, String errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
