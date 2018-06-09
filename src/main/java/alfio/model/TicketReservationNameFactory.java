package alfio.model;


public class TicketReservationNameFactory {
    enum WESTERN{
        UNITED_STATES, CANADA, ITALY, SWITZERLAND, UNITED_KINGDOM, AUSTRALIA, SOUTH_AFRICA;
    }
    enum EASTERN{
        CHINA, KOREA, JAPAN;
    }
    public TicketReservationName getNameNotation(String Country, String firstName, String lastName) {

        TicketReservationName ticketReservationName = new TicketReservatonWesternName(firstName, lastName);

//        for (WESTERN western : WESTERN.values()) {
            //init by western name notation
//            if (western.name().equals(Country)) {
//                ticketReservationName = new TicketReservatonWesternName(firstName, lastName);
//            }
//        }
        for (EASTERN eastern : EASTERN.values()) {
            if (eastern.name().equals(Country)) {
                ticketReservationName = new TicketReservationEasternName(firstName, lastName);
            }
        }
        return ticketReservationName;
    }
}

