package com.fun.inject.transform.api.mixin.operation.impl;

import com.fun.inject.mapper.Mapper;
import com.fun.inject.transform.api.mixin.annotations.Mixin;
import com.fun.inject.transform.api.mixin.annotations.Overwrite;
import com.fun.inject.transform.api.mixin.operation.Operation;
import com.fun.utils.asm.ASMUtils;
import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OverwriteOperation implements Operation {
    private final Overwrite annotation;
    private final Mixin mixin;
    private final MethodNode replacement;
    @Override
    public void dispose(MethodNode methodNode) {
        if(methodNode.desc.equals(Mapper.getMappedMethodDesc(annotation.desc()))
            &&methodNode.name.equals(Mapper.getMappedMethod(
                annotation.method(),
                Mapper.getMappedClass(mixin.value()),
                annotation.desc()
        ))){
               methodNode.instructions.clear();
               methodNode.instructions.add(ASMUtils.cloneInsnList(replacement.instructions));
               methodNode.visitMaxs(0, 0);
        }
    }
}
