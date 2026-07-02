package net.swzo.create_blueprinted.exception;

public class SchematicImageRenderException extends RuntimeException {
    public SchematicImageRenderException(String message, Exception e) {
        super(message, e);
    }
}
