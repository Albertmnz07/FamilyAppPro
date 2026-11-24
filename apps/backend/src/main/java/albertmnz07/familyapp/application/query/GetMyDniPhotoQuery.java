package albertmnz07.familyapp.application.query;

import java.util.UUID;

public record GetMyDniPhotoQuery(UUID personaId, Side side) {

    public enum Side {
        FRONT("Front"),
        BACK("Back");

        private final String label;

        Side(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
