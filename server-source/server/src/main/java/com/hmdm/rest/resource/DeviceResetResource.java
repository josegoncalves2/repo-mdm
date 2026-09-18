/*
 *
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hmdm.rest.resource;

import com.hmdm.persistence.UnsecureDAO;
import com.hmdm.persistence.domain.Device;
import com.hmdm.remote.DeviceResetHub;
import com.hmdm.rest.json.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * <p>A confirmacao que o aparelho pede antes de se apagar.</p>
 *
 * <p>O launcher 6.36 nao apaga o aparelho so' porque a configuracao mandou. Ele primeiro
 * chama este endereco ({@code ConfirmDeviceResetTask}); se a chamada falhar, registra
 * "Failed to confirm device reset on server" e <b>nao</b> apaga nada. No Headwind original
 * quem responde aqui e' o plugin devicereset, que e' pago e nao existe nesta instalacao --
 * e' por isso que "Redefinir fabrica" nunca surtiu efeito, mesmo com o resto do caminho
 * montado.</p>
 *
 * <p>O caminho e' publico porque quem chama e' o aparelho, que nao tem sessao de painel --
 * exatamente como {@code /rest/public/sync}. Isso obriga a checagem que este recurso faz:
 * so' confirma para um aparelho que existe <b>e</b> que tem um pedido de restauracao
 * realmente armado. Sem essa segunda condicao, este endereco seria um jeito de qualquer um
 * na rede confirmar um apagamento que ninguem pediu.</p>
 */
@Api(tags = {"Device reset"})
@Singleton
@Path("/plugins/devicereset/public")
public class DeviceResetResource {

    private static final Logger log = LoggerFactory.getLogger(DeviceResetResource.class);

    private UnsecureDAO unsecureDAO;

    /**
     * <p>A constructor required by Swagger.</p>
     */
    public DeviceResetResource() {
    }

    @Inject
    public DeviceResetResource(UnsecureDAO unsecureDAO) {
        this.unsecureDAO = unsecureDAO;
    }

    @ApiOperation(
            value = "Confirm a factory reset",
            notes = "Called by the device agent right before erasing itself. Answering OK is what " +
                    "allows the wipe to proceed; the request is refused unless a reset was actually " +
                    "requested for this device from the control panel."
    )
    @POST
    @Path("/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmReset(@PathParam("number") @ApiParam("Device number") String number) {
        try {
            final Device device = this.unsecureDAO.getDeviceByNumber(number);
            if (device == null) {
                log.warn("Confirmacao de restauracao recusada: nenhum aparelho com numero '{}'", number);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }
            if (!DeviceResetHub.getInstance().confirm(number)) {
                log.warn("Confirmacao de restauracao recusada para '{}': nenhum pedido armado", number);
                return Response.ERROR("error.device.reset.not.requested");
            }
            log.warn("Restauracao de fabrica confirmada para '{}'; o aparelho vai se apagar", number);
            return Response.OK();
        } catch (Exception e) {
            log.error("Falha ao confirmar a restauracao de fabrica de '{}'", number, e);
            return Response.INTERNAL_ERROR();
        }
    }
}
