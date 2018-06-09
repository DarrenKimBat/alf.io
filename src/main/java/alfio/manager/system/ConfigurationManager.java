/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.manager.system;

import alfio.manager.system.configuration.*;
import alfio.manager.user.UserManager;
import alfio.model.Event;
import alfio.model.modification.ConfigurationModification;
import alfio.model.system.Configuration;
import alfio.model.system.Configuration.*;
import alfio.model.system.ConfigurationKeys;
import alfio.model.system.ConfigurationPathLevel;
import alfio.repository.EventRepository;
import alfio.repository.system.ConfigurationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static alfio.model.system.ConfigurationKeys.*;
import static alfio.util.OptionalWrapper.optionally;

@Component
@Transactional
@Log4j2
@AllArgsConstructor
public class ConfigurationManager {


    public static final Predicate<ConfigurationModification> TO_BE_SAVED = c -> Optional.ofNullable(c.getId()).orElse(-1) > -1 || !StringUtils.isBlank(c.getValue());


    private final ConfigurationRepository configurationRepository;
    private final UserManager userManager;
    private final EventRepository eventRepository;

    //TODO: refactor, not the most beautiful code, find a better solution...
    private Configuration findByConfigurationPathAndKey(ConfigurationPath path, ConfigurationKeys key) {
        ConfigurationPathStrategy strategy = ConfigurationPathFactory.getPathStrategy(path.pathLevel(), configurationRepository);
        return strategy.findConfiguration(path, key);
    }

    /**
     * Select the most "precise" configuration in the given list.
     *
     * @param conf
     * @return
     */
    public static Configuration selectPath(List<Configuration> conf) {
        return conf.size() == 1 ? conf.get(0) : conf.stream()
            .sorted(Comparator.comparing(Configuration::getConfigurationPathLevel).reversed())
            .findFirst().orElse(null);
    }

    //meh
    @SuppressWarnings("unchecked")
    public static <T> T from(ConfigurationPath c) {
        return (T) c;
    }

    public int getIntConfigValue(ConfigurationPathKey pathKey, int defaultValue) {
        try {
            return Optional.ofNullable(findByConfigurationPathAndKey(pathKey.getPath(), pathKey.getKey()))
                .map(Configuration::getValue)
                .map(Integer::parseInt).orElse(defaultValue);
        } catch (NumberFormatException | EmptyResultDataAccessException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanConfigValue(ConfigurationPathKey pathKey, boolean defaultValue) {
        return getStringConfigValue(pathKey)
            .map(Boolean::parseBoolean)
            .orElse(defaultValue);
    }


    public String getStringConfigValue(ConfigurationPathKey pathKey, String defaultValue) {
        return getStringConfigValue(pathKey).orElse(defaultValue);
    }

    public Optional<String> getStringConfigValue(ConfigurationPathKey pathKey) {
        return optionally(() -> findByConfigurationPathAndKey(pathKey.getPath(), pathKey.getKey())).map(Configuration::getValue);
    }

    public Map<ConfigurationKeys, Optional<String>> getStringConfigValueFrom(ConfigurationPathKey... keys) {
        Map<ConfigurationKeys, Optional<String>> res = new HashMap<>();
        for(ConfigurationPathKey key : keys) {
            res.put(key.getKey(), getStringConfigValue(key));
        }
        return res;
    }

    public String getRequiredValue(ConfigurationPathKey pathKey) {
        return getStringConfigValue(pathKey)
            .orElseThrow(() -> new IllegalArgumentException("Mandatory configuration key " + pathKey.getKey() + " not present"));
    }

    // begin SYSTEM related configuration methods

    public void saveConfig(ConfigurationPathKey pathKey, String value) {
        ConfigurationPathStrategy strategy = ConfigurationPathFactory.getPathStrategy(pathKey.getPath().pathLevel(), configurationRepository) ;
        strategy.saveConfig(pathKey, value);
    }

    public void saveAllSystemConfiguration(List<ConfigurationModification> list) {
        new SystemPathStrategy(configurationRepository).saveAllSystemConfiguration(list);
    }

    public void saveAllOrganizationConfiguration(int organizationId, List<ConfigurationModification> list, String username) {
        new OrganizationPathStrategy(configurationRepository).saveAllOrganizationConfiguration(userManager, organizationId, list, username);
    }

    public void saveAllEventConfiguration(int eventId, int organizationId, List<ConfigurationModification> list, String username) {
        new EventPathStrategy(configurationRepository).saveAllEventConfiguration(userManager, eventRepository, eventId, organizationId, list, username);
    }

    public void saveCategoryConfiguration(int categoryId, int eventId, List<ConfigurationModification> list, String username) {
        new TicketCategoryStrategy(configurationRepository).saveCategoryConfiguration(userManager, eventRepository, categoryId, eventId, list, username);
    }

    public void saveSystemConfiguration(ConfigurationKeys key, String value) {
        new SystemPathStrategy(configurationRepository).saveSystemConfiguration(key, value);
    }

    public static Optional<String> evaluateValue(String key, String value) {
        if(ConfigurationKeys.fromString(key).isBooleanComponentType()) {
            return Optional.ofNullable(StringUtils.trimToNull(value));
        }
        return Optional.of(Objects.requireNonNull(value));
    }

    /**
     * Checks if the basic options have been already configured:
     * <ul>
     *     <li>Google maps' api keys</li>
     *     <li>Base application URL</li>
     *     <li>E-Mail</li>
     * </ul>
     * @return {@code true} if there are missing options, {@code true} otherwise
     */
    public boolean isBasicConfigurationNeeded() {
        return ConfigurationKeys.basic().stream()
            .anyMatch(key -> {
                boolean absent = !configurationRepository.findOptionalByKey(key.getValue()).isPresent();
                if (absent) {
                    log.warn("cannot find a value for " + key.getValue());
                }
                return absent;
            });
    }

    public static Predicate<Configuration> checkActualConfigurationLevel(boolean isAdmin, ConfigurationPathLevel level) {
        return conf -> isAdmin || conf.getConfigurationKey().supports(level);
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadOrganizationConfig(int organizationId, String username) {
        return new OrganizationPathStrategy(configurationRepository).loadOrganizationConfig(this, userManager, organizationId, username);
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadEventConfig(int eventId, String username) {
        return new EventPathStrategy(configurationRepository).loadEventConfig(this, userManager, eventRepository, eventId, username);

    }

    public Predicate<Event> areBooleanSettingsEnabledForEvent(ConfigurationKeys... keys) {
        return event -> Arrays.stream(keys)
            .allMatch(k -> getBooleanConfigValue(Configuration.from(event.getOrganizationId(), event.getId()).apply(k), false));
    }

    public static Map<ConfigurationKeys.SettingCategory, List<Configuration>> union(ConfigurationPathLevel... levels) {
        List<Configuration> configurations = Arrays.stream(levels)
            .sorted(ConfigurationPathLevel.COMPARATOR.reversed())
            .flatMap(l -> ConfigurationKeys.byPathLevel(l).stream().map(mapEmptyKeys(l)))
            .sorted((c1, c2) -> new CompareToBuilder().append(c2.getConfigurationPathLevel(), c1.getConfigurationPathLevel()).append(c1.getConfigurationKey(), c2.getConfigurationKey()).toComparison())
            .collect(LinkedList::new, (List<Configuration> list, Configuration conf) -> {
                int existing = (int) list.stream().filter(c -> c.getConfigurationKey() == conf.getConfigurationKey()).count();
                if (existing == 0) {
                    list.add(conf);
                }
            }, (l1, l2) -> {
            });
        return configurations.stream().collect(groupByCategory());
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadCategoryConfig(int eventId, int categoryId, String username) {
        return new TicketCategoryStrategy(configurationRepository).loadCategoryConfig(userManager, eventRepository, eventId, categoryId, username);
    }

    public static Map<ConfigurationKeys.SettingCategory, List<Configuration>> groupByCategory(Map<ConfigurationKeys.SettingCategory, List<Configuration>> all, Map<ConfigurationKeys.SettingCategory, List<Configuration>> existing) {
        return all.entrySet().stream()
            .map(e -> {
                Set<Configuration> entries = new TreeSet<>();
                ConfigurationKeys.SettingCategory key = e.getKey();
                entries.addAll(e.getValue());
                if(existing.containsKey(key)) {
                    List<Configuration> configurations = existing.get(key);
                    entries.removeAll(configurations);
                    entries.addAll(configurations);
                }
                return Pair.of(key, new ArrayList<>(entries));
            })
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadAllSystemConfigurationIncludingMissing(String username) {
        return new SystemPathStrategy(configurationRepository).loadAllSystemConfigurationIncludingMissing(userManager, username);
    }

    public static Collector<Configuration, ?, Map<ConfigurationKeys.SettingCategory, List<Configuration>>> groupByCategory() {
        return Collectors.groupingBy(c -> c.getConfigurationKey().getCategory());
    }

    public static Function<ConfigurationKeys, Configuration> mapEmptyKeys(ConfigurationPathLevel level) {
        return k -> new Configuration(-1, k.getValue(), null, k.getDescription(), level);
    }

    public void deleteKey(String key) {
        configurationRepository.deleteByKey(key);
    }

    public void deleteOrganizationLevelByKey(String key, int organizationId, String username) {
        new OrganizationPathStrategy(configurationRepository).deleteOrganizationLevelByKey(userManager, key, organizationId, username);
    }

    public void deleteEventLevelByKey(String key, int eventId, String username) {
        Event event = eventRepository.findById(eventId);
        Validate.notNull(event, "Wrong event id");
        Validate.isTrue(userManager.isOwnerOfOrganization(userManager.findUserByUsername(username), event.getOrganizationId()), "User is not owner of the organization. Therefore, delete is not allowed.");
        configurationRepository.deleteEventLevelByKey(key, eventId);
    }

    public void deleteCategoryLevelByKey(String key, int eventId, int categoryId, String username) {
        Event event = eventRepository.findById(eventId);
        Validate.notNull(event, "Wrong event id");
        Validate.isTrue(userManager.isOwnerOfOrganization(userManager.findUserByUsername(username), event.getOrganizationId()), "User is not owner of the organization. Therefore, delete is not allowed.");
        configurationRepository.deleteCategoryLevelByKey(key, eventId, categoryId);
    }

    public static Map<ConfigurationKeys.SettingCategory, List<Configuration>> collectConfigurationKeysByCategory(ConfigurationPathLevel pathLevel) {
        return ConfigurationKeys.byPathLevel(pathLevel)
            .stream()
            .map(mapEmptyKeys(pathLevel))
            .sorted()
            .collect(groupByCategory());
    }

    public String getShortReservationID(Event event, String reservationId) {
        return StringUtils.substring(reservationId, 0, getIntConfigValue(Configuration.from(event.getOrganizationId(), event.getId(), PARTIAL_RESERVATION_ID_LENGTH), 8)).toUpperCase();
    }

    public boolean hasAllConfigurationsForInvoice(Event event) {
        return getStringConfigValue(Configuration.from(event.getOrganizationId(), event.getId(), ConfigurationKeys.INVOICE_ADDRESS)).isPresent() &&
            getStringConfigValue(Configuration.from(event.getOrganizationId(), event.getId(), ConfigurationKeys.VAT_NR)).isPresent();
    }

    public boolean isRecaptchaForOfflinePaymentEnabled(Event event) {
        return getBooleanConfigValue(Configuration.from(event.getOrganizationId(), event.getId(), ENABLE_CAPTCHA_FOR_OFFLINE_PAYMENTS), false)
            && getStringConfigValue(Configuration.getSystemConfiguration(ENABLE_CAPTCHA_FOR_OFFLINE_PAYMENTS), null) != null;
    }
}
