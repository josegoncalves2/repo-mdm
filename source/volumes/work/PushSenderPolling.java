/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.inject.Inject
 *  com.google.inject.Singleton
 *  javax.servlet.AsyncContext
 */
package com.hmdm.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hmdm.notification.PushSender;
import com.hmdm.notification.persistence.NotificationDAO;
import com.hmdm.notification.persistence.domain.PushMessage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.AsyncContext;

@Singleton
public class PushSenderPolling
implements PushSender {
    private final NotificationDAO notificationDAO;
    private final Map<Integer, DeviceEntry> deviceIdMap = new java.util.concurrent.ConcurrentHashMap<Integer, DeviceEntry>();
    private final Map<AsyncContext, DeviceEntry> deviceContextMap = new java.util.concurrent.ConcurrentHashMap<AsyncContext, DeviceEntry>();

    @Inject
    public PushSenderPolling(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    @Override
    public void init() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int send(PushMessage message) {
        DeviceEntry deviceEntry = this.deviceIdMap.get(message.getDeviceId());
        if (deviceEntry == null) {
            return this.notificationDAO.send(message);
        }
        List<PushMessage> pendingMessages = this.notificationDAO.getPendingMessagesForDelivery(message.getDeviceId());
        List<PushMessage> list = deviceEntry.messages;
        synchronized (list) {
            for (PushMessage pending : pendingMessages) {
                deviceEntry.messages.add(pending);
            }
            deviceEntry.messages.add(message);
        }
        try {
            deviceEntry.context.complete();
        }
        catch (Exception e) {
            // Consulta ja' encerrada: devolve ao banco para a proxima consulta levar.
            this.unregister(deviceEntry.context);
            List<PushMessage> list2 = deviceEntry.messages;
            synchronized (list2) {
                for (PushMessage m : deviceEntry.messages) {
                    this.notificationDAO.send(m);
                }
                deviceEntry.messages.clear();
            }
        }
        return 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendPending(int deviceId, List<PushMessage> messages) {
        DeviceEntry deviceEntry = this.deviceIdMap.get(deviceId);
        if (deviceEntry == null) {
            return;
        }
        List<PushMessage> list = deviceEntry.messages;
        synchronized (list) {
            for (PushMessage pending : messages) {
                deviceEntry.messages.add(pending);
            }
        }
        deviceEntry.context.complete();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<PushMessage> getPendingMessages(AsyncContext asyncContext) {
        DeviceEntry deviceEntry = this.deviceContextMap.get(asyncContext);
        LinkedList<PushMessage> result = new LinkedList<PushMessage>();
        if (deviceEntry != null) {
            List<PushMessage> list = deviceEntry.messages;
            synchronized (list) {
                result.addAll(deviceEntry.messages);
                deviceEntry.messages.clear();
            }
        }
        return result;
    }

    public void register(Integer deviceId, AsyncContext asyncContext) {
        DeviceEntry deviceEntry = new DeviceEntry(deviceId, asyncContext);
        this.deviceIdMap.put(deviceId, deviceEntry);
        this.deviceContextMap.put(asyncContext, deviceEntry);
    }

    public void unregister(AsyncContext asyncContext) {
        DeviceEntry deviceEntry = this.deviceContextMap.get(asyncContext);
        if (deviceEntry != null) {
            // So' remove se ainda for a consulta atual: o aparelho reabre o long poll antes de
            // a consulta anterior terminar, e apagar pelo deviceId tirava a consulta NOVA do
            // mapa -- dai' todo push ficava no banco ate' o poll expirar (minutos).
            this.deviceIdMap.remove(deviceEntry.deviceId, deviceEntry);
            this.deviceContextMap.remove(asyncContext);
        }
    }

    public class DeviceEntry {
        public Integer deviceId;
        public AsyncContext context;
        public final List<PushMessage> messages = new LinkedList<PushMessage>();

        public DeviceEntry(Integer deviceId, AsyncContext context) {
            this.deviceId = deviceId;
            this.context = context;
        }
    }
}
