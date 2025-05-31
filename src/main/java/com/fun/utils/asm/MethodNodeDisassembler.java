package com.fun.utils.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MethodNodeDisassembler {

    /**
     * 打印MethodNode的所有指令及详细信息
     * @param methodNode 要分析的方法节点
     */
    public static void printMethodInstructions(MethodNode methodNode) {
        System.out.println("Method: " + methodNode.name + methodNode.desc);
        System.out.println("Max stack: " + methodNode.maxStack);
        System.out.println("Max locals: " + methodNode.maxLocals);
        System.out.println("Access flags: " + getAccessFlags(methodNode.access));
        System.out.println("--------------------------------------------------");

        // 使用Textifier生成可读的指令表示
        Printer printer = new Textifier();
        TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(printer);

        // 遍历所有指令
        for (AbstractInsnNode insn : methodNode.instructions) {
            System.out.printf("%-5d %-15s", methodNode.instructions.indexOf(insn), getOpcodeName(insn.getOpcode()));

            // 输出指令详细信息
            insn.accept(traceMethodVisitor);
            String insnText = printer.getText().get(0).toString().trim();
            System.out.println(insnText);

            printer.getText().clear();
        }
    }

    /**
     * 获取操作码名称
     */
    private static String getOpcodeName(int opcode) {
        if (opcode < 0) return "LABEL";
        return Opcodes.class.getFields()[opcode].getName();
    }

    /**
     * 获取访问标志字符串
     */
    private static String getAccessFlags(int access) {
        List<String> flags = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0) flags.add("PUBLIC");
        if ((access & Opcodes.ACC_PRIVATE) != 0) flags.add("PRIVATE");
        if ((access & Opcodes.ACC_PROTECTED) != 0) flags.add("PROTECTED");
        if ((access & Opcodes.ACC_STATIC) != 0) flags.add("STATIC");
        if ((access & Opcodes.ACC_FINAL) != 0) flags.add("FINAL");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) flags.add("SYNCHRONIZED");
        if ((access & Opcodes.ACC_NATIVE) != 0) flags.add("NATIVE");
        if ((access & Opcodes.ACC_ABSTRACT) != 0) flags.add("ABSTRACT");
        return String.join(" | ", flags);
    }

    /**
     * 示例：分析一个类的方法
     */
    public static void analyzeClass(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (MethodNode method : classNode.methods) {
            printMethodInstructions(method);
            System.out.println("\n\n");
        }
    }

    public static void main(String[] args) throws Exception {
        // 示例：分析当前类
        analyzeClass(Files.readAllBytes(Paths.get(
            MethodNodeDisassembler.class.getResource(
                MethodNodeDisassembler.class.getSimpleName() + ".class"
            ).toURI()
        )));
    }
}