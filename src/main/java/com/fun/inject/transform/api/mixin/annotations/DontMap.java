package com.fun.inject.transform.api.mixin.annotations;

import com.fun.utils.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD})
public @interface DontMap {
    class Helper {
        public static boolean isAnnotation(@NotNull AnnotationNode node) {
            return node.desc.substring(1, node.desc.length() - 1).equals(ASMUtils.slash(DontMap.class.getName()));
        }

        public static boolean hasAnnotation(@NotNull ClassNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static boolean hasAnnotation(@NotNull MethodNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }
    }
}
