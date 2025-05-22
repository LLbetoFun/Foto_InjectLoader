package com.fun.inject.transform.api.mixin.operation;

import org.objectweb.asm.tree.MethodNode;

public interface Operation {
    void dispose(MethodNode methodNode);
}
