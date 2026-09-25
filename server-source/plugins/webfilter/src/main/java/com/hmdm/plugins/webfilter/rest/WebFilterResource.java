package com.hmdm.plugins.webfilter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hmdm.plugins.webfilter.catalog.WebFilterCatalog;
import com.hmdm.plugins.webfilter.persistence.domain.WebFilterAppCategory;
import com.hmdm.plugins.webfilter.rest.json.PolicyView;
import com.hmdm.plugins.webfilter.rest.json.SettingsView;
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
            c.put("sourceCount", catalog.getSiteSources(id).size());
            ArrayNode sources = c.putArray("sources");
            catalog.getSiteSources(id).forEach(sources::add);
            ArrayNode a = c.putArray("apps");
            apps.getOrDefault(id, java.util.Collections.<String>emptySet()).forEach(a::add);
        }
        ArrayNode prot = root.putArray("protectedPackages");
        catalog.getProtectedPackages().forEach(prot::add);
        root.set("attribution", catalog.getAttribution());
        root.put("canManageSources", SecurityContext.get().isSuperAdmin());
        return ok(Response.OK(root));
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
        SettingsView v = new SettingsView();
        v.setDnsDomain(service.getDnsDomain());
        return ok(Response.OK(v));
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response saveSettings(SettingsView settings) {
        if (denied()) {
            return forbidden();
        }
        List<ValidationError> errors = service.saveDnsDomain(settings.getDnsDomain());
        return errors.isEmpty() ? getSettings() : invalid(errors);
    }
    @PUT
    @Path("/sources/{category}")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response saveSources(@PathParam("category") String category, java.util.List<String> urls) {
        if (denied() || !SecurityContext.get().isSuperAdmin()) { return forbidden(); }
        try {
            catalog.saveSources(category, urls);
            sourceWriter.writeAll();
            return ok(Response.OK());
        } catch (IllegalArgumentException e) { return http(400, Response.ERROR(e.getMessage())); }
    }

}
