package com.fun.inject.transform.api;


import com.fun.inject.Bootstrap;
import com.fun.inject.mapper.Mapper;
import org.objectweb.asm.Type;

public class Transformer {

    public String name, obfName;
    public byte[] oldBytes;
    public byte[] newBytes;

    public Class<?> clazz;

    public Transformer() {
        super();
    }
    private Transformer(String name) {
        name = name.replace('.', '/');
        this.name = name;
        obfName = Mapper.getMappedClass(name);
        if (obfName != null) {
            try {
                clazz = Bootstrap.findClass(obfName);
                //oldBytes = InjectUtils.getClassBytes(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Transformer(Class<?> clazz) {
//        this.name = Mapper.getMappedClass(Type.getInternalName(clazz));
//        this.obfName = Type.getInternalName(clazz);
//
//        this.clazz = clazz;
        this(Mapper.getMappedClass(Type.getInternalName(clazz)));
    }

    public byte[] getOldBytes() {
        return oldBytes;
    }

    public String getName() {
        return name;
    }

    public String getObfName() {
        return obfName;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
