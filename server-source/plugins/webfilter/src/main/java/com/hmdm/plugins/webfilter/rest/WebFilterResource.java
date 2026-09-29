package com.hmdm.plugins.webfilter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterSettings;
import com.hmdm.plugins.webfilter.rest.json.PolicyView;
import com.hmdm.plugins.webfilter.rest.json.AttributionView;
import com.hmdm.plugins.webfilter.rest.json.SettingsView;
import com.hmdm.plugins.webfilter.rest.json.SourceView;
import com.hmdm.plugins.webfilter.rest.json.ValidationError;
import com.hmdm.plugins.webfilter.service.WebFilterService;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Console API of the web filter. All operations require the <code>plugin_webfilter_access</code> permission and
 * act on the customer of the authenticated user only.</p>
 */
@Singleton
@Path("/plugins/webfilter/private")
@Api(tags = {"Web Filter plugin"})
@Produces(MediaType.APPLICATION_JSON)
public class WebFilterResource {

    public static final String PERMISSION = "plugin_webfilter_access";

    private WebFilterService service;
    private WebFilterCatalog catalog;
    @Inject private com.hmdm.plugins.webfilter.resolver.ResolverConfigWriter sourceWriter;

    /** A constructor required by Swagger. */
    public WebFilterResource() {
    }

    @Inject
    public WebFilterResource(WebFilterService service, WebFilterCatalog catalog) {
        this.service = service;
        this.catalog = catalog;
    }

    private static boolean denied() {
        return !SecurityContext.get().hasPermission(PERMISSION);
    }

    /** The console's JSON envelope, with the HTTP status the spec requires (403, 400, 404). */
    private static javax.ws.rs.core.Response http(int status, Response body) {
        return javax.ws.rs.core.Response.status(status).entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    private static javax.ws.rs.core.Response ok(Response body) {
        return http(200, body);
    }

    private static javax.ws.rs.core.Response forbidden() {
        return http(403, Response.PERMISSION_DENIED());
    }

    private static javax.ws.rs.core.Response notFound() {
        return http(404, Response.OBJECT_NOT_FOUND_ERROR());
    }

    private static javax.ws.rs.core.Response invalid(List<ValidationError> errors) {
        return http(400, Response.ERROR("plugin.webfilter.error.validation", errors));
    }

    @GET
    @Path("/catalog")
    public javax.ws.rs.core.Response getCatalog() {
        if (denied()) {
            return forbidden();
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode categories = root.putArray("categories");
        Map<String, Set<String>> apps = service.appsByCategory(WebFilterService.currentCustomerId());
        for (String id : catalog.getCategoryIds()) {
            ObjectNode c = categories.addObject();
            c.put("id", id);
            c.put("required", WebFilterCatalog.REQUIRED_CATEGORY.equals(id));
            putSources(c, id);
            ArrayNode a = c.putArray("apps");
            apps.getOrDefault(id, java.util.Collections.<String>emptySet()).forEach(a::add);
        }
        ArrayNode prot = root.putArray("protectedPackages");
        catalog.getProtectedPackages().forEach(prot::add);
        root.set("attribution", mapper.valueToTree(catalog.getAttributionEntries()));
        // Same check as saving the settings (DNS domain): whoever reaches this point may manage the lists
        root.put("canManageSources", !denied());
        return ok(Response.OK(root));
    }

    /** Every list of the category (active or not) and how many of them go to the resolver. */
    private void putSources(ObjectNode node, String category) {
        List<SourceView> entries = catalog.getSourceEntries(category);
        node.put("sourceCount", (int) entries.stream().filter(SourceView::isActive).count());
        ArrayNode sources = node.putArray("sources");
        for (SourceView s : entries) {
            ObjectNode item = sources.addObject();
            item.put("url", s.getUrl());
            item.put("active", s.isActive());
        }
    }

    @GET
    @Path("/dashboard")
    public javax.ws.rs.core.Response dashboard() {
        if (denied()) {
            return forbidden();
        }
        return ok(Response.OK(service.dashboard()));
    }

    @GET
    @Path("/events")
    public javax.ws.rs.core.Response searchEvents(@javax.ws.rs.QueryParam("ip") String ip,
                                                 @javax.ws.rs.QueryParam("device") String device,
                                                 @javax.ws.rs.QueryParam("site") String site) {
        if (denied()) {
            return forbidden();
        }
        return ok(Response.OK(service.searchEvents(ip, device, site)));
    }

    @GET
    @Path("/policies")
    public javax.ws.rs.core.Response listPolicies() {
        if (denied()) {
            return forbidden();
        }
        return ok(Response.OK(service.listPolicies()));
    }

    @GET
    @Path("/policies/{configurationId}")
    public javax.ws.rs.core.Response getPolicy(@PathParam("configurationId") int configurationId) {
        if (denied()) {
            return forbidden();
        }
        PolicyView view = service.getPolicy(configurationId);
        return view == null ? notFound() : ok(Response.OK(view));
    }

    @PUT
    @Path("/policies/{configurationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response savePolicy(@PathParam("configurationId") int configurationId, PolicyView policy) {
        if (denied()) {
            return forbidden();
        }
        List<ValidationError> errors = service.savePolicy(configurationId, policy);
        if (errors == null) {
            return notFound();
        }
        return errors.isEmpty() ? ok(Response.OK(service.getPolicy(configurationId))) : invalid(errors);
    }

    @GET
    @Path("/apps")
    public javax.ws.rs.core.Response getAppCategories() {
        if (denied()) {
            return forbidden();
        }
        return ok(Response.OK(service.getAppCategories()));
    }

    @PUT
    @Path("/apps")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response addAppCategory(WebFilterAppCategory item) {
        if (denied()) {
            return forbidden();
        }
        List<ValidationError> errors = service.addAppCategory(item.getPackageName(), item.getCategory());
        return errors.isEmpty() ? ok(Response.OK(service.getAppCategories())) : invalid(errors);
    }

    @DELETE
    @Path("/apps/{id}")
    public javax.ws.rs.core.Response removeAppCategory(@PathParam("id") int id) {
        if (denied()) {
            return forbidden();
        }
        return service.removeAppCategory(id) ? ok(Response.OK(service.getAppCategories())) : notFound();
    }

    @GET
    @Path("/settings")
    public javax.ws.rs.core.Response getSettings() {
        if (denied()) {
            return forbidden();
        }
        SettingsView v = toSettingsView(service.getSettings());
        return ok(Response.OK(v));
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response saveSettings(SettingsView settings) {
        if (denied()) {
            return forbidden();
        }
        if (settings == null) {
            settings = new SettingsView();
        }
        WebFilterSettings s = new WebFilterSettings();
        s.setDnsDomain(settings.getDnsDomain());
        s.setBlockPageTitle(settings.getBlockPageTitle());
        s.setBlockPageMessage(settings.getBlockPageMessage());
        s.setBlockPageLogoUrl(settings.getBlockPageLogoUrl());
        s.setBlockPageSupportText(settings.getBlockPageSupportText());
        s.setBlockPageCustomHtml(settings.getBlockPageCustomHtml());
        s.setBlockPageCustomCss(settings.getBlockPageCustomCss());
        List<ValidationError> errors = service.saveSettings(s);
        return errors.isEmpty() ? getSettings() : invalid(errors);
    }

    private SettingsView toSettingsView(WebFilterSettings s) {
        SettingsView v = new SettingsView();
        v.setDnsDomain(s.getDnsDomain());
        v.setBlockPageTitle(s.getBlockPageTitle());
        v.setBlockPageMessage(s.getBlockPageMessage());
        v.setBlockPageLogoUrl(s.getBlockPageLogoUrl());
        v.setBlockPageSupportText(s.getBlockPageSupportText());
        v.setBlockPageCustomHtml(s.getBlockPageCustomHtml());
        v.setBlockPageCustomCss(s.getBlockPageCustomCss());
        return v;
    }
    @PUT
    @Path("/sources/{category}")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response saveSources(@PathParam("category") String category, java.util.List<SourceView> sources) {
        // Same check as saving the settings (DNS domain)
        if (denied()) { return forbidden(); }
        try {
            catalog.saveSources(category, sources);
            // Rewrites sources.json/blocky.yml; the resolver downloads the changed lists and reloads Blocky
            sourceWriter.writeAll();
            ObjectNode saved = new ObjectMapper().createObjectNode();
            saved.put("id", category);
            putSources(saved, category);
            return ok(Response.OK(saved));
        } catch (IllegalArgumentException e) { return http(400, Response.ERROR(e.getMessage())); }
    }

    @PUT
    @Path("/attribution")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response saveAttribution(java.util.List<AttributionView> sources) {
        if (denied()) { return forbidden(); }
        try {
            catalog.saveAttributionEntries(sources);
            return ok(Response.OK(catalog.getAttributionEntries()));
        } catch (IllegalArgumentException e) {
            return http(400, Response.ERROR(e.getMessage()));
        }
    }

}
