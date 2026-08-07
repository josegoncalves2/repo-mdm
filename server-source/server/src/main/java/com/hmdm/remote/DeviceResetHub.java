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

package com.hmdm.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Os pedidos de restauracao de fabrica que ainda nao chegaram ao aparelho.</p>
 *
 * <p>Por que existe: o launcher 6.36 nao tem tipo de push para apagar o aparelho -- a
 * constante {@code wipe} nao existe no binario, entao esse push era recebido e descartado.
 * O caminho que ele <b>de fato</b> implementa e' outro: ao atualizar a configuracao ele
 * chama {@code checkFactoryReset()} e, se o campo {@code factoryReset} vier verdadeiro na
 * resposta de sincronizacao, confirma com o servidor e so' entao apaga. Marcar o aparelho
 * aqui e mandar um {@code configUpdated} e' o que traduz o botao do painel para esse
 * caminho.</p>
 *
 * <p><b>Em memoria, e com prazo, de proposito.</b> Guardar "apagar este aparelho" numa
 * coluna do banco significa que um pedido esquecido continua armado indefinidamente: o
 * tablet fica sem rede por tres semanas, volta, sincroniza e e' apagado por um clique que
 * ninguem lembra de ter dado. Um pedido que expira em {@link #TTL_MS} erra para o lado de
 * nao apagar -- e um pedido perdido custa um segundo clique, enquanto um pedido indevido
 * custa o aparelho.</p>
 */
public class DeviceResetHub {

    private static final Logger logger = LoggerFactory.getLogger(DeviceResetHub.class);

    /** Janela para o aparelho aparecer. Um ciclo de sincronizacao leva ~1 minuto. */
    static final long TTL_MS = 10 * 60 * 1000L;

    private static final DeviceResetHub INSTANCE = new DeviceResetHub();

    /** numero do aparelho -> instante em que o pedido deixa de valer. */
    private final Map<String, Long> pending = new ConcurrentHashMap<>();

    private DeviceResetHub() {
    }

    public static DeviceResetHub getInstance() {
        return INSTANCE;
    }

    public void request(String deviceNumber) {
        if (deviceNumber == null) {
            return;
        }
        pending.put(deviceNumber, System.currentTimeMillis() + TTL_MS);
        logger.warn("Restauracao de fabrica armada para '{}'; expira em {} minutos",
                deviceNumber, TTL_MS / 60000L);
    }

    /**
     * <p>Responde se o aparelho deve receber {@code factoryReset=true} na sincronizacao.</p>
     *
     * <p>Nao consome o pedido: quem consome e' a confirmacao do proprio aparelho, em
     * {@link #confirm(String)}. Se consumisse aqui, uma resposta de sincronizacao perdida
     * na rede desarmaria o pedido sem que nada tivesse acontecido no aparelho.</p>
     */
    public boolean isRequested(String deviceNumber) {
        if (deviceNumber == null) {
            return false;
        }
        Long expiry = pending.get(deviceNumber);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            pending.remove(deviceNumber);
            logger.info("Pedido de restauracao de fabrica de '{}' expirou sem ser entregue", deviceNumber);
            return false;
        }
        return true;
    }

    /**
     * O aparelho confirmou que recebeu a ordem e vai executa-la; o pedido sai da fila para
     * nao ser reenviado no proximo ciclo.
     *
     * @return true se havia mesmo um pedido armado para este aparelho.
     */
    public boolean confirm(String deviceNumber) {
        if (deviceNumber == null) {
            return false;
        }
        boolean had = pending.remove(deviceNumber) != null;
        if (had) {
            logger.warn("Aparelho '{}' confirmou a restauracao de fabrica", deviceNumber);
        }
        return had;
    }

    public void cancel(String deviceNumber) {
        if (deviceNumber != null && pending.remove(deviceNumber) != null) {
            logger.info("Pedido de restauracao de fabrica de '{}' cancelado", deviceNumber);
        }
    }
}
