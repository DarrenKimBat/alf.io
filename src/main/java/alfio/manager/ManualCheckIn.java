package alfio.manager;

import static alfio.manager.support.CheckInStatus.OK_READY_TO_BE_CHECKED_IN;
import static alfio.manager.support.CheckInStatus.SUCCESS;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import alfio.manager.support.DefaultCheckInResult;
import alfio.manager.support.TicketAndCheckInResult;
import alfio.model.Audit;
import alfio.model.Ticket;
import alfio.model.Ticket.TicketStatus;
import alfio.model.audit.ScanAudit;
import alfio.repository.AuditingRepository;
import alfio.repository.TicketRepository;
import alfio.repository.audit.ScanAuditRepository;
import alfio.repository.user.UserRepository;


public class ManualCheckIn extends CheckInMethod {	
	
	public ManualCheckIn(CheckInManager checkInManager, TicketRepository ticketRepository,
			ExtensionManager extensionManager, ScanAuditRepository scanAuditRepository,
			AuditingRepository auditingRepository, UserRepository userRepository) {
		super(checkInManager, ticketRepository, extensionManager, scanAuditRepository, auditingRepository, userRepository);
	}

	public boolean executeCheckIn(int eventId, String ticketIdentifier, String user) {
		Optional<Ticket> ticket = ticketRepository.findByUUIDForUpdate(ticketIdentifier);
        return ticket.map(t -> {
            if(t.getStatus() == TicketStatus.TO_BE_PAID) {
                this.checkInManager.acquire(ticketIdentifier);
            }
            checkIn(ticketIdentifier);
            updateRepository(eventId, ticketIdentifier, user, 
			t.getTicketsReservationId(), Audit.EventType.MANUAL_CHECK_IN, t.getId());
            return true;
        }).orElse(false);
	}
	
	public TicketAndCheckInResult checkInAndGetResult(int eventId, String ticketIdentifier, String user) {
		Optional<Ticket> ticket = ticketRepository.findByUUIDForUpdate(ticketIdentifier);
        ticket.map(t -> {
            if(t.getStatus() == TicketStatus.TO_BE_PAID) {
                this.checkInManager.acquire(ticketIdentifier);
            }
            checkIn(ticketIdentifier);
            updateRepository(eventId, ticketIdentifier, user, 
			t.getTicketsReservationId(), Audit.EventType.MANUAL_CHECK_IN, t.getId());
            return new TicketAndCheckInResult(t, new DefaultCheckInResult(SUCCESS, "success"));
        });
        return null;
    }
}
