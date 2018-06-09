package alfio.manager.system.configuration;

import alfio.manager.EventManager;
import alfio.manager.system.ConfigurationManager;
import alfio.manager.user.UserManager;
import alfio.model.Event;
import alfio.model.modification.ConfigurationModification;
import alfio.model.system.Configuration;
import alfio.model.system.ConfigurationKeys;
import alfio.model.system.ConfigurationPathLevel;
import alfio.model.user.User;
import alfio.repository.EventRepository;
import alfio.repository.system.ConfigurationRepository;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static alfio.manager.system.ConfigurationManager.collectConfigurationKeysByCategory;
import static alfio.manager.system.ConfigurationManager.groupByCategory;


public class TicketCategoryStrategy implements ConfigurationPathStrategy {

    private static final Map<ConfigurationKeys.SettingCategory, List<Configuration>> CATEGORY_CONFIGURATION = collectConfigurationKeysByCategory(ConfigurationPathLevel.TICKET_CATEGORY);

    private ConfigurationRepository configurationRepository;

    public TicketCategoryStrategy(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public Configuration findConfiguration(Configuration.ConfigurationPath path, ConfigurationKeys key) {

        Configuration.TicketCategoryConfigurationPath o = ConfigurationManager.from(path);
        return ConfigurationManager.selectPath(configurationRepository.findByTicketCategoryAndKey(
            o.getOrganizationId(),
            o.getEventId(),
            o.getId(),
            key.getValue()
        ));
    }

    @Override
    public void saveConfig(Configuration.ConfigurationPathKey pathKey, String value) {

    }

    public void saveCategoryConfiguration(UserManager userManager, EventRepository eventRepository, int categoryId, int eventId, List<ConfigurationModification> list, String username) {
        User user = userManager.findUserByUsername(username);
        Event event = eventRepository.findById(eventId);
        Validate.notNull(event, "event does not exist");
        Validate.isTrue(userManager.isOwnerOfOrganization(user, event.getOrganizationId()), "Cannot update settings, user is not owner of event");
        list.stream()
            .filter(ConfigurationManager.TO_BE_SAVED)
            .forEach(c -> {
                Optional<Configuration> existing = configurationRepository.findByKeyAtCategoryLevel(eventId, event.getOrganizationId(), categoryId, c.getKey());
                Optional<String> value = ConfigurationManager.evaluateValue(c.getKey(), c.getValue());
                if(!value.isPresent()) {
                    configurationRepository.deleteCategoryLevelByKey(c.getKey(), eventId, categoryId);
                } else if (existing.isPresent()) {
                    configurationRepository.updateCategoryLevel(eventId, event.getOrganizationId(), categoryId, c.getKey(), value.get());
                } else {
                    configurationRepository.insertTicketCategoryLevel(event.getOrganizationId(), eventId, categoryId, c.getKey(), value.get(), ConfigurationKeys.fromString(c.getKey()).getDescription());
                }
            });
    }


    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadCategoryConfig(UserManager userManager, EventRepository eventRepository, int eventId, int categoryId, String username) {
        User user = userManager.findUserByUsername(username);
        Event event = eventRepository.findById(eventId);
        int organizationId = event.getOrganizationId();
        if(!userManager.isOwnerOfOrganization(user, organizationId)) {
            return Collections.emptyMap();
        }
        Map<ConfigurationKeys.SettingCategory, List<Configuration>> existing = configurationRepository.findCategoryConfiguration(organizationId, eventId, categoryId).stream().sorted().collect(groupByCategory());
        return groupByCategory(CATEGORY_CONFIGURATION, existing);
    }
}
