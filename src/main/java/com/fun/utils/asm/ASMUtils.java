package com.fun.utils.asm;

import org.objectweb.asm.tree.AnnotationNode;

public class ASMUtils {
    public static String slash(String s) {
        return s.replace('.', '/');
    }

    public static <T> T getAnnotationValue(AnnotationNode node, String name) {
        if (node != null)
            for (int i = 0; i < node.values.size(); i += 2) {
                if (node.values.get(i).equals(name)) {
                    Object obj = node.values.get(i + 1);
                    return (T) obj;
                }
            }
        return null;
    }

}
