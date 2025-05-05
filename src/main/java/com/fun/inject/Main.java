package com.fun.inject;


import com.fun.inject.utils.InjectUtils;
import com.fun.inject.utils.InjectorUtils;
import com.sun.tools.attach.VirtualMachine;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static com.fun.utils.file.NativeExtractor.extractNativesFromJar;

public class Main {
    public static final String path = System.getProperty("user.dir");
    public static final String dllpath = "natives/libagent.dll";
    public static final String hwiSpooferpath = "natives/libhwidSpoofer.dll";
    public static VirtualMachine vm;
    public static String pid;
    public static File workDir = new File(System.getProperty("user.home"), ".foto");

    static {

        if (!Bootstrap.isRemote) {
            extractNativesFromJar(new File(workDir, "natives"));
            System.load(new File(workDir ,"/natives/libinjector.dll").getAbsolutePath());
        }
        //InjectorUtils.addToSystemClassLoaderSearch(new File(path,"UI.jar").getAbsolutePath());
    }

    public static String getJarPath() {
        try {
            ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            URL location = protectionDomain.getCodeSource().getLocation();
            return new File(location.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to get JAR path", e);
        }
    }

    public static void start() {
        Bootstrap.classLoader = Main.class.getClassLoader();
        Scanner sc = new Scanner(System.in);
        System.out.println("Please enter the target Minecraft PID.");
        pid = String.valueOf(sc.nextLine());
        InjectUtils.stopSearch();
        File f = new File(System.getProperty("user.home") + "\\foto_path.txt");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {

            }
        }
        try {
            PrintWriter pw = new PrintWriter(f);
            pw.println(getJarPath());
            //if(!Main.class.getName().contains("com.fun"))pw.print(Main.class.getName().substring(0, Math.min(16, Main.class.getName().length())));
            pw.close();
        } catch (FileNotFoundException e) {

        }
        File dll = new File(workDir, dllpath);
        InjectorUtils.injectorR(Integer.parseInt(pid), dll.getAbsolutePath());

        //System.out.println("injected in:"+pid);


    }

    public static String getNewPackage() {
        if (!Main.class.getName().contains("com.fun"))
            return (Main.class.getName().substring(0, Math.min(16, Main.class.getName().length())));
        return null;
    }

    public static void main(String[] args) {

        InjectUtils.searchForProcess();
        start();
    }

    private static File writeagent(Class<?> class1) {
        try {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
            manifest.getMainAttributes().putValue("Can-Redefine-Classes", "true");
            manifest.getMainAttributes().putValue("Can-Retransform-Classes", "true");
            manifest.getMainAttributes().putValue("Agent-Class", class1.getName());
            File file = new File(System.getProperty("java.io.tmpdir") + getRandomString(6) + ".jar");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            new JarOutputStream(new FileOutputStream(file), manifest).close();
            file.deleteOnExit();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
