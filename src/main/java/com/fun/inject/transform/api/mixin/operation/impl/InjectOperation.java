package com.fun.inject.transform.api.mixin.operation.impl;

import com.fun.inject.mapper.Mapper;
import com.fun.inject.transform.api.mixin.annotations.*;
import com.fun.inject.transform.api.mixin.operation.Operation;
import com.fun.utils.asm.ASMUtils;
import com.fun.utils.asm.Block;
import com.fun.utils.asm.MethodNodeDisassembler;
import com.fun.utils.asm.MixinTargetParser;
import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class InjectOperation implements Operation {
    private final Inject info;
    private final Mixin mixin;
    private final MethodNode targetMethod;
    private final Map<Integer,Integer> localMap = new HashMap<>();
    private void removeLastRet(){
        if ((targetMethod.desc.endsWith("V") || info.deleteLastReturn()) && targetMethod.instructions.size() > 0) {
            if (targetMethod.instructions.get(targetMethod.instructions.size() - 1) instanceof LabelNode)
                targetMethod.instructions.remove(targetMethod.instructions.get(targetMethod.instructions.size() - 1));
            while (targetMethod.instructions.size() > 0 &&
                    !(targetMethod.instructions.get(targetMethod.instructions.size() - 1) instanceof LabelNode))
                targetMethod.instructions.remove(targetMethod.instructions.get(targetMethod.instructions.size() - 1));
        }
    }
    private void processLocals(){
        if(targetMethod.visibleParameterAnnotations!=null) {
            List<AnnotationNode>[] visibleParameterAnnotations = targetMethod.visibleParameterAnnotations;
            for (int i = 0; i < visibleParameterAnnotations.length; i++) {
                List<AnnotationNode> nodes = visibleParameterAnnotations[i];
                final int index = i + (ASMUtils.isStaticMethod(targetMethod)? 0 : 1);
                nodes.stream()
                        .filter(annotationNode -> annotationNode.desc.contains(ASMUtils.slash(Local.class.getName())))
                        .forEach(annotationNode -> {
                            Local local = Local.Builder.fromAnnotation(annotationNode);
                            localMap.put(index, local.index());
                        });
            }
        }

        final int maxLocalIndex = targetMethod.maxLocals;
        Arrays.stream(targetMethod.instructions.toArray())
                .filter(node -> ASMUtils.isStoreOpe(node.getOpcode())
                        && node instanceof VarInsnNode
                        && !localMap.containsKey(((VarInsnNode) node).var))
                .forEach(storeNode -> localMap.put(((VarInsnNode) storeNode).var, ((VarInsnNode) storeNode).var+maxLocalIndex));
        targetMethod.instructions.forEach(insnNode -> {
            if(insnNode instanceof VarInsnNode)
                ((VarInsnNode) insnNode).var = localMap.getOrDefault(((VarInsnNode) insnNode).var,((VarInsnNode) insnNode).var);
        });
    }
    private void insert(MethodNode sourceMethod){
        InsnList sourceList = sourceMethod.instructions;
        InsnList targetList = ASMUtils.cloneInsnList(targetMethod.instructions);
        AbstractInsnNode insertPoint = getInsertPoint(sourceList);

        Block targetBlock = ASMUtils.getBlock(insertPoint,sourceList);

        At.Shift shift = info.at().shift();

        if(shift == At.Shift.BEFORE)
            sourceList.insert(targetBlock.start,targetList);
        else sourceList.insert(targetBlock.end,targetList);
    }
    private AbstractInsnNode getInsertPoint(InsnList instructions) {
        At targetInfo = info.at();
        switch (targetInfo.value()) {
            case "STRING" -> {
                for (AbstractInsnNode instruction : instructions) {
                    if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof String && targetInfo.target().equals(((LdcInsnNode) instruction).cst)) {
                        return instruction;
                    }
                }
                return null;
            }
            case "HEAD" -> {
                return instructions.get(0);
            }
            case "RETURN" -> {
                return Arrays.stream(instructions.toArray())
                        .filter(node -> ASMUtils.isRetOpe(node.getOpcode())&&node instanceof InsnNode)
                        .findFirst().orElse(null);

            }
            case "INVOKE" -> {
                String target = info.at().target();
                MixinTargetParser.ParsedMethod parsedMethod = MixinTargetParser.parseMethodTarget(target);

                return Arrays.stream(instructions.toArray())
                        .filter((node) -> node instanceof MethodInsnNode)
                        .filter((node)-> ((MethodInsnNode) node).owner.equals(Mapper.getMappedClass(parsedMethod.className)))
                        .filter((node)->((MethodInsnNode) node).name.equals(Mapper.getMappedMethod(parsedMethod.methodName,parsedMethod.className,parsedMethod.methodDesc)))
                        .filter((node)->((MethodInsnNode) node).desc.equals(Mapper.getMappedMethodDesc(parsedMethod.methodDesc)))
                        .findFirst().orElse(null);
            }
            case "FIELD" -> {
                String target = info.at().target();
                MixinTargetParser.ParsedField parsedField = MixinTargetParser.parseFieldTarget(target);

                return Arrays.stream(instructions.toArray())
                        .filter((node) -> node instanceof FieldInsnNode)
                        .filter((node -> ((FieldInsnNode) node).owner.equals(Mapper.getMappedClass(parsedField.owner))))
                        .filter((node -> ((FieldInsnNode) node).name.equals(Mapper.getMappedField(parsedField.name,parsedField.owner))))
                        .filter((node) -> ((FieldInsnNode) node).desc.equals(Mapper.getMappedFieldDesc(parsedField.desc)))
                        .findFirst().orElse(null);
            }

        }
        return null;
    }
    @Override
    public void dispose(MethodNode methodNode) {
        if(methodNode.desc.equals(Mapper.getMappedMethodDesc(info.desc()))
                &&methodNode.name.equals(Mapper.getMappedMethod(
                info.method(),
                Mapper.getMappedClass(mixin.value()),
                info.desc()
        )))
        {
            removeLastRet();
            processLocals();

            insert(methodNode);
            //MethodNodeDisassembler.printMethodInstructions(methodNode);
        }
    }
}
