package alfio.manager.system.configuration;

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
import java.util.stream.Collectors;

import static alfio.manager.system.ConfigurationManager.*;
import static alfio.model.system.ConfigurationKeys.ALFIO_PI_INTEGRATION_ENABLED;
import static alfio.model.system.ConfigurationKeys.OFFLINE_CHECKIN_ENABLED;
import static alfio.model.system.ConfigurationPathLevel.EVENT;
import static alfio.model.system.ConfigurationPathLevel.SYSTEM;


public class EventPathStrategy implements ConfigurationPathStrategy {

    public static final Map<ConfigurationKeys.SettingCategory, List<Configuration>> EVENT_CONFIGURATION = collectConfigurationKeysByCategory(ConfigurationPathLevel.EVENT);
    private ConfigurationRepository configurationRepository;

    public EventPathStrategy(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public Configuration findConfiguration(Configuration.ConfigurationPath path, ConfigurationKeys key) {

        Configuration.EventConfigurationPath o = ConfigurationManager.from(path);
        return ConfigurationManager.selectPath(configurationRepository.findByEventAndKey(
            o.getOrganizationId(),
            o.getId(),
            key.getValue()
        ));
    }

    @Override
    public void saveConfig(Configuration.ConfigurationPathKey pathKey, String value) {
        Configuration.ConfigurationPath path = pathKey.getPath();
        Configuration.EventConfigurationPath eventPath = (Configuration.EventConfigurationPath) path;

        saveEventConfiguration(eventPath.getId(), eventPath.getOrganizationId(), pathKey.getKey().name(), value);
    }

    public void saveEventConfiguration(int eventId, int organizationId, String key, String optionValue) {
        Optional<Configuration> existing = configurationRepository.findByKeyAtEventLevel(eventId, organizationId, key);
        Optional<String> value = ConfigurationManager.evaluateValue(key, optionValue);
        if(!value.isPresent()) {
            configurationRepository.deleteEventLevelByKey(key, eventId);
        } else if (existing.isPresent()) {
            configurationRepository.updateEventLevel(eventId, organizationId, key, value.get());
        } else {
            configurationRepository.insertEventLevel(organizationId, eventId, key, value.get(), ConfigurationKeys.fromString(key).getDescription());
        }
    }


    public void saveAllEventConfiguration(UserManager userManager, EventRepository eventRepository, int eventId, int organizationId, List<ConfigurationModification> list, String username) {
        User user = userManager.findUserByUsername(username);
        Validate.isTrue(userManager.isOwnerOfOrganization(user, organizationId), "Cannot update settings, user is not owner");
        Event event = eventRepository.findById(eventId);
        Validate.notNull(event, "event does not exist");
        if(organizationId != event.getOrganizationId()) {
            Validate.isTrue(userManager.isOwnerOfOrganization(user, event.getOrganizationId()), "Cannot update settings, user is not owner of event");
        }
        list.stream()
            .filter(ConfigurationManager.TO_BE_SAVED)
            .forEach(c -> saveEventConfiguration(eventId, organizationId, c.getKey(), c.getValue()));
    }


    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadEventConfig(ConfigurationManager configurationManager, UserManager userManager, EventRepository eventRepository, int eventId, String username) {
        User user = userManager.findUserByUsername(username);
        Event event = eventRepository.findById(eventId);
        int organizationId = event.getOrganizationId();
        if(!userManager.isOwnerOfOrganization(user, organizationId)) {
            return Collections.emptyMap();
        }
        boolean isAdmin = userManager.isAdmin(user);
        Map<ConfigurationKeys.SettingCategory, List<Configuration>> existing = configurationRepository.findEventConfiguration(organizationId, eventId).stream().filter(checkActualConfigurationLevel(isAdmin, EVENT)).sorted().collect(groupByCategory());
        boolean offlineCheckInEnabled = configurationManager.areBooleanSettingsEnabledForEvent(ALFIO_PI_INTEGRATION_ENABLED, OFFLINE_CHECKIN_ENABLED).test(event);
        return removeAlfioPISettingsIfNeeded(offlineCheckInEnabled, groupByCategory(isAdmin ? union(SYSTEM, EVENT) : EVENT_CONFIGURATION, existing));
    }

    private static Map<ConfigurationKeys.SettingCategory, List<Configuration>> removeAlfioPISettingsIfNeeded(boolean offlineCheckInEnabled, Map<ConfigurationKeys.SettingCategory, List<Configuration>> settings) {
        if(offlineCheckInEnabled) {
            return settings;
        }
        return settings.entrySet().stream()
            .filter(e -> e.getKey() != ConfigurationKeys.SettingCategory.ALFIO_PI)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
