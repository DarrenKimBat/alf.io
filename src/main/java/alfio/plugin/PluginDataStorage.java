package alfio.plugin;

import alfio.model.Event;
import alfio.model.plugin.PluginConfigOption;
import alfio.model.plugin.PluginLog;
import alfio.model.system.ComponentType;
import alfio.repository.EventRepository;
import alfio.repository.plugin.PluginConfigurationRepository;
import alfio.repository.plugin.PluginLogRepository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;

public class PluginDataStorage {
    private final String pluginId;
    private final PluginConfigurationRepository pluginConfigurationRepository;
    private final PluginLogRepository pluginLogRepository;
    private final TransactionTemplate tx;
    private final EventRepository eventRepository;

    public PluginDataStorage(String pluginId,
                              PluginConfigurationRepository pluginConfigurationRepository,
                              PluginLogRepository pluginLogRepository, TransactionTemplate tx, EventRepository eventRepository) {
        this.pluginId = pluginId;
        this.pluginConfigurationRepository = pluginConfigurationRepository;
        this.pluginLogRepository = pluginLogRepository;
        this.tx = tx;
        this.eventRepository = eventRepository;
    }

    public Event getEventById(int eventId) {
        return eventRepository.findById(eventId);
    }

    public Optional<String> getConfigValue(String name, int eventId) {
        return pluginConfigurationRepository.loadSingleOption(pluginId, eventId, name).map(PluginConfigOption::getOptionValue);
    }

    public void insertConfigValue(int eventId, String name, String value, String description, ComponentType componentType) {
        pluginConfigurationRepository.insert(pluginId, eventId, name, value, description, componentType);
    }

    public void registerSuccess(String description, int eventId) {
        tx.execute(tc -> {
            pluginLogRepository.insertEvent(pluginId, eventId, description, PluginLog.Type.SUCCESS, ZonedDateTime.now(Clock.systemUTC()));
            return null;
        });
    }

    public void registerFailure(String description, int eventId) {
        tx.execute(tc -> {
            pluginLogRepository.insertEvent(pluginId, eventId, description, PluginLog.Type.ERROR, ZonedDateTime.now(Clock.systemUTC()));
            return null;
        });
    }

    public void registerWarning(String description, int eventId) {
        tx.execute(tc -> {
            pluginLogRepository.insertEvent(pluginId, eventId, description, PluginLog.Type.WARNING, ZonedDateTime.now(Clock.systemUTC()));
            return null;
        });
    }

}
