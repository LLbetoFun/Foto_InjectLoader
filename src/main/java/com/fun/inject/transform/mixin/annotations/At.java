package com.fun.inject.transform.mixin.annotations;



import com.fun.utils.asm.ASMUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.PARAMETER)
public @interface At {
    enum Shift {
        AFTER, BEFORE
    }

    String value();

    String target() default "";

    Shift shift() default Shift.BEFORE;

    int ordinal() default 0;

    class Helper {
        public static At fromNode(AnnotationNode annotation) {
            return new At() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return At.class;
                }

                @Override
                public String value() {
                    return ASMUtils.getAnnotationValue(annotation, "value");
                }

                @Override
                public String target() {
                    Object value = ASMUtils.getAnnotationValue(annotation, "target");
                    if (value == null) return "";
                    return (String) value;
                }

                @Override
                public Shift shift() {
                    Object value = ASMUtils.getAnnotationValue(annotation, "shift");
                    if (value == null) return Shift.BEFORE;
                    return Shift.valueOf(((String[]) value)[1]);
                }

                @Override
                public int ordinal() {
                    Object value = ASMUtils.getAnnotationValue(annotation, "ordinal");
                    if (value == null) return 0;
                    return (int) value;
                }
            };
        }

        public static boolean isAnnotation(AnnotationNode node) {
            return node.desc.contains(ASMUtils.slash(At.class.getName()));
        }

        public static boolean hasAnnotation(MethodNode node) {
            return node.visibleAnnotations.stream().anyMatch(Helper::isAnnotation);
        }

        public static At getAnnotation(MethodNode node) {
            if (!hasAnnotation(node)) return null;
            return fromNode(node.visibleAnnotations.stream().filter(Helper::isAnnotation).findFirst().orElse(null));
        }
    }
}
