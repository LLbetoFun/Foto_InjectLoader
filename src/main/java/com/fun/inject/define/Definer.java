package com.fun.inject.define;

import com.fun.inject.Bootstrap;
import com.fun.inject.utils.NativeUtils;
import com.fun.inject.transform.api.asm.Transformers;
import com.fun.inject.utils.ReflectionUtils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Definer {
    public static ArrayList<String> undefinedClasses = new ArrayList<>();
    public static ArrayList<String> sorted = new ArrayList<>();
    public static Map<String, byte[]> cache = new HashMap<>();
    public static String lastClassName;

    public static void sortClass(String className) {
        if (className == null) return;
        className = className.replace('/', '.');
        byte[] bytes = Bootstrap.classes.get(className);
        ClassNode cn;
        try {
            cn = Transformers.node(bytes);

        } catch (Exception e) {
            System.out.println("err:" + className);
            File fo = new File(System.getProperty("user.home"), className.replace('.', '/') + ".class");
            try {
                FileUtils.writeByteArrayToFile(fo, bytes);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return;
        }
        if (cn == null) {
            //System.out.println("classNode is Null:"+className);
            return;
        }
        if (!undefinedClasses.contains(cn.name) && !sorted.contains(cn.name)) {
            sorted.add(cn.name);


            sortClass(cn.superName);
            for (String s : cn.interfaces) {
                sortClass(s);
            }
            if ((cn.access & Opcodes.ACC_INTERFACE) == 0)
                for (MethodNode mn : cn.methods) {
                    if (mn.name.equals("<clinit>")) {
                        for (String uc : usedClasses(mn)) {
                            sortClass(uc);
                        }
                    }
                }
            if (!undefinedClasses.contains(cn.name)) {
                undefinedClasses.add(cn.name);
                cache.put(cn.name, bytes);
            }

        } else if (sorted.contains(cn.name) && !undefinedClasses.contains(cn.name)) {
            undefinedClasses.add(cn.name);
            cache.put(cn.name, bytes);
        }


    }

    public static void defineClasses() {
        for (String className : undefinedClasses) {
            byte[] bytes = cache.get(className);
            try {
                if (ReflectionUtils.invokeMethod(Bootstrap.classLoader, "findLoadedClass", new Class[]{String.class}, className.replace('/', '.')) == null) {

                    NativeUtils.defineClass(Bootstrap.classLoader, bytes);
                }

            } catch (Throwable e) {
                e.printStackTrace();

            }
        }


    }

    public static ArrayList<String> usedClasses(MethodNode mn) {
        ArrayList<String> usedClasses = new ArrayList<>();
        for (AbstractInsnNode ain : mn.instructions.toArray()) {
            if (ain instanceof MethodInsnNode) {
                usedClasses.add(((MethodInsnNode) ain).owner);
            }
            if (ain instanceof FieldInsnNode) {
                usedClasses.add(((FieldInsnNode) ain).owner);
            }
            if (ain instanceof TypeInsnNode) {
                usedClasses.add(((TypeInsnNode) ain).desc);
            }
            if (ain instanceof LdcInsnNode) {
                if (((LdcInsnNode) ain).cst instanceof Type) {
                    usedClasses.add(((Type) ((LdcInsnNode) ain).cst).getInternalName());
                }
            }
        }
        return usedClasses;
    }

    public final static class Pair {
        public String name;
        public byte[] bytes;

        public Pair(String first, byte[] second) {
            this.name = first;
            this.bytes = second;
        }
    }
}
