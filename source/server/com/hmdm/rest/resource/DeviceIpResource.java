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
import com.hmdm.rest.json.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>O endereco de rede que o proprio aparelho enxerga.</p>
 *
 * <p>Por que nao basta olhar de onde a conexao veio: nesta instalacao os tablets saem por
 * um proxy HTTP e, atras dele, por mais um NAT. O endereco observado pelo servidor e' o do
 * ultimo salto, igual para todos os aparelhos -- e' util para diagnostico de rede e
 * inutil para identificar um tablet. Nenhum cabecalho resolve isso: o segundo NAT nao
 * escreve nenhum.</p>
 *
 * <p>Quem sabe em que endereco o aparelho esta' e' o aparelho. O agente
 * {@code com.hwmdm.remote} le o endereco local do socket que usa para falar com este
 * servidor -- ou seja, exatamente a interface pela qual ele esta' conectado, e nao a
 * primeira da lista -- e o informa aqui.</p>
 *
 * <p>O caminho e' publico porque quem chama e' o aparelho, que nao tem sessao de painel,
 * como {@code /rest/public/sync}. O que o torna seguro nao e' autenticacao e sim o escopo:
 * so' se aceita endereco das redes de DEVICE_IP_NETWORKS, e apenas para um aparelho que ja' existe. O pior que
 * um abuso consegue e' escrever um endereco de rede local errado no cadastro de um
 * aparelho conhecido.</p>
 */
@Api(tags = {"Device IP"})
@Singleton
@Path("/plugins/deviceip/public")
public class DeviceIpResource {

    private static final Logger log = LoggerFactory.getLogger(DeviceIpResource.class);

    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

    /**
     * Redes onde um aparelho pode estar (CIDR, separadas por virgula), vindas de
     * DEVICE_IP_NETWORKS. A rede dos tablets nao e' necessariamente RFC 1918 (ex.: 172.14.x),
     * por isso a lista e' configuracao da instalacao e nao regra fixa.
     */
    private static final List<long[]> DEVICE_NETWORKS = parseNetworks(System.getenv("DEVICE_IP_NETWORKS"));

    /** Proxy/NAT nunca e' o endereco de um aparelho (mesma variavel do BaseIPFilter). */
    private static final Set<String> INFRASTRUCTURE_IPS = new HashSet<>(Arrays.asList(
            Optional.ofNullable(System.getenv("INFRASTRUCTURE_IPS")).orElse("").trim().split("\\s*,\\s*")));

    private static long toLong(String ip) {
        long v = 0;
        for (String part : ip.split("\\.")) {
            v = (v << 8) | Integer.parseInt(part);
        }
        return v;
    }

    private static List<long[]> parseNetworks(String value) {
        List<long[]> nets = new ArrayList<>();
        for (String cidr : Optional.ofNullable(value).orElse("").split("\\s*,\\s*")) {
            String[] p = cidr.trim().split("/");
            if (p.length != 2 || !IPV4.matcher(p[0]).matches()) {
                continue;
            }
            int bits = Integer.parseInt(p[1]);
            long mask = bits == 0 ? 0 : (0xFFFFFFFFL << (32 - bits)) & 0xFFFFFFFFL;
            nets.add(new long[]{toLong(p[0]) & mask, mask});
        }
        return nets;
    }

    static boolean isAcceptedDeviceIp(String ip) {
        if (ip == null || !IPV4.matcher(ip).matches() || INFRASTRUCTURE_IPS.contains(ip)) {
            return false;
        }
        long addr = toLong(ip);
        for (long[] net : DEVICE_NETWORKS) {
            if ((addr & net[1]) == net[0]) {
                return true;
            }
        }
        return false;
    }

    private UnsecureDAO unsecureDAO;

    /**
     * <p>A constructor required by Swagger.</p>
     */
    public DeviceIpResource() {
    }

    @Inject
    public DeviceIpResource(UnsecureDAO unsecureDAO) {
        this.unsecureDAO = unsecureDAO;
    }

    @ApiOperation(
            value = "Report the address the device is actually reachable at",
            notes = "Called by the device agent. Replaces the address observed by the server, which " +
                    "behind a proxy or NAT is the last hop and is identical for every device."
    )
    @PUT
    @Path("/{number}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reportIp(@PathParam("number") @ApiParam("Device number") String number,
                             Map<String, Object> body) {
        try {
            final Object raw = body == null ? null : body.get("ip");
            final String ip = raw == null ? null : String.valueOf(raw).trim();
            if (!isAcceptedDeviceIp(ip)) {
                log.warn("Endereco recusado para '{}': '{}' fora de DEVICE_IP_NETWORKS ou e' de infraestrutura", number, ip);
                return Response.ERROR("error.device.ip.invalid");
            }

            final Device device = this.unsecureDAO.getDeviceByNumber(number);
            if (device == null) {
                log.warn("Endereco recusado: nenhum aparelho com numero '{}'", number);
                return Response.DEVICE_NOT_FOUND_ERROR();
            }

            if (!ip.equals(device.getPublicIp())) {
                log.info("Aparelho '{}' passou a ser alcancavel em {}", number, ip);
            }
            this.unsecureDAO.updateDeviceIp(device.getId(), ip);
            return Response.OK();
        } catch (Exception e) {
            log.error("Falha ao registrar o endereco do aparelho '{}'", number, e);
            return Response.INTERNAL_ERROR();
        }
    }
}
