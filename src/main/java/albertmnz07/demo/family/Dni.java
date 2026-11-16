package albertmnz07.demo.family;

public record Dni(
        String number,
        byte[] frontPhoto,
        String frontPhotoContentType,
        byte[] backPhoto,
        String backPhotoContentType) {
}
