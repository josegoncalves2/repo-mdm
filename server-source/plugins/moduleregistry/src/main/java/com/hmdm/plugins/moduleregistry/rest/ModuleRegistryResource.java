package com.hmdm.plugins.moduleregistry.rest;

import com.hmdm.persistence.domain.User;
import com.hmdm.plugins.moduleregistry.persistence.ModuleRegistryDAO;
import com.hmdm.plugins.moduleregistry.persistence.domain.ModuleState;
import com.hmdm.plugins.moduleregistry.rest.json.ToggleModuleRequest;
import com.hmdm.rest.json.Response;
import com.hmdm.security.SecurityContext;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>API de manutencao dos modulos NATIVOS do console (item 7 do relatorio de queixas:
 * "tudo deve ser tratado como modular"). Cada modulo pode ser desligado sem parar o servidor;
 * o estado fica por customer (vale para todos os operadores daquela conta, nao por navegador).</p>
 *
 * <p>A lista de identificadores validos e' fechada de proposito: o admin nao digita um id
 * livre, ele so liga/desliga o que o proprio console declara no registro do lado do cliente
 * (app/components/main/moduleRegistry.js). Os tres essenciais nunca podem ser desligados —
 * sem eles o proprio console de administracao fica inacessivel.</p>
 */
@Singleton
@Path("/plugins/moduleregistry/private")
@Api(tags = {"Module registry (manutencao de modulos)"})
@Produces(MediaType.APPLICATION_JSON)
public class ModuleRegistryResource {

    public static final String PERMISSION = "modules_manage";

    /** Mesmos ids usados em activeTab / openTab no tabs.controller.js. */
    private static final Set<String> KNOWN_MODULE_IDS = new HashSet<>(Arrays.asList(
            "SUMMARY", "DEVICES", "KIOSK", "plugin-webfilter", "GPSMAP", "REMOTE", "CHAT", "REPORTS",
            "CONFS", "APPS", "FILES", "ICONS",
            "USERS", "ROLES", "GROUPS", "COMMON", "DESIGN", "GENERAL", "GOVERNANCE"
    ));

    /** O minimo para o console continuar administravel (enunciado do pedido, item 7). */
    private static final Set<String> ESSENTIAL_MODULE_IDS = new HashSet<>(Arrays.asList(
            "DEVICES", "USERS", "GENERAL"
    ));

    private final ModuleRegistryDAO dao;

    public ModuleRegistryResource() {
        this.dao = null;
    }

    @Inject
    public ModuleRegistryResource(ModuleRegistryDAO dao) {
        this.dao = dao;
    }

    private static boolean denied() {
        return !SecurityContext.get().hasPermission(PERMISSION);
    }

    private static javax.ws.rs.core.Response http(int status, Response body) {
        return javax.ws.rs.core.Response.status(status).entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * <p>Estado de todos os modulos nativos conhecidos, para o customer do usuario logado.
     * Sem restricao de permissao alem de estar autenticado: e' o proprio menu lateral que
     * consulta isto em todo carregamento do painel, para qualquer papel.</p>
     */
    @GET
    @Path("/state")
    public javax.ws.rs.core.Response getState() {
        User user = SecurityContext.get().getCurrentUser().orElse(null);
        if (user == null) {
            return http(403, Response.PERMISSION_DENIED());
        }

        Map<String, ModuleState> byId = new HashMap<>();
        for (ModuleState s : this.dao.findByCustomer(user.getCustomerId())) {
            byId.put(s.getModuleId(), s);
        }

        // Todo id conhecido entra na resposta, mesmo sem linha no banco (== ligado, nunca
        // mexido). O cliente nao precisa saber a diferenca entre "nunca mexido" e "religado".
        List<ModuleState> result = new java.util.ArrayList<>();
        for (String id : KNOWN_MODULE_IDS) {
            ModuleState s = byId.get(id);
            if (s == null) {
                s = new ModuleState();
                s.setModuleId(id);
                s.setDisabled(false);
            }
            s.setEssential(ESSENTIAL_MODULE_IDS.contains(id));
            result.add(s);
        }
        return http(200, Response.OK(result));
    }

    /**
     * <p>Liga ou desliga um modulo. Exige a permissao {@link #PERMISSION}; recusa o
     * desligamento de um modulo essencial com um motivo visivel em vez de aceitar em silencio.</p>
     */
    @POST
    @Path("/toggle")
    @Consumes(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response toggle(ToggleModuleRequest request) {
        if (denied()) {
            return http(403, Response.PERMISSION_DENIED());
        }

        User user = SecurityContext.get().getCurrentUser().orElse(null);
        if (user == null) {
            return http(403, Response.PERMISSION_DENIED());
        }

        if (request == null || request.getModuleId() == null || !KNOWN_MODULE_IDS.contains(request.getModuleId())) {
            return http(400, Response.ERROR("plugin.moduleregistry.error.unknown"));
        }

        if (request.isDisabled() && ESSENTIAL_MODULE_IDS.contains(request.getModuleId())) {
            return http(409, Response.ERROR("plugin.moduleregistry.error.essential"));
        }

        String actor = user.getName() != null ? user.getName() : user.getLogin();
        this.dao.setDisabled(user.getCustomerId(), request.getModuleId(), request.isDisabled(), actor, request.getReason());

        return http(200, Response.OK());
    }
}
