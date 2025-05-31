package com.fun.inject.transform.impl;

import com.fun.inject.Main;
import com.fun.inject.transform.IClassTransformer;
import com.fun.inject.transform.api.mixin.Mixins;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.ProtectionDomain;

public class MixinClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        byte[] modifiedClassfileBuffer = Mixins.transform(classBeingRedefined,classfileBuffer);;
        if(modifiedClassfileBuffer != null) {
            try {
                FileUtils.writeByteArrayToFile(new File(Main.workDir,"caches/"+className+"_old.class"), classfileBuffer.clone());

                FileUtils.writeByteArrayToFile(new File(Main.workDir,"caches/"+className+".class"), modifiedClassfileBuffer.clone());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return modifiedClassfileBuffer;
    }
}
