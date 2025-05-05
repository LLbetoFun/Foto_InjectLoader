package com.fun.inject;


import com.fun.api.interfaces.FotoInjection;
import com.fun.inject.define.Definer;
import com.fun.inject.mapper.Mapper;
import com.fun.inject.mapper.RemapException;
import com.fun.inject.transform.impl.GameClassTransformer;
import com.fun.inject.transform.api.Transformer;
import com.fun.inject.transform.api.Transformers;
import com.fun.inject.utils.InjectUtils;
import com.fun.inject.utils.Native;
import com.fun.inject.utils.NativeUtils;
import com.fun.inject.utils.ReflectionUtils;
import com.fun.inject.version.MinecraftType;
import com.fun.inject.version.MinecraftVersion;
import com.fun.utils.file.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Bootstrap {

    //FunGhostClient.jar
    public static String jarPath;
    //isRemote
    public static boolean isRemote = false;
    public static int SERVERPORT = 11432;
    public static Map<String, byte[]> classes = new HashMap<>();
    public static Native instrumentation;
    public static GameClassTransformer transformer;
    public static MinecraftType minecraftType = MinecraftType.VANILLA;
    public static MinecraftVersion minecraftVersion = MinecraftVersion.VER_189;
    @Deprecated
    public static String[] selfClasses = new String[]{"com.fun", "org.newdawn", "javax.vecmath", "org.objectweb", "org.jetbrains.skija", "org.joml", "org.java_websocket"};
    public static List<String> transformClasses = new ArrayList<>();
    public static List<String> injectionMainClasses = new ArrayList<>();
    public static ClassLoader classLoader;

    private static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inStream.read(buffer)) != -1)
            outStream.write(buffer, 0, len);
        outStream.close();
        return outStream.toByteArray();

    }

    public static void cacheJar(File file) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
                if (!entry.isDirectory())
                    if (entry.getName().endsWith(".class"))
                        classes.put(entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6), readStream(zis));
        }
    }


    public static int getArgumentCount(String methodDescriptor) {
        int argumentCount = 0;

        for (int currentOffset = 1; methodDescriptor.charAt(currentOffset) != ')'; ++argumentCount) {
            while (methodDescriptor.charAt(currentOffset) == '[') {
                ++currentOffset;
            }

            if (methodDescriptor.charAt(currentOffset++) == 'L') {
                int semiColumnOffset = methodDescriptor.indexOf(59, currentOffset);
                currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
            }
        }

        return argumentCount;
    }

    public static byte[] readClazzBytes(Class<?> c) throws IOException {

        return InjectUtils.getClassBytes(c);//(c.getName().replace('.', '/') + ".class"));
    }
    @Deprecated
    public static boolean isSelfClass(String name) {
        if (Main.getNewPackage() != null && name.startsWith(Main.getNewPackage())) return true;

        name = name.replace('/', '.');
        String[] slash = name.split("\\.");
        if (slash.length == 2 && slash[0].length() == 16) {
            return true;
        }
        for (String cname : getSelfClasses()) {
            if (name.startsWith(cname)) {
                return true;
            }
        }
        return false;
    }
    @Deprecated
    public static String[] getSelfClasses() {
        return selfClasses;

    }


    public static void initVersion() {
        minecraftVersion = MinecraftVersion.getMinecraftVersion();
        try {
            Class<?> c = findClass("net.minecraft.client.Minecraft");//com/heypixel/heypixel/HeyPixel
            if (c != null) {
                minecraftType = MinecraftType.MCP;
                if (ReflectionUtils.getFieldValue(c, minecraftVersion == MinecraftVersion.VER_1181 ? "f_90981_" : "field_71432_P") != null)//m_91087_
                    minecraftType = MinecraftType.FORGE;
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
    }


    private static void loadJar(URLClassLoader urlClassLoader, URL jar) {
        NativeUtils.loadJar(urlClassLoader, jar);
    }

    private static void defineClassesInCache() {
        for (String s : classes.keySet()) {
            Definer.sortClass(s);
        }
        Definer.defineClasses();
    }
    private static void clearClassCache() {
        classes.clear();
    }
    private static void parseAPIsInCache() {
        classes.keySet().forEach((s -> {
            byte[] classBytes = classes.get(s);
            ClassNode classNode = Transformers.node(classBytes);
            if(classNode.visibleAnnotations != null)
                classNode.visibleAnnotations.forEach(annotationNode -> {
                    if (annotationNode.desc.equals("Lcom/fun/api/annotations/Injection;")) {
                        injectionMainClasses.add(s);

                    }
                    if (annotationNode.desc.equals("Lcom/fun/api/annotations/Transform;")) {
                        transformClasses.add(s);
                    }
                });
        }));
    }
    public static void startInjectThread() {//启动注入线程
        new Thread(Bootstrap::inject).start();
    }

    public static native void inject();//初始化完毕后调用start

    public synchronized static void start() throws URISyntaxException, IOException, InterruptedException {//启动方法
        isRemote = true;
        File f = new File(System.getProperty("user.home") + "\\foto_path.txt");
        BufferedReader bufferedreader = new BufferedReader(new FileReader(f));
        String line = "";
        while ((line = bufferedreader.readLine()) != null) {
            jarPath = line;
            break;
        }
        bufferedreader.close();
        instrumentation = new Native();
        boolean running = true;
        while (running) {
            for (Object o : Thread.getAllStackTraces().keySet().toArray()) {
                Thread thread = (Thread) o;
                if (thread.getName().equals("Client thread") || thread.getName().equals("Render thread")) {

                    classLoader = thread.getContextClassLoader();
                    running = false;
                    break;
                }
            }
        }

        initVersion();

        File injectionDir = new File(Main.workDir, "/injections");
        if(!injectionDir.exists())injectionDir.mkdirs();
        Arrays.stream(injectionDir.listFiles()).forEach(file -> {
            try {
                if(FileUtils.getFileExtension(file).equals(".jar"))loadJar(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            if (classLoader.getClass().getName().contains("launchwrapper") || classLoader.getClass().getName().contains("modlauncher")) {
                cacheJar(new File(jarPath));
                defineClassesInCache();
            } else if (ClassLoader.getSystemClassLoader() != (classLoader)) {
                loadJar((URLClassLoader) classLoader, new File(jarPath).toURI().toURL());
            }


            Class<?> agentClass = findClass(Bootstrap.class.getName());
            for (Method m : agentClass.getDeclaredMethods()) {
                if (m.getName().equals("init")) {
                    m.invoke(null, classLoader, jarPath);

                }
            }

            injectionMainClasses.forEach(c -> {
                try {
                    System.out.println("Init injection main class:"+c);
                    Class<?> mainClass = findClass(c);
                    Object fotoInjection= mainClass.getConstructor().newInstance();
                    for (Method m : mainClass.getDeclaredMethods()) {
                        if (m.getName().equals("initialize")) {

                            m.invoke(fotoInjection);

                        }
                    }

                } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException | InstantiationException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        transform();


    }
    private static void loadJar(File targetJar) throws Exception {
        targetJar=Mapper.mapJar(targetJar, minecraftVersion, minecraftType);
        //remap

        cacheJar(targetJar);
        //cache

        NativeUtils.addToSystemClassLoaderSearch(targetJar.getAbsolutePath());
        if (classLoader.getClass().getName().contains("launchwrapper") || classLoader.getClass().getName().contains("modlauncher")) {
            defineClassesInCache();
        } else if (ClassLoader.getSystemClassLoader() != (classLoader)) {
            loadJar((URLClassLoader) classLoader, targetJar.toURI().toURL());
        }
        //define/addpath

        parseAPIsInCache();
        //load apis

        clearClassCache();
        //clear

    }

    public static void transform() {
        Transformers.init();
        transformer = new GameClassTransformer();
        instrumentation.addTransformer(transformer, true);

        //NativeUtils.messageBox("native cl:"+NativeUtils.class.getClassLoader(),"Fish");

        for (Transformer transformer : Transformers.transformers) {
            try {

                if (transformer.clazz == null) {
                    continue;
                }

                NativeUtils.retransformClass(transformer.clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        instrumentation.doneTransform();

        System.out.println("Transform classes successfully");


    }

    public static void init(ClassLoader cl, String jarPathIn) {
        classLoader = cl;
        jarPath = jarPathIn;
        isRemote = true;
        initVersion();
        //File injection = new File(new File(jarPath).getParent(), "/injections/" + minecraftVersion.injection);
        try {
            Mapper.readMappings(minecraftVersion, minecraftType);
        } catch (RemapException e) {
            e.printStackTrace();
        }

        System.out.println("foto core initialized!");


    }

    public static Class<?> findClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name.replace('/', '.'));
    }

    public static Class<?> findSysClass(String name) throws ClassNotFoundException {
        return ClassLoader.getSystemClassLoader().loadClass(name.replace('/', '.'));
    }

    public static void destroyClient() {
        instrumentation.removeTransformer(transformer);
        Transformers.transformers.forEach(it -> {
            NativeUtils.redefineClass(it.getClazz(), it.getOldBytes());
        });
        //todo destroy
        NativeUtils.destroy();
    }


}
