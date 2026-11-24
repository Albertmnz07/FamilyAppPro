package albertmnz07.familyapp.application.command;

import java.util.UUID;

/** Each side is optional: a {@code null} upload leaves that side as it was. */
public record UpdateDniPhotosCommand(UUID personaId, PhotoUpload front, PhotoUpload back) {}
