package alfio.manager.system.configuration;

import alfio.manager.system.ConfigurationManager;
import alfio.manager.user.UserManager;
import alfio.model.modification.ConfigurationModification;
import alfio.model.system.Configuration;
import alfio.model.system.ConfigurationKeys;
import alfio.model.transaction.PaymentProxy;
import alfio.model.user.User;
import alfio.repository.system.ConfigurationRepository;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static alfio.manager.system.ConfigurationManager.collectConfigurationKeysByCategory;
import static alfio.manager.system.ConfigurationManager.union;
import static alfio.model.system.ConfigurationPathLevel.ORGANIZATION;
import static alfio.model.system.ConfigurationPathLevel.SYSTEM;
import static alfio.manager.system.ConfigurationManager.groupByCategory;


public class OrganizationPathStrategy implements ConfigurationPathStrategy {

    public static final Map<ConfigurationKeys.SettingCategory, List<Configuration>> ORGANIZATION_CONFIGURATION = collectConfigurationKeysByCategory(ORGANIZATION);

    private ConfigurationRepository configurationRepository;

    public OrganizationPathStrategy(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @Override
    public Configuration findConfiguration(Configuration.ConfigurationPath path, ConfigurationKeys key) {
        Configuration.OrganizationConfigurationPath o = ConfigurationManager.from(path);
        return ConfigurationManager.selectPath(configurationRepository.findByOrganizationAndKey(o.getId(), key.getValue()));
    }

    @Override
    public void saveConfig(Configuration.ConfigurationPathKey pathKey, String value) {
        Configuration.OrganizationConfigurationPath orgPath = (Configuration.OrganizationConfigurationPath) pathKey.getPath();

        saveOrganizationConfiguration(orgPath.getId(), pathKey.getKey().name(), value);
    }

    public void saveOrganizationConfiguration(int organizationId, String key, String optionValue) {
        Optional<String> value = ConfigurationManager.evaluateValue(key, optionValue);
        Optional<Configuration> existing = configurationRepository.findByKeyAtOrganizationLevel(organizationId, key);
        if (!value.isPresent()) {
            configurationRepository.deleteOrganizationLevelByKey(key, organizationId);
        } else if (existing.isPresent()) {
            configurationRepository.updateOrganizationLevel(organizationId, key, value.get());
        } else {
            configurationRepository.insertOrganizationLevel(organizationId, key, value.get(), ConfigurationKeys.fromString(key).getDescription());
        }
    }


    public void saveAllOrganizationConfiguration(UserManager userManager, int organizationId, List<ConfigurationModification> list, String username) {
        Validate.isTrue(userManager.isOwnerOfOrganization(userManager.findUserByUsername(username), organizationId), "Cannot update settings, user is not owner");
        list.stream()
            .filter(ConfigurationManager.TO_BE_SAVED)
            .forEach(c -> saveOrganizationConfiguration(organizationId, c.getKey(), c.getValue()));
    }

    public Map<ConfigurationKeys.SettingCategory, List<Configuration>> loadOrganizationConfig(ConfigurationManager configurationManager, UserManager userManager, int organizationId, String username) {
        User user = userManager.findUserByUsername(username);
        if(!userManager.isOwnerOfOrganization(user, organizationId)) {
            return Collections.emptyMap();
        }
        boolean isAdmin = userManager.isAdmin(user);
        Map<ConfigurationKeys.SettingCategory, List<Configuration>> existing = configurationRepository.findOrganizationConfiguration(organizationId).stream().filter(ConfigurationManager.checkActualConfigurationLevel(isAdmin, ORGANIZATION)).sorted().collect(groupByCategory());
        String paymentMethodsBlacklist = configurationManager.getStringConfigValue(Configuration.from(organizationId, ConfigurationKeys.PAYMENT_METHODS_BLACKLIST), "");
        Map<ConfigurationKeys.SettingCategory, List<Configuration>> result = ConfigurationManager.groupByCategory(isAdmin ? union(SYSTEM, ORGANIZATION) : ORGANIZATION_CONFIGURATION, existing);
        List<ConfigurationKeys.SettingCategory> toBeRemoved = PaymentProxy.availableProxies()
            .stream()
            .filter(pp -> paymentMethodsBlacklist.contains(pp.getKey()))
            .flatMap(pp -> pp.getSettingCategories().stream())
            .collect(Collectors.toList());

        if(toBeRemoved.isEmpty()) {
            return result;
        } else {
            return result.entrySet().stream()
                .filter(entry -> !toBeRemoved.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    public void deleteOrganizationLevelByKey(UserManager userManager, String key, int organizationId, String username) {
        Validate.isTrue(userManager.isOwnerOfOrganization(userManager.findUserByUsername(username), organizationId), "User is not owner of the organization. Therefore, delete is not allowed.");
        configurationRepository.deleteOrganizationLevelByKey(key, organizationId);
    }
}
