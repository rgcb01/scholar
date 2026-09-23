package dev.rgcb.scholar.api;

/** Predictable failure of a supported addon operation; no authored state is committed. */
public final class ScholarApiException extends RuntimeException {
    public enum Code { INVALID_INPUT, NOT_FOUND, INVALID_THREAD, VALIDATION_FAILED, IO_FAILURE }
    private final Code code;

    public ScholarApiException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
