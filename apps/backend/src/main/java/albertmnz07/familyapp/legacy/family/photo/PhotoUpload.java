package albertmnz07.familyapp.legacy.family.photo;

/**
 * One side of an optional DNI-photo upload: the bytes plus the declared content type. Absent when
 * the client omits that part, which leaves the stored photo untouched.
 */
public record PhotoUpload(byte[] content, String contentType) {}
