package com.fun.utils.asm;

import org.objectweb.asm.Type;

public class MixinTargetParser {

    public static class ParsedMethod {
        public String className;      // 完整类名（包含L和;）
        public String methodName;     // 方法名
        public String methodDesc;     // 完整方法描述符（参数和返回类型）

        @Override
        public String toString() {
            return "ParsedMethod{" +
                    "className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", methodDesc='" + methodDesc + '\'' +
                    '}';
        }
    }

    public static class ParsedField {
        public String owner;    // 类名（Lnet/minecraft/client/Minecraft; 格式）
        public String name;     // 字段名
        public String desc;    // 字段类型描述符（Lnet/minecraft/util/profiling/ProfilerFiller; 格式）

        @Override
        public String toString() {
            return "ParsedField{" +
                    "owner='" + owner + '\'' +
                    ", name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

    public static ParsedMethod parseMethodTarget(String descriptor) {
        ParsedMethod result = new ParsedMethod();
        result.methodDesc = descriptor;

        // 查找方法名的起始位置（最后一个/和(之间的部分）
        int methodStart = descriptor.indexOf(';') + 1;
        int paramStart = descriptor.indexOf('(', methodStart);

        if (methodStart > 0 && paramStart > methodStart) {
            // 提取完整类名（包括L和;）
            result.className = Type.getType(descriptor.substring(0, methodStart)).getInternalName();

            // 提取方法名（;和(之间的部分）
            result.methodName = descriptor.substring(methodStart, paramStart);

            // 提取方法描述符（从(开始的部分）
            result.methodDesc = descriptor.substring(paramStart);
        } else {
            // 如果不是标准方法描述符格式，保持原始值
            result.className = descriptor;
        }

        return result;
    }
    public static ParsedField parseFieldTarget(String descriptor) {
        ParsedField result = new ParsedField();

        // 查找字段名的分隔符(:)
        int colonPos = descriptor.indexOf(':');
        if (colonPos == -1) {
            throw new IllegalArgumentException("Invalid field descriptor format, missing ':'");
        }

        // 提取owner和字段名部分（:之前的部分）
        String ownerAndName = descriptor.substring(0, colonPos);

        // 提取owner（最后一个;之前的部分）
        int lastSemicolon = ownerAndName.lastIndexOf(';');
        if (lastSemicolon == -1) {
            throw new IllegalArgumentException("Invalid owner format, missing ';'");
        }

        result.owner = Type.getType(ownerAndName.substring(0, lastSemicolon + 1)).getInternalName();
        result.name = ownerAndName.substring(lastSemicolon + 1);
        result.desc = descriptor.substring(colonPos + 1);

        return result;
    }

}