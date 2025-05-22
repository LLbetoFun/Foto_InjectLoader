package com.fun.utils.asm;

import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.LabelNode;

@AllArgsConstructor
public class Block {
    public LabelNode start,end;
}
