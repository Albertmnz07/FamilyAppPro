package albertmnz07.demo.family;

public record Dni(
        String number,
        String frontPhotoKey,
        String frontPhotoContentType,
        String backPhotoKey,
        String backPhotoContentType) {

    public boolean hasFrontPhoto() {
        return frontPhotoKey != null && !frontPhotoKey.isBlank();
    }

    public boolean hasBackPhoto() {
        return backPhotoKey != null && !backPhotoKey.isBlank();
    }
}
