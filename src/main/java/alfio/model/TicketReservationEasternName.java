package alfio.model;

public class TicketReservationEasternName extends TicketReservationName{
    public TicketReservationEasternName(String firstName, String lastName) {
        super(firstName, lastName);
    }

    @Override
    public String getFullName() {
        return (firstName != null && lastName != null) ? (lastName + " " + firstName) : fullName;
    }
}
