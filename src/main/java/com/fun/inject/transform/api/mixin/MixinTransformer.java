package com.fun.inject.transform.api.mixin;

import com.fun.inject.transform.api.asm.Transformers;
import com.fun.inject.transform.api.mixin.annotations.Inject;
import com.fun.inject.transform.api.mixin.annotations.Mixin;
import com.fun.inject.transform.api.mixin.annotations.Overwrite;
import com.fun.inject.transform.api.mixin.operation.Operation;
import com.fun.inject.transform.api.mixin.operation.impl.InjectOperation;
import com.fun.inject.transform.api.mixin.operation.impl.OverwriteOperation;
import com.fun.utils.asm.ASMUtils;
import lombok.Getter;
import org.apache.commons.compress.utils.Lists;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

@Getter
public class MixinTransformer {
    private final ClassNode classNode;
    private final Mixin mixin;
    private final Class<?> targetClass;
    private final List<Operation> operations= Lists.newArrayList();
    private byte[] oldBytes;
    public MixinTransformer(ClassNode classNode, Mixin mixin) {
        this.classNode = classNode;
        this.mixin = mixin;
        this.targetClass = mixin.value();
        parseOperations();
    }
    public void addOperation(Operation operation) {
        operations.add(operation);
    }
    public byte[] transform(byte[] bytes) {
        oldBytes = bytes;
        ClassNode node = Transformers.node(bytes);
        node.methods.forEach(methodNode ->
                operations.forEach(operation ->
                        operation.dispose(methodNode)));
        return Transformers.rewriteClass(node);
    }
    public void parseOperations() {
        classNode.methods.forEach(methodNode -> {
            AnnotationNode inject = ASMUtils.getAnnotationNode(methodNode, Inject.class);
            AnnotationNode overwrite = ASMUtils.getAnnotationNode(methodNode, Overwrite.class);
            if(overwrite != null)addOperation(new OverwriteOperation(Overwrite.Helper.fromNode(overwrite),mixin,methodNode));
            if(inject != null) addOperation(new InjectOperation(Inject.Helper.fromNode(inject),mixin,methodNode));
        });
    }
}
