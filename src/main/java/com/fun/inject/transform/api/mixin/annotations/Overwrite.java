package com.fun.inject.transform.api.mixin.annotations;


import com.fun.utils.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;


import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
public @interface Overwrite {
    String method();

    String desc();

    class Helper {
        public static Overwrite fromNode(AnnotationNode annotation) {
            return new Overwrite() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Overwrite.class;
                }

                @Override
                public String method() {
                    return ASMUtils.getAnnotationValue(annotation, "method");
                }

                @Override
                public String desc() {
                    return ASMUtils.getAnnotationValue(annotation, "desc");
                }
            };
        }

        public static boolean isAnnotation(@NotNull AnnotationNode node) {
            return node.desc.contains(ASMUtils.slash(Overwrite.class.getName()));
        }

        public static boolean hasAnnotation(@NotNull MethodNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static @Nullable Overwrite getAnnotation(MethodNode node) {
            if (!hasAnnotation(node)) return null;
            return fromNode(node.visibleAnnotations.stream().filter(Helper::isAnnotation).findFirst().orElse(null));
        }
    }
}
