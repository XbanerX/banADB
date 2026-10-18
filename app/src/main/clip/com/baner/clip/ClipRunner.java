package com.baner.clip;

import android.content.ClipData;
import android.os.IBinder;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class ClipRunner {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            getClip();
            return;
        }

        String action = args[0];
        if ("get".equalsIgnoreCase(action)) {
            getClip();
        } else if ("setb64".equalsIgnoreCase(action)) {
            String b64 = args.length > 1 ? args[1] : "";
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(b64);
                String text = new String(decoded, StandardCharsets.UTF_8);
                setClip(text);
            } catch (Exception e) {
                setClip(b64);
            }
        } else if ("set".equalsIgnoreCase(action)) {
            String text = args.length > 1 ? args[1] : "";
            if (text.startsWith("b64:")) {
                try {
                    byte[] decoded = java.util.Base64.getDecoder().decode(text.substring(4));
                    text = new String(decoded, StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
            setClip(text);
        }
    }

    private static Object getClipboardService() throws Exception {
        Class<?> smClass = Class.forName("android.os.ServiceManager");
        Method getService = smClass.getMethod("getService", String.class);
        IBinder binder = (IBinder) getService.invoke(null, "clipboard");
        if (binder == null) return null;

        Class<?> stubClass = Class.forName("android.content.IClipboard$Stub");
        Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
        return asInterface.invoke(null, binder);
    }

    private static void getClip() {
        try {
            Object service = getClipboardService();
            if (service == null) return;

            Method targetMethod = null;
            for (Method m : service.getClass().getMethods()) {
                if (m.getName().equals("getPrimaryClip")) {
                    targetMethod = m;
                    Class<?>[] pts = m.getParameterTypes();
                    try {
                        ClipData clip = null;
                        if (pts.length == 4) { // Android 14, 15, 16 (callingPkg, attributionTag, userId, deviceId)
                            clip = (ClipData) m.invoke(service, "com.android.shell", null, 0, 0);
                        } else if (pts.length == 3) { // Android 12, 13 (callingPkg, attributionTag, userId)
                            clip = (ClipData) m.invoke(service, "com.android.shell", null, 0);
                        } else if (pts.length == 2) { // Android 10, 11 (callingPkg, userId)
                            clip = (ClipData) m.invoke(service, "com.android.shell", 0);
                        } else if (pts.length == 1) { // Android 9 or below (callingPkg)
                            clip = (ClipData) m.invoke(service, "com.android.shell");
                        }
                        if (clip != null && clip.getItemCount() > 0) {
                            CharSequence text = clip.getItemAt(0).getText();
                            if (text != null) {
                                System.out.print(text.toString());
                                return;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Throwable t) {
            // Silently fail
        }
    }

    private static void setClip(String text) {
        try {
            Object service = getClipboardService();
            if (service == null) return;

            ClipData clip = ClipData.newPlainText("text", text);

            for (Method m : service.getClass().getMethods()) {
                if (m.getName().equals("setPrimaryClip")) {
                    Class<?>[] pts = m.getParameterTypes();
                    try {
                        if (pts.length == 5) { // Android 14, 15, 16 (clip, callingPkg, attributionTag, userId, deviceId)
                            m.invoke(service, clip, "com.android.shell", null, 0, 0);
                            System.out.println("OK");
                            return;
                        } else if (pts.length == 4) { // Android 12, 13 (clip, callingPkg, attributionTag, userId)
                            m.invoke(service, clip, "com.android.shell", null, 0);
                            System.out.println("OK");
                            return;
                        } else if (pts.length == 3) { // Android 10, 11 (clip, callingPkg, userId)
                            m.invoke(service, clip, "com.android.shell", 0);
                            System.out.println("OK");
                            return;
                        } else if (pts.length == 2) { // Android 9 or below (clip, callingPkg)
                            m.invoke(service, clip, "com.android.shell");
                            System.out.println("OK");
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Throwable t) {
            // Silently fail
        }
    }
}
