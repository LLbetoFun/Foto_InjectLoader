package com.fun.inject.transform.mixin.annotations;


import com.fun.inject.transform.mixin.StringPair;
import com.fun.utils.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Shadow {
    class Helper {
        public static boolean isAnnotation(@NotNull AnnotationNode node) {
            return node.desc.substring(1, node.desc.length() - 1).equals(ASMUtils.slash(Shadow.class.getName()));
        }

        public static boolean hasAnnotation(@NotNull MethodNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static boolean hasAnnotation(@NotNull FieldNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static void recordShadow(ClassNode node, ArrayList<StringPair> methodShadows, ArrayList<StringPair> fieldShadows) {
            // Process @Shadow
            for (MethodNode method : node.methods) {
                if (Helper.hasAnnotation(method))
                    methodShadows.add(new StringPair(method.name, method.desc));
            }
            for (FieldNode field : node.fields) {
                if (Helper.hasAnnotation(field))
                    fieldShadows.add(new StringPair(node.name, field.name));
            }
        }

        public static void processShadow(MethodNode method, String targetName, ArrayList<StringPair> methodShadows, ArrayList<StringPair> fieldShadows) {
            // Process @Shadow
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                    if (methodShadows.stream().anyMatch(m -> m.a.equals(methodInsnNode.name) && m.b.equals(methodInsnNode.desc)))
                        methodInsnNode.owner = targetName;
                } else if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                    if (fieldShadows.stream().anyMatch(f -> f.a.equals(fieldInsnNode.owner) && f.b.equals(fieldInsnNode.name)))
                        fieldInsnNode.owner = targetName;
                }
            }
        }

        public static void processShadowBySuper(ClassNode node, String superClz) {
            // Process @Shadow
            ArrayList<StringPair> methodShadows = new ArrayList<>();
            ArrayList<StringPair> fieldShadows = new ArrayList<>();
            String targetName = superClz;
            targetName = targetName.replace('.', '/');
            for (MethodNode method : node.methods) {
                if (Helper.hasAnnotation(method))
                    methodShadows.add(new StringPair(method.name, method.desc));
            }
            for (FieldNode field : node.fields) {
                if (Helper.hasAnnotation(field))
                    fieldShadows.add(new StringPair(node.name, field.name));
            }
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                        if (methodShadows.stream().anyMatch(m -> m.a.equals(methodInsnNode.name) && m.b.equals(methodInsnNode.desc)))
                            methodInsnNode.owner = targetName;
                    } else if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                        if (fieldShadows.stream().anyMatch(f -> f.a.equals(fieldInsnNode.owner) && f.b.equals(fieldInsnNode.name)))
                            fieldInsnNode.owner = targetName;
                    }
                }
            }
        }
    }
}
