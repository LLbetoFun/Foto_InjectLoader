package com.fun.inject.transform.api.mixin;

import com.fun.inject.transform.api.mixin.annotations.Mixin;
import com.fun.inject.utils.NativeUtils;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Mixins {
    private static final List<MixinTransformer> transformers = new ArrayList<MixinTransformer>();
    public static void addMixin(ClassNode classNode, Mixin mixin) {
        transformers.add(new MixinTransformer(classNode, mixin));
    }
    public static void callRetransform(){
        for (MixinTransformer transformer : transformers) {
            NativeUtils.retransformClass(transformer.getTargetClass());
        }
    }
    public static byte[] transform(Class<?> clazz,byte[] bytes) {
        MixinTransformer matched =  transformers.stream().filter(transformer -> transformer.getTargetClass().equals(clazz)).findAny().orElse(null);
        if(matched == null) return null;
        return matched.transform(bytes);
    }
    public static void destroy(){
        transformers.forEach((transformer) -> {
            NativeUtils.redefineClass(transformer.getTargetClass(),transformer.getOldBytes());
        });
    }
}
