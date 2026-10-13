package com.hmdm.plugins.webfilter.persistence.mapper;

import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterDelivery;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEvent;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterPolicy;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.rest.json.AttributionView;
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
    @Select("SELECT customerId, dnsDomain, blockPageTitle, blockPageMessage, blockPageLogoUrl, " +
            "blockPageSupportText, blockPageCustomHtml, blockPageCustomCss " +
            "FROM plugin_webfilter_settings WHERE customerId = #{customerId}")
    WebFilterSettings findSettings(@Param("customerId") int customerId);

    @Insert("INSERT INTO plugin_webfilter_settings (customerId, dnsDomain, blockPageTitle, blockPageMessage, " +
            "blockPageLogoUrl, blockPageSupportText, blockPageCustomHtml, blockPageCustomCss) VALUES " +
            "(#{customerId}, #{dnsDomain}, #{blockPageTitle}, #{blockPageMessage}, #{blockPageLogoUrl}, " +
            "#{blockPageSupportText}, #{blockPageCustomHtml}, #{blockPageCustomCss}) " +
            "ON CONFLICT (customerId) DO UPDATE SET dnsDomain = EXCLUDED.dnsDomain, " +
            "blockPageTitle = EXCLUDED.blockPageTitle, blockPageMessage = EXCLUDED.blockPageMessage, " +
            "blockPageLogoUrl = EXCLUDED.blockPageLogoUrl, blockPageSupportText = EXCLUDED.blockPageSupportText, " +
            "blockPageCustomHtml = EXCLUDED.blockPageCustomHtml, blockPageCustomCss = EXCLUDED.blockPageCustomCss")
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
    // Subset of "urls" kept registered but left out of the filter, one per line
    @Select("SELECT inactiveUrls FROM plugin_webfilter_sources WHERE category = #{category}")
    String inactiveSourceUrls(@Param("category") String category);
    @Insert("INSERT INTO plugin_webfilter_sources(category,urls,inactiveUrls) VALUES(#{category},#{urls},#{inactiveUrls}) " +
            "ON CONFLICT(category) DO UPDATE SET urls = EXCLUDED.urls, inactiveUrls = EXCLUDED.inactiveUrls")
    void saveSourceUrls(@Param("category") String category, @Param("urls") String urls,
                        @Param("inactiveUrls") String inactiveUrls);

    @Select("SELECT name, license, url FROM plugin_webfilter_attribution_sources ORDER BY position, name")
    List<AttributionView> findAttributionSources();

    @Delete("DELETE FROM plugin_webfilter_attribution_sources")
    void deleteAttributionSources();

    @Insert("INSERT INTO plugin_webfilter_attribution_sources(position, name, license, url) " +
            "VALUES(#{position}, #{source.name}, #{source.license}, #{source.url})")
    void insertAttributionSource(@Param("position") int position, @Param("source") AttributionView source);

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

    @Select("SELECT COALESCE(e.clientIp, d.publicIp) AS clientIp, e.*, d.number AS deviceNumber FROM plugin_webfilter_events e " +
            "LEFT JOIN devices d ON d.id = e.deviceId " +
            "LEFT JOIN plugin_webfilter_policies p ON p.customerId = e.customerId " +
            "AND p.configurationId = e.configurationId " +
            // Consulta DNS sem aparelho identificado nao tem perfil (configurationId nulo) e precisa aparecer.
            "WHERE e.customerId = #{customerId} AND (e.configurationId IS NULL OR p.enabled = TRUE) " +
            "ORDER BY e.createdAt DESC LIMIT #{limit}")
    List<WebFilterEvent> findRecentEvents(@Param("customerId") int customerId, @Param("limit") int limit);

    // Rastreabilidade: criterios combinados (E), busca parcial, sem esconder eventos de perfil inativo
    // ou de aparelho nao identificado. IP casa com o do DNS ou com o informado pelo aparelho.
    // O IP calculado vem antes de e.*: com rotulo repetido, o MyBatis le a primeira coluna.
    @Select({"<script>",
            "SELECT COALESCE(e.clientIp, d.publicIp) AS clientIp, e.*, d.number AS deviceNumber",
            "FROM plugin_webfilter_events e LEFT JOIN devices d ON d.id = e.deviceId",
            "WHERE e.customerId = #{customerId}",
            "<if test='ip != null'> AND (e.clientIp ILIKE #{ip} OR d.publicIp ILIKE #{ip})</if>",
            "<if test='device != null'> AND (d.number ILIKE #{device} OR d.description ILIKE #{device})</if>",
            "<if test='site != null'> AND (e.host ILIKE #{site} OR e.url ILIKE #{site})</if>",
            "ORDER BY e.createdAt DESC LIMIT #{limit}",
            "</script>"})
    List<WebFilterEvent> searchEvents(@Param("customerId") int customerId, @Param("ip") String ip,
                                      @Param("device") String device, @Param("site") String site,
                                      @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM plugin_webfilter_events e " +
            "LEFT JOIN plugin_webfilter_policies p ON p.customerId = e.customerId " +
            "AND p.configurationId = e.configurationId " +
            "WHERE e.customerId = #{customerId} AND e.createdAt >= #{since} " +
            "AND (e.configurationId IS NULL OR p.enabled = TRUE)")
    int countEvents(@Param("customerId") int customerId, @Param("since") long since);

    // Historico paginado de "Trafego bloqueado recentemente": mesma selecao de findRecentEvents.
    @Select("SELECT COALESCE(e.clientIp, d.publicIp) AS clientIp, e.*, d.number AS deviceNumber FROM plugin_webfilter_events e " +
            "LEFT JOIN devices d ON d.id = e.deviceId " +
            "LEFT JOIN plugin_webfilter_policies p ON p.customerId = e.customerId " +
            "AND p.configurationId = e.configurationId " +
            "WHERE e.customerId = #{customerId} AND (e.configurationId IS NULL OR p.enabled = TRUE) " +
            "ORDER BY e.createdAt DESC, e.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<WebFilterEvent> findEventsPage(@Param("customerId") int customerId, @Param("offset") int offset,
                                        @Param("limit") int limit);

    // Busca paginada (IP, dispositivo, site combinados), mesmos criterios de searchEvents.
    @Select({"<script>",
            "SELECT COALESCE(e.clientIp, d.publicIp) AS clientIp, e.*, d.number AS deviceNumber",
            "FROM plugin_webfilter_events e LEFT JOIN devices d ON d.id = e.deviceId",
            "WHERE e.customerId = #{customerId}",
            "<if test='ip != null'> AND (e.clientIp ILIKE #{ip} OR d.publicIp ILIKE #{ip})</if>",
            "<if test='device != null'> AND (d.number ILIKE #{device} OR d.description ILIKE #{device})</if>",
            "<if test='site != null'> AND (e.host ILIKE #{site} OR e.url ILIKE #{site})</if>",
            "ORDER BY e.createdAt DESC, e.id DESC LIMIT #{limit} OFFSET #{offset}",
            "</script>"})
    List<WebFilterEvent> searchEventsPage(@Param("customerId") int customerId, @Param("ip") String ip,
                                          @Param("device") String device, @Param("site") String site,
                                          @Param("offset") int offset, @Param("limit") int limit);

    @Select({"<script>",
            "SELECT COUNT(*) FROM plugin_webfilter_events e LEFT JOIN devices d ON d.id = e.deviceId",
            "WHERE e.customerId = #{customerId}",
            "<if test='ip != null'> AND (e.clientIp ILIKE #{ip} OR d.publicIp ILIKE #{ip})</if>",
            "<if test='device != null'> AND (d.number ILIKE #{device} OR d.description ILIKE #{device})</if>",
            "<if test='site != null'> AND (e.host ILIKE #{site} OR e.url ILIKE #{site})</if>",
            "</script>"})
    int countSearchEvents(@Param("customerId") int customerId, @Param("ip") String ip,
                          @Param("device") String device, @Param("site") String site);

    @Select("SELECT COUNT(*) FROM plugin_webfilter_events e " +
            "LEFT JOIN plugin_webfilter_policies p ON p.customerId = e.customerId " +
            "AND p.configurationId = e.configurationId " +
            "WHERE e.customerId = #{customerId} AND (e.configurationId IS NULL OR p.enabled = TRUE)")
    int countAllEvents(@Param("customerId") int customerId);

    @Delete({"<script>",
            "DELETE FROM plugin_webfilter_events WHERE customerId = #{customerId} AND id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    int deleteEvents(@Param("customerId") int customerId, @Param("ids") List<Integer> ids);

    @Delete("DELETE FROM plugin_webfilter_events WHERE customerId = #{customerId}")
    int deleteAllEvents(@Param("customerId") int customerId);

    // Servidor ativo (modo DEV/PRD) gravado pelo hwmdm-admin; null quando a tabela ainda nao existe.
    @Select("SELECT CASE WHEN COALESCE((SELECT value FROM hwmdm_system_settings WHERE key = 'server.mode'), 'dev') = 'prd' " +
            "THEN (SELECT value FROM hwmdm_system_settings WHERE key = 'server.prd.url') " +
            "ELSE (SELECT value FROM hwmdm_system_settings WHERE key = 'server.dev.url') END")
    String activeServerUrl();

    @Delete("DELETE FROM plugin_webfilter_events WHERE createdAt < #{before}")
    int purgeEvents(@Param("before") long before);
}
