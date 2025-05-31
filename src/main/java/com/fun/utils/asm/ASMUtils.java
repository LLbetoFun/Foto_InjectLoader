package com.fun.utils.asm;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ASMUtils {
    public static String slash(String s) {
        return s.replace(".", "/");
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

    @Nullable
    public static AnnotationNode getAnnotationNode(MethodNode node, Class<? extends Annotation> klass) {
        if ( node.visibleAnnotations == null ) return null;
        for( AnnotationNode annotationNode : node.visibleAnnotations ) {
            if (String.format("L%s;",Type.getInternalName(klass)).equals(annotationNode.desc)) {
                return annotationNode;
            }
        }
        return null;
    }

    public static boolean isLoadOpe(int opcode) {
        for (Field field : Opcodes.class.getFields())
            if (field.getName().endsWith("LOAD"))
                try {
                    if ((int) field.get(null) == opcode)
                        return true;
                } catch (Throwable ignored) {
                }
        return false;
    }

    public static boolean isStoreOpe(int opcode) {
        for (Field field : Opcodes.class.getFields())
            if (field.getName().endsWith("STORE"))
                try {
                    if ((int) field.get(null) == opcode)
                        return true;
                } catch (Throwable ignored) {
                }
        return false;
    }
    public static boolean isStaticMethod(MethodNode methodNode) {
        return (methodNode.access & Opcodes.ACC_STATIC) != 0;
    }
    public static boolean isRetOpe(int opcode) {
        for (Field field : Opcodes.class.getFields())
            if (field.getName().endsWith("RETURN"))
                try {
                    if ((int) field.get(null) == opcode)
                        return true;
                } catch (Throwable ignored) {
                }
        return false;
    }
    // 正确复制指令的方法
    public static InsnList cloneInsnList(InsnList source) {
        InsnList copy = new InsnList();
        Map<LabelNode, LabelNode> labelMap = new HashMap<>();

        // 第一遍：复制所有指令并建立标签映射
        for (AbstractInsnNode insn : source) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode)insn, new LabelNode());
            }
        }

        // 第二遍：完成复制并修复跳转
        for (AbstractInsnNode insn : source) {
            copy.add(insn.clone(labelMap)); // 使用clone方法处理标签重映射
        }

        return copy;
    }
    public static Block getBlock(AbstractInsnNode node, InsnList list) {
        LabelNode first = new LabelNode(), last = new LabelNode();
        list.insertBefore(node,first);
        list.insert(node,last);
        for (int i = 0; i < list.size(); i++) {
            AbstractInsnNode abstractInsnNode = list.get(i);
            if (abstractInsnNode instanceof LabelNode)
                first = (LabelNode) abstractInsnNode;
            if (abstractInsnNode == node)
                break;
        }
        boolean passed = false;
        for (AbstractInsnNode abstractInsnNode : list) {
            if (abstractInsnNode == node)
                passed = true;
            if (passed) {
                if (abstractInsnNode instanceof LabelNode) {
                    last = (LabelNode) abstractInsnNode;
                    break;
                }
            }
        }
        return new Block(first, last);
    }

}
