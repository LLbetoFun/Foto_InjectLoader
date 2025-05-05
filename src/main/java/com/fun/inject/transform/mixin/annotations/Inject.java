package com.fun.inject.transform.mixin.annotations;


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
public @interface Inject {
    String method();

    String desc();

    At at();

    boolean deleteLastReturn() default false;

    class Helper {
        public static Inject fromNode(AnnotationNode annotation) {
            return new Inject() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Inject.class;
                }

                @Override
                public String method() {
                    return ASMUtils.getAnnotationValue(annotation, "method");
                }

                @Override
                public String desc() {
                    return ASMUtils.getAnnotationValue(annotation, "desc");
                }

                @Override
                public At at() {
                    AnnotationNode annotationNode = ASMUtils.getAnnotationValue(annotation, "at");
                    if (annotationNode == null) return null;
                    return At.Helper.fromNode(annotationNode);
                }

                @Override
                public boolean deleteLastReturn() {
                    Object object = ASMUtils.getAnnotationValue(annotation, "deleteLastReturn");
                    if (object == null) return false;
                    return (boolean) object;
                }
            };
        }

        public static boolean isAnnotation(@NotNull AnnotationNode node) {
            return node.desc.contains(ASMUtils.slash(Inject.class.getName()));
        }

        public static boolean hasAnnotation(@NotNull MethodNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static @Nullable Inject getAnnotation(MethodNode node) {
            if (!hasAnnotation(node)) return null;
            return fromNode(node.visibleAnnotations.stream().filter(Helper::isAnnotation).findFirst().orElse(null));
        }
    }
}
