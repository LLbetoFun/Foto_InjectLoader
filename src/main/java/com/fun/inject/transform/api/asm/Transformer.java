package com.fun.inject.transform.api.asm;


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
    public Transformer setTarget(Class<?> clazz) {
//        this.name = Mapper.getMappedClass(Type.getInternalName(clazz));
//        this.obfName = Type.getInternalName(clazz);
//
//        this.clazz = clazz;
        return setTarget(Mapper.getMappedClass(Type.getInternalName(clazz)));
        //System.out.println("Transformer " + clazz.getName() + " loaded");

    }
    public Transformer setTarget(String name){
        name = name.replace('.', '/');
        this.name = name;
        obfName = Mapper.getMappedClass(name);
        try {
            clazz = Bootstrap.findClass(obfName);
            //oldBytes = InjectUtils.getClassBytes(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
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
