package alfio.model;

import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper;
import lombok.Getter;

@Getter
public class TicketReservationName {
    protected String fullName;
    protected String firstName;
    protected String lastName;

    public TicketReservationName(String firstName, String lastName){
        this.fullName = fullName;
        this.firstName = firstName;
    }
}
