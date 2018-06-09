package alfio.manager.system.configuration;

import alfio.model.system.ConfigurationPathLevel;
import alfio.repository.system.ConfigurationRepository;

public class ConfigurationPathFactory {

    public static ConfigurationPathStrategy getPathStrategy(ConfigurationPathLevel level, ConfigurationRepository repository) {
        ConfigurationPathStrategy ret;
        switch(level) {
            case SYSTEM:
                ret = new SystemPathStrategy(repository);
                break;
            case EVENT:
                ret = new EventPathStrategy(repository);
                break;
            case ORGANIZATION:
                ret = new OrganizationPathStrategy(repository);
                break;
            case TICKET_CATEGORY:
                ret = new TicketCategoryStrategy(repository);
                break;
            default:
                throw new IllegalStateException("Can't reach here");
        }

        return ret;
    }

}
