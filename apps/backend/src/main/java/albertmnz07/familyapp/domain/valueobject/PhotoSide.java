package albertmnz07.familyapp.domain.valueobject;

/** Which side of a DNI document a photo represents. */
public enum PhotoSide {
    FRONT("front", "Front"),
    BACK("back", "Back");

    private final String lowerName;
    private final String displayName;

    PhotoSide(String lowerName, String displayName) {
        this.lowerName = lowerName;
        this.displayName = displayName;
    }

    public String lowerName() {
        return lowerName;
    }

    public String displayName() {
        return displayName;
    }
}
