package alfio.manager.system.configuration;

import alfio.manager.system.ConfigurationManager;
import alfio.model.system.Configuration;
import alfio.model.system.ConfigurationKeys;
import alfio.repository.system.ConfigurationRepository;

public interface ConfigurationPathStrategy {

    abstract public Configuration findConfiguration(Configuration.ConfigurationPath path, ConfigurationKeys key);
    abstract public void saveConfig(Configuration.ConfigurationPathKey pathKey, String value);
}

