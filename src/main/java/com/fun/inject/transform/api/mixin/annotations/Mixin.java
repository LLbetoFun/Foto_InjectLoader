package com.fun.inject.transform.api.mixin.annotations;

import com.fun.inject.Bootstrap;
import com.fun.utils.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mixin {
    Class<?> value();

    class Helper {
        public static Mixin fromNode(AnnotationNode annotation) {
            return new Mixin() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Mixin.class;
                }

                @Override
                public Class<?> value() {
                    try {
                        return Bootstrap.findClass(getTargetName(annotation));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        public static String getTargetName(AnnotationNode annotation) {
            return ((Type) ASMUtils.getAnnotationValue(annotation, "value")).getClassName();
        }

        public static boolean isAnnotation(@NotNull AnnotationNode node) {
            return node.desc.contains(ASMUtils.slash(Mixin.class.getName()));
        }

        public static boolean hasAnnotation(@NotNull ClassNode node) {
            return node.visibleAnnotations != null && node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static @Nullable Mixin getAnnotation(ClassNode node) {
            if (!hasAnnotation(node)) return null;
            return fromNode(node.visibleAnnotations.stream().filter(Helper::isAnnotation).findFirst().orElse(null));
        }
    }
}
