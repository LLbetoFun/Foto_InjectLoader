package com.fun.inject.transform.api.mixin.operation.impl;

import com.fun.inject.mapper.Mapper;
import com.fun.inject.transform.api.mixin.annotations.Mixin;
import com.fun.inject.transform.api.mixin.annotations.Overwrite;
import com.fun.inject.transform.api.mixin.operation.Operation;
import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.MethodNode;
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
               methodNode.instructions=replacement.instructions; //match method
        }
    }
}
