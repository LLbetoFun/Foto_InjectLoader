package com.fun.utils.asm;

import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

@AllArgsConstructor
public class Block {
    public AbstractInsnNode start,end;
}
