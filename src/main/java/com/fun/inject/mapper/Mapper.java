package com.fun.inject.mapper;

import com.fun.inject.Bootstrap;
import com.fun.inject.version.MinecraftType;
import com.fun.inject.version.MinecraftVersion;
import com.fun.inject.utils.FishClassWriter;
import com.fun.utils.file.IOUtils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class Mapper {
    public static final String[] obfibleClasses = new String[]{"com/mojang", "net/minecraft"};
    public static HashMap<String, String> classMap = new HashMap<>();
    public static HashMap<String, String> methodMap = new HashMap<>();
    public static HashMap<String, String> fieldMap = new HashMap<>();

    public static byte[] getAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();

        return buffer.toByteArray();
    }

    public static String getMappedClass(Class<?> c) {
        return getMappedClass(Type.getInternalName(c));
    }

    public static File mapJar(File jarIn,MinecraftVersion mcVersion, MinecraftType mcType) {
        //if(mcType==MinecraftType.NONE)return jarIn;
        File destFile = new File(new File(jarIn.getParent()).getParent(), String.format("%s_%s",jarIn.getName(), mcType.getType()));
        try (JarFile jar = new JarFile(jarIn)) {
            if (destFile.exists()) {
                destFile.delete();
            }
            destFile.createNewFile();
            destFile.deleteOnExit();
            //JarFile target=new JarFile(destFile);
            JarOutputStream jos = new JarOutputStream(Files.newOutputStream(destFile.toPath()));
            try {
                readMappings(jarIn.getAbsolutePath(), mcType);
            }
            catch (RemapException e){
                readMappings(mcVersion,mcType);
            }
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    InputStream is = jar.getInputStream(entry);
                    byte[] b = mapBytes(getAllBytes(is), mcType);
                    getAllBytes(is);
                    jos.putNextEntry(new ZipEntry(entry.getName()));
                    jos.write(b);
                    jos.closeEntry();

                    //System.out.println(entry.getName());
                }
            }
            //target.close();
            jos.close();
            return destFile;

        } catch (IOException | RemapException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static boolean isMethodDesc(String desc) {
        return (desc.contains("(") && desc.contains(")"));
    }

    public static String map(String mcpName, String owner, String desc) {
        if (isMethodDesc(desc)) {
            return getMappedMethod(mcpName, owner, desc);
        }
        return getMappedField(mcpName, owner);
    }

    public static ClassReader getClassReader(String className) {
        byte[] bytes = Bootstrap.classes.get(className.replace('/', '.'));
        if (bytes == null) {
            return null;
        }
        return new ClassReader(bytes);
    }

    public static byte[] mapBytes(byte[] bytes, MinecraftType mT) {

        ClassNode classNode = null;
        String desc = null;
        try {
            classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            ArrayList<MethodNode> removeMethods = new ArrayList<>();
            ArrayList<FieldNode> removeField = new ArrayList<>();
            //System.out.println("start remap: "+classNode.name);
            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.visibleAnnotations != null) {
                    for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
                        if (annotationNode.desc.equals("Lcom/fun/inject/mapper/SideOnly;")) {
                            for (Object object : annotationNode.values) {
                                if (object instanceof String[]) {
                                    for (String s : (String[]) object) {
                                        if ((s.equals("AGENT") && !Bootstrap.isRemote)
                                                || (s.equals("INJECTOR") && Bootstrap.isRemote)) {
                                            removeMethods.add(methodNode);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (mT == MinecraftType.NONE) continue;
                try {
                    String s = getMappedMethod(methodNode.name, Bootstrap.findClass(getMappedClass(classNode.superName)), methodNode.desc);
                    if (s == null) {
                        for (String itfs : classNode.interfaces) {
                            s = getMappedMethod(methodNode.name, Bootstrap.findClass(getMappedClass(itfs)), methodNode.desc);
                            if (s != null) break;
                        }
                    }
                    methodNode.name = s == null ? methodNode.name : s;
                } catch (ClassNotFoundException e) {

                }
                methodNode.desc = getMappedMethodDesc(methodNode.desc);
                for (AbstractInsnNode insnNode : methodNode.instructions) {
                    if (insnNode instanceof MethodInsnNode) {
                        if (isMapibleClass(((MethodInsnNode) insnNode).owner)) {
                            Class<?> owner = Bootstrap.findClass(getMappedClass(((MethodInsnNode) insnNode).owner));

                            ((MethodInsnNode) insnNode).name = getMappedMethod(((MethodInsnNode) insnNode).name,
                                    owner, ((MethodInsnNode) insnNode).desc);
                            ((MethodInsnNode) insnNode).owner = getMappedClass(((MethodInsnNode) insnNode).owner);

                        }
                        ((MethodInsnNode) insnNode).desc = getMappedMethodDesc(((MethodInsnNode) insnNode).desc);
                        desc = ((MethodInsnNode) insnNode).desc;
                    }
                    if (insnNode instanceof FieldInsnNode) {
                        if (isMapibleClass(((FieldInsnNode) insnNode).owner)) {
                            Class<?> owner = Bootstrap.findClass(getMappedClass(((FieldInsnNode) insnNode).owner));
                            ((FieldInsnNode) insnNode).name = getMappedField(((FieldInsnNode) insnNode).name, owner);
                            ((FieldInsnNode) insnNode).owner = getMappedClass(((FieldInsnNode) insnNode).owner);
                        }
                        ((FieldInsnNode) insnNode).desc = getMappedFieldDesc(((FieldInsnNode) insnNode).desc);

                    }
                    if (insnNode instanceof TypeInsnNode) {
                        ((TypeInsnNode) insnNode).desc = getMappedClass(((TypeInsnNode) insnNode).desc);
                    }
                    if (insnNode instanceof LdcInsnNode) {
                        if (((LdcInsnNode) insnNode).cst instanceof Type) {
                            ((LdcInsnNode) insnNode).cst = Type.getType(getMappedFieldDesc(((Type) ((LdcInsnNode) insnNode).cst).getDescriptor()));
                        }
                        //System.out.println(((LdcInsnNode) insnNode).cst+" "+((LdcInsnNode) insnNode).cst.getClass().getName());

                    }
                    if (insnNode instanceof InvokeDynamicInsnNode) {
                        ((InvokeDynamicInsnNode) insnNode).desc = getMappedMethodDesc(((InvokeDynamicInsnNode) insnNode).desc);
                        Object[] bsmArgs = ((InvokeDynamicInsnNode) insnNode).bsmArgs;
                        for (int i = 0, bsmArgsLength = bsmArgs.length; i < bsmArgsLength; i++) {
                            Object a = bsmArgs[i];
                            if (a instanceof Type) {
                                Type b = Type.getType(getMappedDesc(((Type) a).getDescriptor()));
                                bsmArgs[i] = b;
                                //System.out.println("type:"+b);//bsmArgs[i]=Type.getType(get)
                            }
                            if (a instanceof Handle) {
                                Handle b = new Handle(((Handle) a).getTag()
                                        , getMappedClass(((Handle) a).getOwner()),
                                        map(((Handle) a).getName(), ((Handle) a).getOwner(), ((Handle) a).getDesc()),
                                        getMappedDesc(((Handle) a).getDesc()),
                                        ((Handle) a).isInterface());
                                bsmArgs[i] = b;
                                //System.out.println("handler:"+ ((Handle) a).getOwner()+" "+ ((Handle) a).getName()+" "+ ((Handle) a).getDesc());
                            }
                            //todo
                        }
                        //System.out.println("____"+((InvokeDynamicInsnNode) insnNode).name+" "+((InvokeDynamicInsnNode) insnNode).desc);
                    }
                }
            }
            for (FieldNode fieldNode : classNode.fields) {
                if (fieldNode.visibleAnnotations != null) {
                    for (AnnotationNode annotationNode : fieldNode.visibleAnnotations) {
                        if (annotationNode.desc.equals("Lcom/fun/inject/mapper/SideOnly;")) {
                            for (Object object : annotationNode.values) {
                                if (object instanceof String[]) {
                                    for (String s : (String[]) object) {
                                        if ((s.equals("AGENT") && !Bootstrap.isRemote)
                                                || (s.equals("INJECTOR") && Bootstrap.isRemote)) {
                                            removeField.add(fieldNode);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (mT == MinecraftType.NONE) continue;
                /*try {
                    fieldNode.name = getObfField(fieldNode.name, Bootstrap.findClass(getMappedClass(classNode.superName)));
                }
                catch (ClassNotFoundException e) {//NERVER FIXME

                }*/
                fieldNode.desc = getMappedFieldDesc(fieldNode.desc);
                //for(){
                //  fieldNode.signature
                //}
            }
            classNode.methods.removeAll(removeMethods);
            classNode.fields.removeAll(removeField);
            if (mT != MinecraftType.NONE) {
                classNode.name = getMappedClass(classNode.name);
                classNode.superName = getMappedClass(classNode.superName);
            }
            FishClassWriter writer = new FishClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            //System.out.println(classNode.name);
            return writer.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(classNode.name + " " + desc + "|");
        }
        return new byte[0];
    }

    public static String getMappedClass(String mcpName) {
        boolean isArray = mcpName.contains("[]");
        String cn = mcpName.endsWith("[]") ? mcpName.substring(0, mcpName.length() - 2) : mcpName;
        String t = classMap.get(cn);
        return t == null ? mcpName : t + (isArray ? "[]" : "");
    }

    public static String getMappedMethod(String mcpName, String owner, String desc) {
        String s = getMappedMethodOrNull(mcpName, owner, desc);
        return s == null ? mcpName : s;
    }

    public static boolean isMapibleClass(String name) {
        name = name.replace('.', '/');
        for (String cname : obfibleClasses) {
            if (name.startsWith(cname)) {
                return true;
            }
        }
        return false;
    }

    public static String getMappedMethodOrNull(String mcpName, String owner, String desc) {
        owner = owner.replace('.', '/');
        String str = methodMap.get(owner + "/" + mcpName + " " + desc);
        if (str == null) {
            return null;
        }
        String[] s1 = str.split(" ")[0].split("/");
        return s1[s1.length - 1];
    }

    public static String getMappedMethod(String mcpName, Class<?> owner, String desc) {
        String s;
        List<Class<?>> classes = getSupers(owner);
        for (Class<?> c : classes) {
            s = getMappedMethodOrNull(mcpName, getMappedClass(c.getName()), desc);
            if (s != null) return s;
        }
        return mcpName;
    }

    public static String getMappedMethodOrNull(String mcpName, Class<?> owner, String desc) {
        String s;
        List<Class<?>> classes = getSupers(owner);
        for (Class<?> c : classes) {
            s = getMappedMethodOrNull(mcpName, getMappedClass(c.getName()), desc);
            if (s != null) return s;
        }
        return null;
    }

    public static List<Class<?>> getSupers(Class<?> theClass) {
        List<Class<?>> classes = new ArrayList<>();
        traverseSupers(theClass, classes);
        return classes;
    }

    private static void traverseSupers(Class<?> clz, List<Class<?>> result) {
        if (clz == null || clz == Object.class || result.contains(clz)) return;
        result.add(clz);
        traverseSupers(clz.getSuperclass(), result);
        for (Class<?> anInterface : clz.getInterfaces())
            traverseSupers(anInterface, result);
    }

    public static String getMappedField(String mcpName, Class<?> owner) {
        String s;
        List<Class<?>> classes = getSupers(owner);
        for (Class<?> c : classes) {
            s = getMappedFieldOrNull(mcpName, getMappedClass(c.getName()));
            if (s != null) return s;
        }
        return mcpName;
    }

    public static String getMappedField(String mcpName, String owner) {
        String s = getMappedFieldOrNull(mcpName, owner);
        return s == null ? mcpName : s;
    }

    public static String getMappedFieldOrNull(String mcpName, String owner) {
        String str = fieldMap.get(owner + "/" + mcpName);
        if (str == null) {
            return null;
        }

        String[] s = str.split("/");
        return s[s.length - 1];
    }

    public static String getMappedDesc(String desc) {
        if (isMethodDesc(desc)) {
            return getMappedMethodDesc(desc);
        }
        return getMappedFieldDesc(desc);
    }

    public static String getMappedMethodDesc(String desc) {
        org.objectweb.asm.Type[] args = org.objectweb.asm.Type.getArgumentTypes(desc);
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (org.objectweb.asm.Type t : args) {
            if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
                sb.append("L");
                sb.append(getMappedClass(t.getInternalName()));
                sb.append(";");
            } else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == org.objectweb.asm.Type.OBJECT) {
                sb.append("[L");
                sb.append(getMappedClass(t.getElementType().getInternalName()));
                sb.append(";");

            } else {
                sb.append(t.getDescriptor());
            }
        }
        sb.append(")");
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getReturnType(desc);
        if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
            sb.append("L");
            sb.append(getMappedClass(t.getInternalName()));
            sb.append(";");
        } else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == org.objectweb.asm.Type.OBJECT) {
            sb.append("[L");
            sb.append(getMappedClass(t.getElementType().getInternalName()));
            sb.append(";");

        } else {
            sb.append(t.getDescriptor());
        }
        return sb.toString();
    }

    public static String getMappedFieldDesc(String desc) {
        StringBuilder sb = new StringBuilder();
        Type t = Type.getType(desc);
        if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
            sb.append("L");
            sb.append(getMappedClass(t.getInternalName()));
            sb.append(";");
        } else if (t.getSort() == Type.ARRAY && t.getElementType().getSort() == org.objectweb.asm.Type.OBJECT) {
            sb.append("[L");
            sb.append(getMappedClass(t.getElementType().getInternalName()));
            sb.append(";");

        } else {
            sb.append(t.getDescriptor());
        }
        return sb.toString();
    }

    public static void readMappings(String jarPath, MinecraftType mcType) throws RemapException {
        try (JarFile jar = new JarFile(jarPath)) {
            InputStream f = null;
            if (mcType == MinecraftType.VANILLA) {
                f = IOUtils.getEntryFromJar(jar, "mappings/vanilla.srg");
            }
            if (mcType == MinecraftType.FORGE) {
                f = IOUtils.getEntryFromJar(jar, "mappings/forge.srg");
            }
            if (f != null) {
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(f, StandardCharsets.UTF_8));
                String line = "";
                while ((line = bufferedreader.readLine()) != null) {
                    String[] parts = line.substring(4)
                            .split(" ");
                    if (line.startsWith("CL")) {
                        classMap.put(parts[1], parts[0]);
                        classMap.put(parts[0], parts[1]);

                    }
                    if (line.startsWith("FD")) {
                        fieldMap.put(parts[1], parts[0]);
                        fieldMap.put(parts[0], parts[1]);
                    }
                    if (line.startsWith("MD")) {
                        methodMap.put(parts[2] + " " + parts[3], parts[0] + " " + parts[1]);
                        methodMap.put(parts[0] + " " + parts[1], parts[2] + " " + parts[3]);
                    }

                }
            }
            else {
                throw new RemapException("Can't Read Mappings from Injection Jar: " + jarPath);
            }

        } catch (Exception e) {
            throw new RemapException("Can't Read Mappings from Injection Jar: " + jarPath);
        }


    }
    public static void readMappings(MinecraftVersion version, MinecraftType mcType) throws RemapException {
        try (JarFile jar = new JarFile(Bootstrap.jarPath)) {
            InputStream f = null;
            if (mcType == MinecraftType.VANILLA) {
                f = IOUtils.getEntryFromJar(jar, String.format("mappings/%s/vanilla.srg",version.getVer()));
            }
            if (mcType == MinecraftType.FORGE) {
                f = IOUtils.getEntryFromJar(jar, String.format("mappings/%s/forge.srg",version.getVer()));
            }
            if (mcType == MinecraftType.FABRIC) {
                f = IOUtils.getEntryFromJar(jar, String.format("mappings/%s/fabric.srg",version.getVer()));
            }
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(f, StandardCharsets.UTF_8));
            String line = "";
            while ((line = bufferedreader.readLine()) != null) {
                String[] parts = line.substring(4)
                        .split(" ");
                if (line.startsWith("CL")) {
                    classMap.put(parts[1], parts[0]);
                    classMap.put(parts[0], parts[1]);
                }
                if (line.startsWith("FD")) {
                    fieldMap.put(parts[1], parts[0]);
                    fieldMap.put(parts[0], parts[1]);
                }
                if (line.startsWith("MD")) {
                    methodMap.put(parts[2] + " " + parts[3], parts[0] + " " + parts[1]);
                    methodMap.put(parts[0] + " " + parts[1], parts[2] + " " + parts[3]);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RemapException(String.format("Can't Read Mapping from injector jar (%s %s).",version.getVer(),mcType.getType()));
        }


    }


}
