package com.hmdm.plugins.webfilter.persistence.mapper;

import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterEntry;
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

    @Select("SELECT * FROM plugin_webfilter_policies WHERE customerId = #{customerId}")
    List<WebFilterPolicy> findPoliciesByCustomer(@Param("customerId") int customerId);

    @Select("SELECT * FROM plugin_webfilter_policies WHERE enabled")
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
}
