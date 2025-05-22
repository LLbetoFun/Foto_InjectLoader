package com.fun.inject.transform.impl;

import com.fun.inject.Bootstrap;
import com.fun.inject.mapper.Mapper;
import com.fun.inject.transform.IClassTransformer;
import com.fun.inject.transform.api.asm.Inject;
import com.fun.inject.transform.api.asm.Transformer;
import com.fun.inject.transform.api.asm.Transformers;
import com.fun.inject.utils.FishClassWriter;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class ASMClassTransformer implements IClassTransformer {
    public static ClassNode node(byte[] bytes) {
        if (bytes != null && bytes.length != 0) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.EXPAND_FRAMES);
            return node;
        }

        return null;
    }

    public static byte[] rewriteClass(ClassNode node) {
        try {
            ClassWriter writer = new FishClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
            //ClassRemapper remapper = new ClassRemapper(writer, new ObfMapper.ReMapper());
            node.accept(writer);
            return writer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        for (Transformer transformer : Transformers.transformers) {
            if (transformer.clazz == classBeingRedefined && loader == Bootstrap.classLoader) {
                transformer.oldBytes = classfileBuffer;

                try {
                    FileUtils.writeByteArrayToFile(new File(System.getProperty("user.home"), transformer.getName() + "Old.class"), classfileBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ClassNode node = Transformers.node(transformer.oldBytes);

                //System.out.println("1");
                for (Method method : transformer.getClass().getDeclaredMethods()) {
                    //System.out.println("2");
                    //System.out.println(method.toString());
                    if (method.isAnnotationPresent(Inject.class)) {

                        if (method.getParameterCount() != 1 || !MethodNode.class.isAssignableFrom(method.getParameterTypes()[0]))
                            continue;

                        Inject inject = method.getAnnotation(Inject.class);

                        String methodToModify = inject.method();
                        String desc = inject.descriptor();

                        String obfName = Mapper.getMappedMethod(methodToModify, transformer.getName(), desc);
                        String obfDesc = Mapper.getMappedMethodDesc(desc);
                        if (obfName == null || obfName.isEmpty()) {
                            //System.out.println("Could not find {} in class {}", methodToModify, transformer.getName());
                            continue;
                        }


                        for (MethodNode mNode : node.methods) {

                            if (mNode.name.equals(obfName) && mNode.desc.equals(obfDesc)) {
                                try {
                                    method.invoke(transformer, mNode);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    }


                }

                byte[] newBytes = rewriteClass(node);
                if (newBytes == null) {
                    System.out.println(className + " rewriteClass failed");
                    return null;
                }
                File fo = new File(System.getProperty("user.home"), transformer.getName() + ".class");


                try {

                    FileUtils.writeByteArrayToFile(fo, newBytes.clone());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("transformer:" + transformer.getName() + " bytes in " + fo.getAbsolutePath());

                transformer.newBytes = newBytes;
                return transformer.newBytes;
            }
        }
        return null;
    }
}
