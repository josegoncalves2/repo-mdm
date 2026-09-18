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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(WebFilterResource.class);
    public static final String PERMISSION = "plugin_webfilter_access";

    private WebFilterService service;
    private WebFilterCatalog catalog;

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

    @GET
    @Path("/catalog")
    public Response getCatalog() {
        if (denied()) {
            return Response.PERMISSION_DENIED();
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
            ArrayNode a = c.putArray("apps");
            apps.getOrDefault(id, Set.of()).forEach(a::add);
        }
        ArrayNode prot = root.putArray("protectedPackages");
        catalog.getProtectedPackages().forEach(prot::add);
        root.set("attribution", catalog.getAttribution());
        return Response.OK(root);
    }

    @GET
    @Path("/policies")
    public Response listPolicies() {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        return Response.OK(service.listPolicies());
    }

    @GET
    @Path("/policies/{configurationId}")
    public Response getPolicy(@PathParam("configurationId") int configurationId) {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        PolicyView view = service.getPolicy(configurationId);
        return view == null ? Response.OBJECT_NOT_FOUND_ERROR() : Response.OK(view);
    }

    @PUT
    @Path("/policies/{configurationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePolicy(@PathParam("configurationId") int configurationId, PolicyView policy) {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        try {
            List<ValidationError> errors = service.savePolicy(configurationId, policy);
            if (!errors.isEmpty()) {
                return Response.ERROR("plugin.webfilter.error.validation", errors);
            }
            return Response.OK(service.getPolicy(configurationId));
        } catch (SecurityException e) {
            log.warn("Web filter policy save denied: {}", e.getMessage());
            return Response.PERMISSION_DENIED();
        }
    }

    @GET
    @Path("/apps")
    public Response getAppCategories() {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        return Response.OK(service.getAppCategories());
    }

    @PUT
    @Path("/apps")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAppCategory(WebFilterAppCategory item) {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        List<ValidationError> errors = service.addAppCategory(item.getPackageName(), item.getCategory());
        return errors.isEmpty() ? Response.OK(service.getAppCategories())
                : Response.ERROR("plugin.webfilter.error.validation", errors);
    }

    @DELETE
    @Path("/apps/{id}")
    public Response removeAppCategory(@PathParam("id") int id) {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        return service.removeAppCategory(id) ? Response.OK(service.getAppCategories())
                : Response.OBJECT_NOT_FOUND_ERROR();
    }

    @GET
    @Path("/settings")
    public Response getSettings() {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        SettingsView v = new SettingsView();
        v.setDnsDomain(service.getDnsDomain());
        return Response.OK(v);
    }

    @PUT
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveSettings(SettingsView settings) {
        if (denied()) {
            return Response.PERMISSION_DENIED();
        }
        List<ValidationError> errors = service.saveDnsDomain(settings.getDnsDomain());
        return errors.isEmpty() ? getSettings() : Response.ERROR("plugin.webfilter.error.validation", errors);
    }
}
