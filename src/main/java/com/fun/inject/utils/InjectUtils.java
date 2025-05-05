package com.fun.inject.utils;


import com.fun.inject.Bootstrap;
import com.fun.inject.Main;
import com.fun.inject.version.MinecraftType;
import com.fun.inject.version.MinecraftVersion;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;

public class InjectUtils {

    static boolean threadKeepGoing = false;

    public static String getSrg(MinecraftVersion ver, MinecraftType type) {
        return "assets/mappings/" + type.getType() + "_" + ver.getVer() + ".srg";
    }

    public static String getCvsM(MinecraftVersion ver) {
        return "assets/mappings/" + "methods" + ver.getVer() + ".csv";
    }

    public static String getCvsF(MinecraftVersion ver) {
        return "assets/mappings/" + "fields" + ver.getVer() + ".csv";
    }

    public static int getMinecraftProcessId() throws InterruptedException {
        User32 user32 = User32.INSTANCE;
        IntByReference pid = new IntByReference(-1);
        do {
            //for(String s:WindowEnumerator.getWindows())
            WinDef.HWND findWindow;
            WinDef.HWND hWnd = findWindow = user32.FindWindow("LWJGL", null);//GLFW30

            if (findWindow != null && findWindow.getPointer() != null) {
                char[] buffer = new char[1024];
                user32.GetWindowText(hWnd, buffer, buffer.length);
                final String windowText = new String(buffer);
                user32.GetWindowThreadProcessId(hWnd, pid);
            } else {
                Thread.sleep(100L);
            }

            hWnd = findWindow = user32.FindWindow("GLFW30", null);//GLFW30

            if (findWindow != null && findWindow.getPointer() != null) {
                char[] buffer = new char[1024];
                user32.GetWindowText(hWnd, buffer, buffer.length);
                final String windowText = new String(buffer);
                user32.GetWindowThreadProcessId(hWnd, pid);
            } else {
                Thread.sleep(100L);
            }
        } while (pid.getValue() == -1);
        return pid.getValue();
    }

    public static void searchForProcess() {
        threadKeepGoing = true;
        ArrayList<Integer> pids = new ArrayList<>();
        new Thread(() -> {
            User32 user32 = User32.INSTANCE;
            IntByReference pid = new IntByReference(-1);
            while (threadKeepGoing) {
                do {

                    //for(String s:WindowEnumerator.getWindows())
                    WinDef.HWND findWindow;
                    WinDef.HWND hWnd = findWindow = user32.FindWindow("LWJGL", null);//GLFW30

                    if (findWindow != null && findWindow.getPointer() != null) {
                        char[] buffer = new char[1024];
                        user32.GetWindowText(hWnd, buffer, buffer.length);
                        final String windowText = new String(buffer);
                        user32.GetWindowThreadProcessId(hWnd, pid);
                    } else {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    hWnd = findWindow = user32.FindWindow("GLFW30", null);//GLFW30

                    if (findWindow != null && findWindow.getPointer() != null) {
                        char[] buffer = new char[1024];
                        user32.GetWindowText(hWnd, buffer, buffer.length);
                        final String windowText = new String(buffer);
                        user32.GetWindowThreadProcessId(hWnd, pid);
                    } else {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } while (pid.getValue() == -1);
                Integer id = pid.getValue();
                if (!pids.contains(id)) {
                    InjectorUtils.injectorR(id, new File(Main.workDir, Main.hwiSpooferpath).getAbsolutePath());
                    System.out.println("Pid: " + id);
                    pids.add(id);
                }
            }

        }).start();
    }

    public static void stopSearch() {
        threadKeepGoing = false;
    }

    public static int getMinecraftProcessId2() throws InterruptedException {
        User32 user32 = User32.INSTANCE;
        IntByReference pid = new IntByReference(-1);
        do {
            //for(String s:WindowEnumerator.getWindows())
            WinDef.HWND findWindow;
            WinDef.HWND hWnd = findWindow = user32.FindWindow("GLFW30", null);//GLFW30

            if (findWindow != null && findWindow.getPointer() != null) {
                char[] buffer = new char[1024];
                user32.GetWindowText(hWnd, buffer, buffer.length);
                final String windowText = new String(buffer);
                user32.GetWindowThreadProcessId(hWnd, pid);
            } else {
                Thread.sleep(100L);
            }
        } while (pid.getValue() == -1);
        return pid.getValue();
    }

    public static void redefineClass(Class<?> clazz, byte[] newByte) throws UnmodifiableClassException, ClassNotFoundException {
        Bootstrap.instrumentation.redefineClass(clazz, newByte);
    }

    public static byte[] getClassBytes(Class<?> c) throws IOException {
        String className = c.getName();
        String classAsPath = className.replace('.', '/') + ".class";
        InputStream stream = c.getClassLoader().getResourceAsStream(classAsPath);
        byte[] bs = stream == null ? null : IOUtils.toByteArray(stream);
        return bs;
    }

    public static byte[] getClassBytes(ClassLoader cl, String name) throws IOException {
        String className = name;
        String classAsPath = className.replace('.', '/') + ".class";
        InputStream stream = cl.getResourceAsStream(classAsPath);
        byte[] bs = stream == null ? null : IOUtils.toByteArray(stream);
        return bs;
    }

}
