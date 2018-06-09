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

public class CheckInByTicketCode extends CheckInMethod {
	private final Optional<String> ticketCode;
	
	public CheckInByTicketCode(CheckInManager checkInManager, TicketRepository ticketRepository,
			ExtensionManager extensionManager, ScanAuditRepository scanAuditRepository,
			AuditingRepository auditingRepository, UserRepository userRepository,
			Optional<String> ticketCode) {
		super(checkInManager, ticketRepository, extensionManager, scanAuditRepository, auditingRepository, userRepository);
		this.ticketCode = ticketCode;
	}
	
	public boolean executeCheckIn(int eventId, String ticketIdentifier, String user) {
		TicketAndCheckInResult descriptor = this.checkInManager.extractStatus(eventId, ticketRepository.findByUUIDForUpdate(ticketIdentifier), ticketIdentifier, ticketCode);
        if(descriptor.getResult().getStatus() == OK_READY_TO_BE_CHECKED_IN) {
            checkIn(ticketIdentifier);
            updateRepository(eventId, ticketIdentifier, user, descriptor.getTicket().getTicketsReservationId(), 
            		Audit.EventType.CHECK_IN, descriptor.getTicket().getId());
            return true;        }
        return false;
	}
	
	public TicketAndCheckInResult checkInAndGetResult(int eventId, String ticketIdentifier, String user) {
        TicketAndCheckInResult descriptor = this.checkInManager.extractStatus(eventId, ticketRepository.findByUUIDForUpdate(ticketIdentifier), ticketIdentifier, ticketCode);
        if(descriptor.getResult().getStatus() == OK_READY_TO_BE_CHECKED_IN) {
            checkIn(ticketIdentifier);
            scanAuditRepository.insert(ticketIdentifier, eventId, ZonedDateTime.now(), user, SUCCESS, ScanAudit.Operation.SCAN);
            auditingRepository.insert(descriptor.getTicket().getTicketsReservationId(), userRepository.findIdByUserName(user).orElse(null), eventId, Audit.EventType.CHECK_IN, new Date(), Audit.EntityType.TICKET, Integer.toString(descriptor.getTicket().getId()));
            return new TicketAndCheckInResult(descriptor.getTicket(), new DefaultCheckInResult(SUCCESS, "success"));
        }
        return descriptor;
    }

}
