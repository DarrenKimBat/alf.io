package alfio.manager;


import static alfio.manager.support.CheckInStatus.SUCCESS;

import java.time.ZonedDateTime;
import java.util.Date;

import org.apache.commons.lang3.Validate;

import alfio.manager.support.TicketAndCheckInResult;
import alfio.model.Audit;
import alfio.model.Ticket;
import alfio.model.Ticket.TicketStatus;
import alfio.model.audit.ScanAudit;
import alfio.repository.AuditingRepository;
import alfio.repository.TicketRepository;
import alfio.repository.audit.ScanAuditRepository;
import alfio.repository.user.UserRepository;


public abstract class CheckInMethod {
	protected CheckInManager checkInManager;
	protected final TicketRepository ticketRepository;
	protected final ExtensionManager extensionManager;
	protected final ScanAuditRepository scanAuditRepository;
    protected final AuditingRepository auditingRepository;
    protected final UserRepository userRepository;
	
	protected void checkIn(String uuid) {
        Ticket ticket = ticketRepository.findByUUID(uuid);
        Validate.isTrue(ticket.getStatus() == TicketStatus.ACQUIRED);
        ticketRepository.updateTicketStatusWithUUID(uuid, TicketStatus.CHECKED_IN.toString());
        ticketRepository.toggleTicketLocking(ticket.getId(), ticket.getCategoryId(), true);
        extensionManager.handleTicketCheckedIn(ticketRepository.findByUUID(uuid));
    }
	
	protected void updateRepository(int eventId, String ticketIdentifier, String user, 
			String TicketsReservationId, Audit.EventType eventType, int ticketId) {
		scanAuditRepository.insert(ticketIdentifier, eventId, ZonedDateTime.now(), user, SUCCESS, ScanAudit.Operation.SCAN);
        auditingRepository.insert(TicketsReservationId, 
        		userRepository.findIdByUserName(user).orElse(null), 
        		eventId, eventType, new Date(),
        		Audit.EntityType.TICKET, Integer.toString(ticketId));
	}

	public CheckInMethod(CheckInManager checkInManager, TicketRepository ticketRepository,
			ExtensionManager extensionManager, ScanAuditRepository scanAuditRepository,
			AuditingRepository auditingRepository, UserRepository userRepository) {
		this.checkInManager = checkInManager;
		this.ticketRepository = ticketRepository;
		this.extensionManager = extensionManager;
		this.scanAuditRepository = scanAuditRepository;
		this.auditingRepository = auditingRepository;
		this.userRepository = userRepository;
	}

	public abstract boolean executeCheckIn(int eventId, String ticketIdentifier, String user);
	public abstract TicketAndCheckInResult checkInAndGetResult(int eventId, String ticketIdentifier, String user);

}
