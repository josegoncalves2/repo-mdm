package com.hmdm.plugins.webfilter.persistence.mapper;

import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterDelivery;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEvent;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>An ORM mapper for the web filter plugin tables. Every query that takes a customer ID is scoped by it; callers
 * decide whether the customer comes from the security context or from a trusted source (device sync, background task).</p>
 */
public interface WebFilterMapper {

    // ------------------------------------------------------------------------------------------------- policies
    @Select("SELECT * FROM plugin_webfilter_policies WHERE customerId = #{customerId} AND configurationId = #{configurationId}")
    WebFilterPolicy findPolicy(@Param("customerId") int customerId, @Param("configurationId") int configurationId);

    @Select("SELECT * FROM plugin_webfilter_policies WHERE customerId = #{customerId} ORDER BY configurationId")
    List<WebFilterPolicy> findPoliciesByCustomer(@Param("customerId") int customerId);

    // Stable order: the generated blocky.yml must not change (and restart the resolver) when only a row moves
    @Select("SELECT * FROM plugin_webfilter_policies WHERE enabled ORDER BY customerId, configurationId")
    List<WebFilterPolicy> findAllEnabledPolicies();

    @Insert("INSERT INTO plugin_webfilter_policies (customerId, configurationId, enabled, updatedAt, updatedBy) " +
            "VALUES (#{customerId}, #{configurationId}, #{enabled}, #{updatedAt}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertPolicy(WebFilterPolicy policy);

    @Update("UPDATE plugin_webfilter_policies SET enabled = #{enabled}, updatedAt = #{updatedAt}, updatedBy = #{updatedBy} " +
            "WHERE id = #{id} AND customerId = #{customerId}")
    int updatePolicy(WebFilterPolicy policy);

    // ------------------------------------------------------------------------------------------------- categories
    @Select("SELECT category FROM plugin_webfilter_policy_categories WHERE policyId = #{policyId} ORDER BY category")
    List<String> findCategories(@Param("policyId") int policyId);

    @Delete("DELETE FROM plugin_webfilter_policy_categories WHERE policyId = #{policyId}")
    void deleteCategories(@Param("policyId") int policyId);

    @Insert("INSERT INTO plugin_webfilter_policy_categories (policyId, category) VALUES (#{policyId}, #{category})")
    void insertCategory(@Param("policyId") int policyId, @Param("category") String category);

    // ------------------------------------------------------------------------------------------------- entries
    @Select("SELECT kind, list, value FROM plugin_webfilter_policy_entries WHERE policyId = #{policyId} ORDER BY kind, list, value")
    List<WebFilterEntry> findEntries(@Param("policyId") int policyId);

    @Delete("DELETE FROM plugin_webfilter_policy_entries WHERE policyId = #{policyId}")
    void deleteEntries(@Param("policyId") int policyId);

    @Insert("INSERT INTO plugin_webfilter_policy_entries (policyId, kind, list, value) " +
            "VALUES (#{policyId}, #{e.kind}, #{e.list}, #{e.value})")
    void insertEntry(@Param("policyId") int policyId, @Param("e") WebFilterEntry entry);

    // ------------------------------------------------------------------------------------------------- app categories
    @Select("SELECT * FROM plugin_webfilter_app_categories WHERE customerId = #{customerId} ORDER BY category, packageName")
    List<WebFilterAppCategory> findAppCategories(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_webfilter_app_categories (customerId, packageName, category) " +
            "VALUES (#{customerId}, #{packageName}, #{category}) ON CONFLICT DO NOTHING")
    int insertAppCategory(WebFilterAppCategory item);

    @Delete("DELETE FROM plugin_webfilter_app_categories WHERE id = #{id} AND customerId = #{customerId}")
    int deleteAppCategory(@Param("id") int id, @Param("customerId") int customerId);

    // ------------------------------------------------------------------------------------------------- locked history
    @Select("SELECT packageName FROM plugin_webfilter_locked_history WHERE policyId = #{policyId}")
    List<String> findLockedHistory(@Param("policyId") int policyId);

    @Insert("INSERT INTO plugin_webfilter_locked_history (policyId, packageName) VALUES (#{policyId}, #{packageName}) " +
            "ON CONFLICT DO NOTHING")
    void insertLockedHistory(@Param("policyId") int policyId, @Param("packageName") String packageName);

    // ------------------------------------------------------------------------------------------------- settings
    @Select("SELECT customerId, dnsDomain FROM plugin_webfilter_settings WHERE customerId = #{customerId}")
    WebFilterSettings findSettings(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_webfilter_settings (customerId, dnsDomain) VALUES (#{customerId}, #{dnsDomain}) " +
            "ON CONFLICT (customerId) DO UPDATE SET dnsDomain = EXCLUDED.dnsDomain")
    void saveSettings(WebFilterSettings settings);

    // ------------------------------------------------------------------------------------------------- delivery
    @Insert("INSERT INTO plugin_webfilter_delivery (deviceId, customerId, configurationId, enabled, policyUpdatedAt, " +
            "deliveredAt, browserSites, hiddenApps) VALUES (#{deviceId}, #{customerId}, #{configurationId}, #{enabled}, " +
            "#{policyUpdatedAt}, #{deliveredAt}, #{browserSites}, #{hiddenApps}) " +
            "ON CONFLICT (deviceId) DO UPDATE SET customerId = EXCLUDED.customerId, " +
            "configurationId = EXCLUDED.configurationId, enabled = EXCLUDED.enabled, " +
            "policyUpdatedAt = EXCLUDED.policyUpdatedAt, deliveredAt = EXCLUDED.deliveredAt, " +
            "browserSites = EXCLUDED.browserSites, hiddenApps = EXCLUDED.hiddenApps")
    void saveDelivery(WebFilterDelivery delivery);

    // Every device of a profile that has a web filter policy, whether it already synced or not
    @Select("SELECT d.id AS deviceId, d.customerId, d.configurationId, d.number AS deviceNumber, " +
            "c.name AS configurationName, d.lastUpdate, w.enabled, w.policyUpdatedAt, w.deliveredAt, " +
            "w.browserSites, w.hiddenApps " +
            "FROM devices d " +
            "JOIN configurations c ON c.id = d.configurationId " +
            "JOIN plugin_webfilter_policies p ON p.configurationId = d.configurationId AND p.customerId = d.customerId " +
            "LEFT JOIN plugin_webfilter_delivery w ON w.deviceId = d.id " +
            "WHERE d.customerId = #{customerId} " +
            "ORDER BY c.name, d.number LIMIT 1000")
    List<WebFilterDelivery> findDeliveries(@Param("customerId") int customerId);

    @Select("SELECT id, customerId, configurationId, number, publicIp FROM devices WHERE publicIp = #{ip}")
    java.util.List<java.util.Map<String, Object>> findDnsDevices(@Param("ip") String ip);

    @Insert("INSERT INTO plugin_webfilter_events (customerId,deviceId,configurationId,host,category,source,createdAt,clientIp,sourceKey) " +
            "VALUES (#{customerId},#{deviceId},#{configurationId},#{host},#{category},'dns',#{createdAt},#{clientIp},#{sourceKey}) ON CONFLICT (sourceKey) DO NOTHING")
    void insertDnsEvent(WebFilterEvent event);

    @Select("SELECT urls FROM plugin_webfilter_sources WHERE category = #{category}")
    String sourceUrls(@Param("category") String category);
    @Insert("INSERT INTO plugin_webfilter_sources(category,urls) VALUES(#{category},#{urls}) ON CONFLICT(category) DO UPDATE SET urls = EXCLUDED.urls")
    void saveSourceUrls(@Param("category") String category, @Param("urls") String urls);

    // ------------------------------------------------------------------------------------------------- events
    @Insert("INSERT INTO plugin_webfilter_events (customerId, deviceId, configurationId, host, url, category, source, " +
            "createdAt) VALUES (#{customerId}, #{deviceId}, #{configurationId}, #{host}, #{url}, #{category}, " +
            "#{source}, #{createdAt})")
    void insertEvent(WebFilterEvent event);

    @Select("SELECT COUNT(*) FROM plugin_webfilter_events WHERE deviceId = #{deviceId} AND host = #{host} " +
            "AND createdAt >= #{since}")
    int countRecentEvents(@Param("deviceId") int deviceId, @Param("host") String host, @Param("since") long since);

    @Select("SELECT COUNT(*) FROM plugin_webfilter_events WHERE deviceId = #{deviceId} AND createdAt >= #{since}")
    int countDeviceEvents(@Param("deviceId") int deviceId, @Param("since") long since);

    @Select("SELECT e.*, d.number AS deviceNumber FROM plugin_webfilter_events e " +
            "LEFT JOIN devices d ON d.id = e.deviceId " +
            "WHERE e.customerId = #{customerId} ORDER BY e.createdAt DESC LIMIT #{limit}")
    List<WebFilterEvent> findRecentEvents(@Param("customerId") int customerId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM plugin_webfilter_events WHERE customerId = #{customerId} AND createdAt >= #{since}")
    int countEvents(@Param("customerId") int customerId, @Param("since") long since);

    @Delete("DELETE FROM plugin_webfilter_events WHERE createdAt < #{before}")
    int purgeEvents(@Param("before") long before);
}
