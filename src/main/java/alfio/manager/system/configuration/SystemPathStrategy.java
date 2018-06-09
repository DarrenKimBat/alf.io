package alfio.manager.system.configuration;

import alfio.manager.system.ConfigurationManager;
import alfio.manager.user.UserManager;
import alfio.model.modification.ConfigurationModification;
import alfio.model.system.Configuration;
import alfio.model.system.ConfigurationKeys;
import alfio.model.system.ConfigurationPathLevel;
import alfio.repository.system.ConfigurationRepository;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.StringUtil;
import sun.swing.StringUIClientPropertyKey;

import java.util.*;
import java.util.stream.Collectors;

import static alfio.manager.system.ConfigurationManager.groupByCategory;
import static alfio.manager.system.ConfigurationManager.mapEmptyKeys;
import static alfio.util.OptionalWrapper.optionally;

public class SystemPathStrategy implements ConfigurationPathStrategy {

    private ConfigurationRepository configurationRepository;


    public SystemPathStrategy(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public Configuration findConfiguration(Configuration.ConfigurationPath path, ConfigurationKeys key) {

        return configurationRepository.findByKey(key.getValue());
    }

    @Override
    public void saveConfig(Configuration.ConfigurationPathKey pathKey, String value) {
        saveSystemConfiguration(pathKey.getKey(), value);
    }

    public void saveSystemConfiguration(ConfigurationKeys key, String value) {
        Optional<Configuration> conf = optionally(() -> findConfiguration(Configuration.system(), key));
        if(key.isBooleanComponentType()) {
            Optional<Boolean> state = getThreeStateValue(value);
            if(conf.isPresent()) {
                if(state.isPresent()) {
                    configurationRepository.update(key.getValue(), value);
                } else {
                    configurationRepository.deleteByKey(key.getValue());
                }
            } else {
                state.ifPresent(v -> configurationRepository.insert(key.getValue(), v.toString(), key.getDescription()));
            }
        } else {
            Optional<String> valueOpt = Optional.ofNullable(value);
            if(!conf.isPresent()) {
                valueOpt.ifPresent(v -> configurationRepository.insert(key.getValue(), v, key.getDescription()));
            } else {
                configurationRepository.update(key.getValue(), value);
            }
        }
    }

    public void saveAllSystemConfiguration(List<ConfigurationModification> list) {
        list.forEach(c -> saveSystemConfiguration(ConfigurationKeys.fromString(c.getKey()), c.getValue()));
    }

    private Optional<Boolean> getThreeStateValue(String value) {
        return Optional.ofNullable(StringUtils.trimToNull(value)).map(Boolean::parseBoolean);
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadAllSystemConfigurationIncludingMissing(UserManager userManager, String username) {
        if(!userManager.isAdmin(userManager.findUserByUsername(username))) {
            return Collections.emptyMap();
        }
        final List<Configuration> existing = configurationRepository.findSystemConfiguration()
            .stream()
            .filter(c -> !ConfigurationKeys.fromString(c.getKey()).isInternal())
            .collect(Collectors.toList());
        final List<Configuration> missing = Arrays.stream(ConfigurationKeys.visible())
            .filter(k -> existing.stream().noneMatch(c -> c.getKey().equals(k.getValue())))
            .map(mapEmptyKeys(ConfigurationPathLevel.SYSTEM))
            .collect(Collectors.toList());
        List<Configuration> result = new LinkedList<>(existing);
        result.addAll(missing);
        return result.stream().sorted().collect(groupByCategory());
    }
}
