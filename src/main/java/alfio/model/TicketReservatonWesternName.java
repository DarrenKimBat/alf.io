package alfio.model;

public class TicketReservatonWesternName extends TicketReservationName{
    public TicketReservatonWesternName (String firstName, String lastName) {
        super(firstName, lastName);
    }

    @Override
    public String getFullName() {
        return (firstName != null && lastName != null) ? (firstName + " " + lastName) : fullName;
    }

}
