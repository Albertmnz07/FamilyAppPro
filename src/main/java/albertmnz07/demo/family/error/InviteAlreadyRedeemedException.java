package albertmnz07.demo.family.error;

public class InviteAlreadyRedeemedException extends IllegalStateException {

    public InviteAlreadyRedeemedException(String message) {
        super(message);
    }
}
