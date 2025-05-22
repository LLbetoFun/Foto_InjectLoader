package com.fun.inject.utils;


import com.fun.inject.transform.impl.ASMClassTransformer;
import com.fun.inject.transform.IClassTransformer;

public class Native {
    public Class<?> nativeUtils;

    public Native(Class<?> nativeUtilsClazz) {
        nativeUtils = nativeUtilsClazz;
    }

    public Native() {
    }


    public void addTransformer(IClassTransformer transformer) {
        NativeUtils.transformers.add(transformer);
    }


    public void retransformClasses(Class<?>... classes) {
        for (Class<?> kls : classes)
            NativeUtils.retransformClass0(kls);
    }


    public Class<?>[] getAllLoadedClasses() {
        return NativeUtils.getAllLoadedClasses().toArray(new Class[0]);
    }


    public void redefineClass(Class<?> clazz, byte[] bytes) {
        //ReflectionUtils.invokeMethod(nativeUtils,"redefineClass",new Class[]{Class.class,byte[].class},clazz,bytes);
        NativeUtils.redefineClass(clazz, bytes);
    }

    public void doneTransform() {
        NativeUtils.doneTransform();
    }

    public void removeTransformer(IClassTransformer transformer) {
        NativeUtils.transformers.remove(transformer);
    }
}
