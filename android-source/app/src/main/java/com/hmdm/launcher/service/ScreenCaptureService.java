/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmdm.launcher.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import com.hmdm.launcher.Const;
import com.hmdm.launcher.util.RemoteLogger;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;

/**
 * Captura a tela do aparelho a pedido do painel, sem dialogo de consentimento.
 *
 * Ate' aqui o comando "screenshot" era recebido e recusado: o agente registrava no log que
 * captura silenciosa exige assinatura de sistema e nao fazia mais nada, entao "Solicitar
 * screenshot" nunca produzia imagem e "Ver ultima captura de tela" respondia 404 para sempre.
 *
 * A partir do Android 11 um servico de acessibilidade com a capacidade canTakeScreenshot
 * consegue capturar sem consentimento por sessao, que e' o que uma sessao de auditoria exige.
 * Abaixo do Android 11 nao ha' caminho silencioso, e o comando continua sendo recusado - com
 * o motivo dito por extenso, em vez de silencio.
 *
 * O servico nao le' o conteudo das telas: ele so' fica vivo para atender a captura.
 */
public class ScreenCaptureService extends AccessibilityService {

    /**
     * Resultado de uma tentativa de captura.
     */
    public interface CaptureCallback {
        void onCaptured(byte[] pngBytes);

        void onFailed(String reason);
    }

    // O sistema mantem uma unica instancia do servico enquanto ele esta' ativo. Guardamos a
    // referencia porque quem recebe o push (PushNotificationProcessor) nao tem como obter o
    // servico de outro jeito.
    private static volatile ScreenCaptureService instance;

    /**
     * Qualidade suficiente para leitura de tela em suporte e auditoria, sem estourar o upload.
     */
    private static final int MAX_WIDTH = 1080;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Nao usamos eventos de acessibilidade.
    }

    @Override
    public void onInterrupt() {
    }

    /**
     * Diz se a captura esta' disponivel agora: precisa de Android 11 e do servico ativo.
     */
    public static boolean isAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && instance != null;
    }

    /**
     * Explica, em uma frase, por que a captura nao esta' disponivel. Usado para devolver ao
     * servidor um motivo concreto em vez de um erro generico.
     */
    public static String unavailableReason() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "Screenshot requires Android 11 or newer (this device runs API "
                    + Build.VERSION.SDK_INT + ")";
        }
        return "Screenshot service is not enabled in Accessibility settings on this device";
    }

    /**
     * Captura a tela e devolve os bytes de um PNG.
     */
    public static void capture(CaptureCallback callback) {
        ScreenCaptureService service = instance;
        if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback.onFailed(unavailableReason());
            return;
        }
        service.takeScreenshotInternal(callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void takeScreenshotInternal(CaptureCallback callback) {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, Executors.newSingleThreadExecutor(),
                    new TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(ScreenshotResult screenshot) {
                            HardwareBuffer buffer = screenshot.getHardwareBuffer();
                            try {
                                ColorSpace colorSpace = screenshot.getColorSpace();
                                Bitmap hardwareBitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace);
                                if (hardwareBitmap == null) {
                                    callback.onFailed("Screen capture returned an empty frame");
                                    return;
                                }
                                // O bitmap devolvido e' do tipo HARDWARE e nao pode ser
                                // comprimido direto: precisa de uma copia em memoria comum.
                                Bitmap bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false);
                                hardwareBitmap.recycle();
                                if (bitmap == null) {
                                    callback.onFailed("Screen capture could not be copied from the GPU buffer");
                                    return;
                                }
                                callback.onCaptured(toPng(bitmap));
                                bitmap.recycle();
                            } catch (Exception e) {
                                callback.onFailed("Screen capture failed: " + e.getMessage());
                            } finally {
                                buffer.close();
                            }
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            callback.onFailed("Screen capture refused by the system, error " + errorCode);
                        }
                    });
        } catch (Exception e) {
            RemoteLogger.log(this, Const.LOG_WARN, "Screen capture failed: " + e.getMessage());
            callback.onFailed("Screen capture failed: " + e.getMessage());
        }
    }

    /**
     * Reduz a imagem para uma largura util e devolve o PNG.
     */
    private static byte[] toPng(Bitmap bitmap) {
        Bitmap scaled = bitmap;
        if (bitmap.getWidth() > MAX_WIDTH) {
            int height = Math.round(bitmap.getHeight() * (MAX_WIDTH / (float) bitmap.getWidth()));
            scaled = Bitmap.createScaledBitmap(bitmap, MAX_WIDTH, height, true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
        if (scaled != bitmap) {
            scaled.recycle();
        }
        return out.toByteArray();
    }
}
