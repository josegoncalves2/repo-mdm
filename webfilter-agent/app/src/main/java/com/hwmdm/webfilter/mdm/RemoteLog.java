package com.hwmdm.webfilter.mdm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.hmdm.IMdmApi;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RemoteLog {

    private static final String TAG = "hwmdm-webfilter";
    private static final String LAUNCHER_PACKAGE = "com.hmdm.launcher";
    private static final String CONNECT_ACTION = "com.hmdm.action.Connect";

    public static final int INFO = 3;
    public static final int WARN = 2;
    public static final int ERROR = 1;

    private static final int MAX_QUEUE = 100;
    private static final Deque<String[]> queue = new ArrayDeque<>();
    private static volatile IMdmApi api;
    private static volatile boolean binding;

    private RemoteLog() { }

    public static void i(Context context, String message) { write(context, INFO, message); }
    public static void w(Context context, String message) { write(context, WARN, message); }
    public static void e(Context context, String message) { write(context, ERROR, message); }

    private static void write(Context context, int level, String message) {
        Log.i(TAG, message);
        IMdmApi current = api;
        if (current != null && send(current, level, message)) return;
        if (current != null) api = null;
        synchronized (queue) {
            if (queue.size() >= MAX_QUEUE) queue.pollFirst();
            queue.addLast(new String[]{String.valueOf(level), message});
        }
        bind(context.getApplicationContext());
    }

    private static boolean send(IMdmApi target, int level, String message) {
        try {
            target.log(System.currentTimeMillis(), level, "com.hwmdm.webfilter", message);
            return true;
        } catch (Throwable t) { return false; }
    }

    private static void bind(Context context) {
        if (binding || api != null) return;
        binding = true;
        Intent intent = new Intent(CONNECT_ACTION).setPackage(LAUNCHER_PACKAGE);
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                api = IMdmApi.Stub.asInterface(binder);
                binding = false;
                flush();
            }
            @Override public void onServiceDisconnected(ComponentName name) { api = null; binding = false; }
            @Override public void onNullBinding(ComponentName name) { binding = false; }
        };
        try {
            if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) binding = false;
        } catch (Throwable t) { binding = false; }
    }

    private static void flush() {
        IMdmApi current = api;
        if (current == null) return;
        synchronized (queue) {
            while (!queue.isEmpty()) {
                String[] entry = queue.peekFirst();
                if (!send(current, Integer.parseInt(entry[0]), entry[1])) return;
                queue.pollFirst();
            }
        }
    }
}
