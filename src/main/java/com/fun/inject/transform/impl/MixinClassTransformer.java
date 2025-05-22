package com.fun.inject.transform.impl;

import com.fun.inject.transform.IClassTransformer;
import com.fun.inject.transform.api.mixin.Mixins;

import java.security.ProtectionDomain;

public class MixinClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return Mixins.transform(classBeingRedefined,classfileBuffer);
    }
}
