package albertmnz07.familyapp.application.command;

/** One side of an optional DNI-photo upload: the bytes plus the declared content type. */
public record PhotoUpload(byte[] content, String contentType) {}
