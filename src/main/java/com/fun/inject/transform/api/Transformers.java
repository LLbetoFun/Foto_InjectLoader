package com.fun.inject.transform.api;


import com.fun.api.annotations.Transform;
import com.fun.inject.Bootstrap;
import com.fun.inject.mapper.Mapper;
import com.fun.inject.transform.mixin.annotations.Mixin;
import com.fun.inject.utils.FishClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

public class Transformers {


    public static List<Transformer> transformers = new ArrayList<>();
    public static List<Mixin> mixins = new ArrayList<>();

    public static boolean contains(String[] s1, String target) {
        for (String s : s1) {
            if (!target.replace('.', '/').contains(Mapper.getMappedClass(s).replace('.', '/'))) return false;
        }
        return true;
    }


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
            node.accept(writer);
            return writer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void init() {
        Transformers.transformers.clear();

        Bootstrap.transformClasses.forEach(tClass -> {
            try {
                Class<?> c=Class.forName(tClass);
                Transform transform=c.getAnnotation(Transform.class);
                if(c.getSuperclass() == Transformer.class) {
                    Constructor<Transformer> constructor = Transformer.class.getDeclaredConstructor(Class.class);
                    transformers.add(constructor.newInstance(transform.clazz()));
                    System.out.println("Transformer " + c.getName() + " loaded");
                }

            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
