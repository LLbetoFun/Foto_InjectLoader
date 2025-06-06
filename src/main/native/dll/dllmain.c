//
// Created by admin on 2024/4/7.
//
#include <windows.h>
#include "../java/jni.h"
#include "utils.h"
#include "../java/jvmti.h"
#include <stdio.h>



#define true 1
#define bool int
#define false 0
#define NEW_STR_SIZE 1024

extern BYTE OldCode[12] = { 0x00 };
extern BYTE HookCode[12] = { 0x48, 0xB8, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0x90, 0xFF, 0xE0 };

void HookFunction64(char* lpModule, LPCSTR lpFuncName, LPVOID lpFunction)
{
    DWORD_PTR FuncAddress = (UINT64)GetProcAddress(GetModuleHandle(lpModule), lpFuncName);
    DWORD OldProtect = 0;
    //MessageBoxA(NULL,"1","Fish",0);
    if (VirtualProtect((LPVOID)FuncAddress, 12, PAGE_EXECUTE_READWRITE, &OldProtect))
    {
        //MessageBoxA(NULL,"2","Fish",0);

        memcpy(OldCode, (LPVOID)FuncAddress, 12);                   // 拷贝原始机器码指令
        *(PINT64)(HookCode + 2) = (UINT64)lpFunction;               // 填充90为指定跳转地址
    }
    memcpy((LPVOID)FuncAddress, &HookCode, sizeof(HookCode));
    //MessageBoxA(NULL,"3","Fish",0);
    // 拷贝Hook机器指令
    VirtualProtect((LPVOID)FuncAddress, 12, OldProtect, &OldProtect);
}
void UnHookFunction64(char* lpModule, LPCSTR lpFuncName)
{
    DWORD OldProtect = 0;
    UINT64 FuncAddress = (UINT64)GetProcAddress(GetModuleHandleA(lpModule), lpFuncName);
    if (VirtualProtect((LPVOID)FuncAddress, 12, PAGE_EXECUTE_READWRITE, &OldProtect))
    {
        memcpy((LPVOID)FuncAddress, OldCode, sizeof(OldCode));
    }
    VirtualProtect((LPVOID)FuncAddress, 12, OldProtect, &OldProtect);
}
void HookFunctionAdress64(LPVOID FuncAddress, LPVOID lpFunction)
{
    DWORD OldProtect = 0;
    //MessageBoxA(NULL,"1","Fish",0);

    if (VirtualProtect((LPVOID)FuncAddress, 12, PAGE_EXECUTE_READWRITE, &OldProtect))
    {
        //MessageBoxA(NULL,"2","Fish",0);

        memcpy(OldCode, (LPVOID)FuncAddress, 12);                   // 拷贝原始机器码指令
        *(PINT64)(HookCode + 2) = (UINT64)lpFunction;               // 填充90为指定跳转地址
    }
    memcpy((LPVOID)FuncAddress, &HookCode, sizeof(HookCode));
    //MessageBoxA(NULL,"3","Fish",0);
    // 拷贝Hook机器指令
    VirtualProtect((LPVOID)FuncAddress, 12, OldProtect, &OldProtect);
}
void UnHookFunctionAdress64(LPVOID FuncAddress)
{
    DWORD OldProtect = 0;
    if (VirtualProtect((LPVOID)FuncAddress, 12, PAGE_EXECUTE_READWRITE, &OldProtect))
    {
        memcpy((LPVOID)FuncAddress, OldCode, sizeof(OldCode));
    }
    VirtualProtect((LPVOID)FuncAddress, 12, OldProtect, &OldProtect);
}
extern void JNICALL unHookGetClass(JNIEnv *env);
extern void JNICALL hookGetClass(JNIEnv *env);
/*int WINAPI MyMessageBoxW(HWND hWnd, LPCTSTR lpText, LPCTSTR lpCaption, UINT uType)
{
    UnHookFunction64(L"user32.dll", "MessageBoxW");
    int ret = MessageBoxW(0, L"hello lyshark", lpCaption, uType);
    HookFunction64(L"user32.dll", "MessageBoxW", (PROC)MyMessageBoxW);
    return ret;
}
bool APIENTRY DllMain(HANDLE handle, DWORD dword, LPVOID lpvoid)
{
    switch (dword)
    {
        case DLL_PROCESS_ATTACH:
            HookFunction64(L"user32.dll", "MessageBoxW", (PROC)MyMessageBoxW);
            break;
        case DLL_PROCESS_DETACH:
        case DLL_THREAD_DETACH:
            break;
    }
    return true;
}*/







 typedef struct {
    JavaVM *vm;
    JNIEnv *jniEnv;
    jvmtiEnv *jvmtiEnv;
} JAVA;
static JAVA *Java;

static char* newPackage="com.fun";
JNIEXPORT void JNICALL gc(JNIEnv *env) {
    // 调用Java的System.gc()方法
    jclass systemClass = (*env)->FindClass(env, "java/lang/System");
    jmethodID gcMethod = (*env)->GetStaticMethodID(env, systemClass, "gc", "()V");
    (*env)->CallStaticVoidMethod(env, systemClass, gcMethod);
}
void *__cdecl safe_malloc(size_t _Size){
    void* p =malloc(_Size);
    if(p!=NULL){
        return p;

    }
    else{
        for(int i=0;i<100;i++){

            p =malloc(_Size);
            if(p!=NULL){
                return p;
            }
            Sleep(10);
            MessageBoxW(NULL,L"内存申请失败，尝试重新申请",L"Foto",0);

        }
    }
    MessageBoxW(NULL,L"内存申请失败，8g内存玩什么外挂?",L"Foto",0);
    return p;

}
static bool isInjecting=false;
char* replace(const char* source, const char* old, const char* new) {
    int source_len = strlen(source);
    int old_len = strlen(old);
    int new_len = strlen(new);
    int count = 0;

    // 首先计算需要替换的次数
    for (int i = 0; i <= source_len - old_len; i++) {
        if (strncmp(&source[i], old, old_len) == 0) {
            count++;
            i += old_len - 1; // 跳过已检查的部分
        }
    }

    // 计算新字符串的长度
    int new_str_len = source_len + count * (new_len - old_len);
    char* new_str = (char*)safe_malloc(new_str_len + 1); // +1 为字符串结束符'\0'

    if (new_str == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        return NULL;
    }

    int j = 0; // 新字符串的索引
    for (int i = 0; i < source_len; i++) {
        // 检查是否需要替换
        if (strncmp(&source[i], old, old_len) == 0) {
            strcpy(&new_str[j], new); // 复制新子串
            j += new_len; // 移动到新字符串的下一个位置
            i += old_len - 1; // 跳过已检查的部分
        } else {
            new_str[j++] = source[i];
        }
    }
    new_str[new_str_len] = '\0'; // 确保字符串正确结束

    return new_str;
}


jclass findThreadClass(JNIEnv *jniEnv, const char *name, jobject thread) {
    jclass Thread = (*jniEnv)->GetObjectClass(jniEnv, thread);
    jclass URLClassLoader = (*jniEnv)->FindClass(jniEnv, "java/net/URLClassLoader");
    jclass ClassLoader = (*jniEnv)->FindClass(jniEnv, "java/lang/ClassLoader");

    jmethodID getContextClassLoader = (*jniEnv)->GetMethodID(jniEnv, Thread, "getContextClassLoader",
                                                                       "()Ljava/lang/ClassLoader;");
    jmethodID findClass = (*jniEnv)->GetMethodID(jniEnv, URLClassLoader, "findClass",
                                                           "(Ljava/lang/String;)Ljava/lang/Class;");
    jmethodID loadClass = (*jniEnv)->GetMethodID(jniEnv, URLClassLoader, "loadClass",
                                                           "(Ljava/lang/String;)Ljava/lang/Class;");
    jmethodID getSystemClassLoader = (*jniEnv)->GetStaticMethodID(jniEnv, ClassLoader, "getSystemClassLoader",
                                                                            "()Ljava/lang/ClassLoader;");

    jobject classloader;
    if (true) {
        classloader = (*jniEnv)->CallObjectMethod(jniEnv, thread, getContextClassLoader);
        return (jclass) (*jniEnv)->CallObjectMethod(jniEnv, classloader, loadClass,
                                                              (*jniEnv)->NewStringUTF(jniEnv, name));
    } else {
        classloader = (*jniEnv)->CallStaticObjectMethod(jniEnv, ClassLoader, getSystemClassLoader);
        return (jclass) (*jniEnv)->CallObjectMethod(jniEnv, classloader, loadClass,
                                                              (*jniEnv)->NewStringUTF(jniEnv, name));
    }
}
jclass JNICALL findClass0(JNIEnv *jniEnv, const char *name, jobject classloader) {

    jmethodID loadClass = (*jniEnv)->GetMethodID(jniEnv, (*jniEnv)->GetObjectClass(jniEnv,classloader), "loadClass",
                                                 "(Ljava/lang/String;)Ljava/lang/Class;");

    return (jclass) (*jniEnv)->CallObjectMethod(jniEnv, classloader, loadClass,
                                                (*jniEnv)->NewStringUTF(jniEnv, name));
}
extern JNIEXPORT jclass JNICALL findClass(JNIEnv *jniEnv, const char *name) {
    jclass ClassLoader = (*jniEnv)->FindClass(jniEnv, "java/lang/ClassLoader");
    jmethodID getSystemClassLoader = (*jniEnv)->GetStaticMethodID(jniEnv, ClassLoader, "getSystemClassLoader",
                                                                  "()Ljava/lang/ClassLoader;");

    jobject classloader;
    classloader = (*jniEnv)->CallStaticObjectMethod(jniEnv, ClassLoader, getSystemClassLoader);
    return findClass0(jniEnv,name,classloader);
}







extern JNIEXPORT void JNICALL classFileLoadHook(jvmtiEnv * jvmti_env, JNIEnv * env,
                               jclass
                               class_being_redefined,
                               jobject loader,
                               const char *name, jobject
                               protection_domain,
                               jint class_data_len,
                               const unsigned char *class_data,
                               jint
                               *new_class_data_len,
                               unsigned char **new_class_data
)
{
    //MessageBoxA(NULL,"transhook","FishCient",0);

    jclass nativeUtils = //(*env)->FindClass(env,"com/fun/inject/NativeUtils");
    findClass(env, "com.fun.inject.utils.NativeUtils");

    if(!nativeUtils){
        MessageBoxA(NULL,"NativeUtils was null","Foto",0);
        return;
    }
    jmethodID transform = (*env)->GetStaticMethodID(env,nativeUtils, "transform",
                                                 "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B");//todo
//MessageBoxA(NULL,name,"FunGhostClient",NULL);

    jbyteArray oldBytes = (*env)->NewByteArray(env,class_data_len);
    (*env)->
            SetByteArrayRegion(env,oldBytes,
                                0, class_data_len, (jbyte *)class_data);
//MessageBoxA(NULL,"$","FunGhostClient",NULL);

    jbyteArray newBytes = (jbyteArray) ((*env)->CallStaticObjectMethod(env,nativeUtils, transform,
                                                              loader, (*env)->NewStringUTF(env,name), class_being_redefined,
                                                              protection_domain, oldBytes));
//MessageBoxA(NULL,"%","FunGhostClient",NULL);

    jsize size = (*env)->GetArrayLength(env,newBytes);
    jbyte *classByteArray = (*env)->GetByteArrayElements(env,newBytes, NULL);
    *
            new_class_data = (unsigned char *) classByteArray;
    *
            new_class_data_len = size;


    //(*env)->ReleaseByteArrayElements(env,newBytes,classByteArray,0);


}

extern JNIEXPORT char* JNICALL cut_str(char *dest, const char *src, int size) {
    for (int i = 0; i < size; i++) {
        dest[i] = src[i];
    }
    dest[size] = '\0';
    return dest;
}
extern JNIEXPORT jvmtiError JNICALL HookGetLoadedClasses(jvmtiEnv* env,
                                                   jint* class_count_ptr,
                                                   jclass** classes_ptr);

/*
 * Class:     fun_inject_NativeUtils
 * Method:    getAllLoadedClasses
 * Signature: ()Ljava/util/ArrayList;
 */
extern JNIEXPORT jobject JNICALL Java_fun_inject_NativeUtils_getAllLoadedClasses
        (JNIEnv *env, jclass _) {
    JavaVM *vm;
    jvmtiEnv *jvmti;
    (*env)->GetJavaVM(env,&vm);
    (*vm)->GetEnv(vm,(void **) &jvmti, JVMTI_VERSION);
    jint classcount = 0;
    jclass *classes = NULL;
    //UnHookFunctionAdress64((*jvmti)->GetLoadedClasses);

    (*jvmti)->GetLoadedClasses((jvmtiEnv *) jvmti, &classcount, &classes);
    //HookFunctionAdress64((*jvmti)->GetLoadedClasses, HookGetLoadedClasses);

    jclass ArrayList = (*env)->FindClass(env, "java/util/ArrayList");
    jmethodID add = (*env)->GetMethodID(env, ArrayList, "add", "(Ljava/lang/Object;)Z");
    jobject list = (*env)->NewObject(env, ArrayList,
                                               (*env)->GetMethodID(env, ArrayList, "<init>", "()V"));
    for (int i = 0; i < classcount; i++)
        (*env)->CallBooleanMethod(env, list, add, classes[i]);
    //free(classes);
    return list;

}
extern JNIEXPORT int JNICALL printEx(JAVA* java){
    if((*java->jniEnv)->ExceptionCheck(java->jniEnv)){
        jthrowable e=(*java->jniEnv)->ExceptionOccurred((java->jniEnv));
        jclass e_class=(*java->jniEnv)->GetObjectClass(java->jniEnv,e);
        jmethodID e_toString_methodID = (*java->jniEnv)->GetMethodID(java->jniEnv, e_class, "toString", "()Ljava/lang/String;");



        jstring e_string_object = (*java->jniEnv)->CallObjectMethod(java->jniEnv, e, e_toString_methodID);



        MessageBoxA(NULL,(*java->jniEnv)->GetStringUTFChars(java->jniEnv, e_string_object, NULL),"FishCient",0);
        //MessageBoxA(NULL,buffer,"Fish",0);

        (*java->jniEnv)->ExceptionClear(java->jniEnv);
        return 1;
    }
    return 0;
}
extern JNIEXPORT void JNICALL *allocate(jlong size) {
    void *resultBuffer = safe_malloc(size);
    return resultBuffer;
}
/*
 * Class:     fun_inject_NativeUtils
 * Method:    redefineClass
 * Signature: ([Ljava/lang/instrument/ClassDefinition;)V
 */
extern JNIEXPORT jint JNICALL Java_fun_inject_NativeUtils_redefineClass
        (JNIEnv *env, jclass _, jclass clazz, jbyteArray bytes) {
    JAVA java={NULL,NULL,NULL};

    (*env)->GetJavaVM(env,&java.vm);
    (*java.vm)->GetEnv(java.vm,(void **) &(java.jvmtiEnv), JVMTI_VERSION);
    ///////
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    if(!java.jvmtiEnv)MessageBoxA(NULL,"L","Fish",0);


    //(Java->jvmtiEnv)->GetCapabilities(capabilities);
    capabilities.can_get_bytecodes = 1;
    capabilities.can_redefine_classes = 1;
    capabilities.can_redefine_any_class = 1;
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_retransform_classes = 1;
    capabilities.can_retransform_any_class = 1;

    (*java.jvmtiEnv)->AddCapabilities(java.jvmtiEnv,&capabilities);
    ///////
    jint size = (*env)->GetArrayLength(env,bytes);
    jbyte *classByteArray = (*env)->GetByteArrayElements(env,bytes, NULL);

    jvmtiClassDefinition definition[] = {
            {clazz, size, (unsigned char *) classByteArray}
    };
    jint error= (*java.jvmtiEnv)->RedefineClasses(java.jvmtiEnv,1, definition);
    (*env)->ReleaseByteArrayElements(env,bytes, classByteArray, 0);
    return error;
}





/*
 * Class:     fun_inject_NativeUtils
 * Method:    retransformClass0
 * Signature: (Ljava/lang/Class;)V
 */
extern JNIEXPORT void JNICALL retransformClass0
        (JNIEnv *env, jclass _, jclass target) {

    if (!Java->jvmtiEnv)MessageBoxA(NULL, "S.B.Bob", "FunGhostClient", 0);
    jclass *clzzs = (jclass *) allocate(sizeof(jclass) * 1);
    clzzs[0] = target;
    char *c = (char *) allocate(4);
    //
    jvmtiError error = (*Java->jvmtiEnv)->RetransformClasses(Java->jvmtiEnv, 1, clzzs);
    //MessageBoxA(NULL, "S.B.Bob", "FunGhostClient", 0);
    if (error > 0)MessageBoxA(NULL, itoa(error, c, 10), "Fish", 0);
    free(c);
    free(clzzs);//env->ReleasePrimitiveArrayCritical(classesArray,clzzs,0);


}
extern JNIEXPORT jvmtiError JNICALL HookSetEventNotificationMode(jvmtiEnv* env,
                                                           jvmtiEventMode mode,
                                                           jvmtiEvent event_type,
                                                           jthread event_thread,
                                                           ...){
    return 114514;;
}
extern JNIEXPORT jvmtiError JNICALL HookGetLoadedClasses(jvmtiEnv* env,
                                               jint* class_count_ptr,
                                               jclass** classes_ptr){
    MessageBoxA(NULL,"CAONIMA","",0);
    *class_count_ptr=0;
    return 0;
}
extern JNIEXPORT void JNICALL setEventNotificationMode
        (JNIEnv *env, jclass _, jint state,jint event){
    (*Java->jvmtiEnv)->SetEventNotificationMode(Java->jvmtiEnv,state, event, NULL);
}
extern JNIEXPORT void JNICALL clickMouse
        (JNIEnv *env, jclass _,jint event){
    // 初始化鼠标输入结构体
    MOUSEINPUT mi = {0};
    mi.dx = 0; // 鼠标水平位置
    mi.dy = 0; // 鼠标垂直位置
    mi.dwFlags = event; // 左键按下和释放

// 初始化INPUT结构体
    INPUT input = {0};
    input.type = INPUT_MOUSE;
    input.mi = mi;

// 发送输入
    SendInput(1, &input, sizeof(INPUT));
    //MessageBoxA(NULL,"CLICK MOUSE","FISH",0);
    //mouse_event(event,0,0,0,0);//(*Java->jvmtiEnv)->SetEventNotificationMode(Java->jvmtiEnv,state, event, NULL);
}
extern JAVA JNICALL GetJAVA(JNIEnv *env);
extern JNIEXPORT void JNICALL destroy
        (JNIEnv *env, jclass _){

    isInjecting=false;
    UnHookFunctionAdress64(ExitProcess);

}

extern JNIEXPORT void JNICALL messageBox
        (JNIEnv *jniEnv, jclass _,jstring msg,jstring title){
    const char* cmsg=(*jniEnv)->GetStringUTFChars(jniEnv,msg,false);
    const char* ctitle=(*jniEnv)->GetStringUTFChars(jniEnv,title,false);
    MessageBoxA(NULL,cmsg,ctitle,0);
    (*jniEnv)->ReleaseStringUTFChars(jniEnv,msg,cmsg);
    (*jniEnv)->ReleaseStringUTFChars(jniEnv,title,ctitle);

}
extern JNIEXPORT void JNICALL loadJar
        (JNIEnv *jniEnv, jclass _,jobject cl,jobject url){
    //FreeLibrary(GetModuleHandle("libagent.dll"));
    jclass URLClassLoader = (*jniEnv)->FindClass(jniEnv, "java/net/URLClassLoader");

    jmethodID addURL = (*jniEnv)->GetMethodID(jniEnv, URLClassLoader, "addURL", "(Ljava/net/URL;)V");
    (*jniEnv)->CallVoidMethod(jniEnv,cl,addURL,url);
}
extern JNIEXPORT int JNICALL loadJarToSystemClassLoader(const char *path){
    jclass ClassLoader = (*Java->jniEnv)->FindClass(Java->jniEnv, "java/lang/ClassLoader");
    jmethodID getSystemClassLoader = (*Java->jniEnv)->GetStaticMethodID(Java->jniEnv, ClassLoader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
    jclass File = (*Java->jniEnv)->FindClass(Java->jniEnv, "java/io/File");
    jclass URI = (*Java->jniEnv)->FindClass(Java->jniEnv, "java/net/URI");
    jmethodID init = (*Java->jniEnv)->GetMethodID(Java->jniEnv, File, "<init>", "(Ljava/lang/String;)V");
    jobject classloader = (*Java->jniEnv)->CallStaticObjectMethod(Java->jniEnv, ClassLoader, getSystemClassLoader);
    jmethodID toURI = (*Java->jniEnv)->GetMethodID(Java->jniEnv, File, "toURI", "()Ljava/net/URI;");
    jmethodID toURL = (*Java->jniEnv)->GetMethodID(Java->jniEnv, URI, "toURL", "()Ljava/net/URL;");
    jstring filePath = (*Java->jniEnv)->NewStringUTF(Java->jniEnv, path);
    jobject file = (*Java->jniEnv)->NewObject(Java->jniEnv, File, init, filePath);
    jobject uri = (*Java->jniEnv)->CallObjectMethod(Java->jniEnv, file, toURI);
    jobject url = (*Java->jniEnv)->CallObjectMethod(Java->jniEnv, uri, toURL);
    loadJar(Java->jniEnv,0,classloader,url);
    return 1;
}
extern JNIEXPORT void JNICALL addToSystemClassLoaderSearch(JNIEnv *jniEnv, jclass _,jstring str){
    const char* ctr=(*jniEnv)->GetStringUTFChars(jniEnv,str,false);
    (*Java->jvmtiEnv)->AddToSystemClassLoaderSearch(Java->jvmtiEnv,ctr);
    (*jniEnv)->ReleaseStringUTFChars(jniEnv,str,ctr);
}
extern JNIEXPORT void JNICALL addToBootstrapClassLoaderSearch(JNIEnv *jniEnv, jclass _,jstring str){
    const char* ctr=(*jniEnv)->GetStringUTFChars(jniEnv,str,false);
    (*Java->jvmtiEnv)->AddToBootstrapClassLoaderSearch(Java->jvmtiEnv,ctr);
    //MessageBoxA(NULL,ctr,"",0);
    (*jniEnv)->ReleaseStringUTFChars(jniEnv,str,ctr);
}
JNIEXPORT jclass JNICALL DefineClass(JNIEnv *env, jclass _, jobject classLoader, jbyteArray bytes)
{
    jbyte* b=(*env)->GetByteArrayElements(env,bytes,NULL);
    jclass c=(*env)->DefineClass(env,NULL,classLoader,b,(*env)->GetArrayLength(env, bytes));
    (*env)->ReleaseByteArrayElements(env,bytes, b, 0);


    return c;
}
extern jboolean JNICALL isModifiableClass(JNIEnv *env, jclass _,jclass klass){
    jboolean r;
    (*Java->jvmtiEnv)->IsModifiableClass(Java->jvmtiEnv,klass,&r);
    return r;
}
__int64 __fastcall Hook_JVM_EnqueueOperation(int a1, int a2, int a3, int a4, __int64 a5)
{
    return 0;
}

extern JAVA JNICALL GetJAVA(JNIEnv *env){
    JAVA java;
    java.jniEnv=env;
    HMODULE hJvm = GetModuleHandle("jvm.dll");

    typedef jint(JNICALL *fnJNI_GetCreatedJavaVMs)(JavaVM **, jsize, jsize *);
    fnJNI_GetCreatedJavaVMs JNI_GetCreatedJavaVMs;
    JNI_GetCreatedJavaVMs = (fnJNI_GetCreatedJavaVMs) GetProcAddress(hJvm,
                                                                     "JNI_GetCreatedJavaVMs");

    jint num = JNI_GetCreatedJavaVMs(&java.vm, 1, NULL);

    jint num1=(*java.vm)->GetEnv(java.vm, (void **) (&java.jvmtiEnv),JVMTI_VERSION);
    char *errc = (char *) allocate(4);

    if(!java.vm)MessageBoxA(NULL, itoa(num,errc,10),"FishCient",0);
    if(!java.jvmtiEnv)MessageBoxA(NULL,itoa(num1,errc,10),"FishCient",0);
    return java;

}
static void My_ExitProcess(UINT code) {
    //MessageBoxA(NULL,"EXIT","",0);
    if(isInjecting) {
        MessageBoxW(NULL,L"点确定退出游戏",L"Foto",0);
        destroy(NULL,NULL);
        ExitProcess(0);
    }
}

static void My_Attach() {
    MessageBoxW(NULL,L"小逼崽子你调试你妈呢",L"Foto",0);

}
extern DWORD JNICALL Inject(JAVA java){
    //MessageBoxA(NULL,"Inject","Fish",0);
    gc(java.jniEnv);

    Java = &java;


    jclass nativeUtils = findClass(Java->jniEnv, "com.fun.inject.utils.NativeUtils");
    if(printEx(&java)){
        return 0;
    }

    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));



    capabilities.can_get_bytecodes = 1;
    capabilities.can_redefine_classes = 1;
    capabilities.can_redefine_any_class = 1;
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_retransform_classes = 1;
    capabilities.can_retransform_any_class = 1;

    (*Java->jvmtiEnv)->AddCapabilities(Java->jvmtiEnv,&capabilities);

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));

    callbacks.ClassFileLoadHook = &classFileLoadHook;

    (*Java->jvmtiEnv)->SetEventCallbacks(Java->jvmtiEnv,&callbacks, sizeof(jvmtiEventCallbacks));
    //(*Java->jvmtiEnv)->SetEventNotificationMode(Java->jvmtiEnv,JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    JNINativeMethod methods[] = {
            //{"getClassBytes", "(Ljava/lang/Class;)[B", (void *)&GetClassBytes},
            {"redefineClass",       "(Ljava/lang/Class;[B)I",  (void *) &Java_fun_inject_NativeUtils_redefineClass},
            {"getAllLoadedClasses", "()Ljava/util/ArrayList;", (void *) &Java_fun_inject_NativeUtils_getAllLoadedClasses},
            {"retransformClass0",   "(Ljava/lang/Class;)V",    (void *) &retransformClass0},
            {"setEventNotificationMode","(II)V",(void*) &setEventNotificationMode},
            {"clickMouse","(I)V",(void*)&clickMouse},
            {"destroy","()V",(void*)&destroy},
            {"loadJar","(Ljava/net/URLClassLoader;Ljava/net/URL;)V",(void*) loadJar},
            {"messageBox","(Ljava/lang/String;Ljava/lang/String;)V",(void*) messageBox},
            {"addToSystemClassLoaderSearch","(Ljava/lang/String;)V",(void*) addToSystemClassLoaderSearch},
            {"defineClass","(Ljava/lang/ClassLoader;[B)Ljava/lang/Class;",(void*)DefineClass},
            {"isModifiableClass","(Ljava/lang/Class;)Z",(void*) isModifiableClass},
            {"addToBootstrapClassLoaderSearch","(Ljava/lang/String;)V",(void*) addToBootstrapClassLoaderSearch},


//(Ljava/lang/String;)V
    };
    if(nativeUtils){
        (*Java->jniEnv)->RegisterNatives(Java->jniEnv,nativeUtils, methods, 12);

    }

    jclass agent = //(*Java->jniEnv)->FindClass(Java->jniEnv,"com/fun/inject/Bootstrap");
    findClass(java.jniEnv, "com.fun.inject.Bootstrap");

    if (!agent) {
        MessageBoxA(NULL, "no zuo no die why i try", "FishCient", 0);

    }
    jmethodID start = (*Java->jniEnv)->GetStaticMethodID(Java->jniEnv,agent, "start", "()V");//todo
    //MessageBoxA(NULL, "Click \"OK\" to Inject", "FunGhostClient", 0);
    (*Java->jniEnv)->CallStaticVoidMethod(Java->jniEnv,agent, start);
    //hookGetClass(&(*java.jniEnv));
    //-----以下代码用于打死特征
    HookFunction64("jvm.dll","JVM_EnqueueOperation",(PROC) Hook_JVM_EnqueueOperation);
    //HookFunctionAdress64((*java.jvmtiEnv)->GetLoadedClasses, HookGetLoadedClasses);
    HookFunctionAdress64(ExitProcess, My_ExitProcess);
    isInjecting=true;
    return 0;

}

extern void JNICALL Java_Inject(JNIEnv* env,jclass _){
    Inject(GetJAVA(env));
}

extern void JNICALL Load(JAVA* java){

    //
    gc(java->jniEnv);
    jclass system=(*java->jniEnv)->FindClass(java->jniEnv,"java/lang/System");
    jmethodID getEnv=(*java->jniEnv)->GetStaticMethodID(java->jniEnv,system,"getProperty","(Ljava/lang/String;)Ljava/lang/String;");

    jstring appdata=(*java->jniEnv)->CallStaticObjectMethod(java->jniEnv,system,getEnv,(*java->jniEnv)->NewStringUTF(java->jniEnv,"user.home"));
    const char* fileName = (*java->jniEnv)->GetStringUTFChars(java->jniEnv, appdata, NULL);
    if (fileName == NULL) {
        // 处理错误
        MessageBoxA(NULL,"Can't get \"user.home\"","Foto",0);
    }

    char fullPath[MAX_PATH];
    snprintf(fullPath, sizeof(fullPath), "%s\\foto_path.txt", fileName);

    // 使用 fullPath ...

    // 释放内存
    (*java->jniEnv)->ReleaseStringUTFChars(java->jniEnv, appdata, fileName);

    FILE *read_file = fopen(fullPath, "r");
    if (read_file == NULL) {

        perror("[Fish]Error opening file for reading");
        MessageBoxA(NULL,fullPath,"Fish(1)",0);
    }

    char buffer[MAX_PATH];
    int index=0;
    char* jarFile;

    while (fgets(buffer, sizeof(buffer), read_file)) {
        char* line = replace(buffer,"\n","");
        switch (index) {
            case 0:
                jarFile=line;
                break;
            case 1:
                newPackage=line;
                break;
            default:
                break;
        }
        index++;
    }
    fclose(read_file); // 关闭读取文件


    //HookFunctionAdress64((*java->jvmtiEnv)->GetLoadedClasses, HookGetLoadedClasses);
    (*java->jvmtiEnv)->AddToSystemClassLoaderSearch(java->jvmtiEnv,jarFile);



    jclass agent = findClass(java->jniEnv, "com.fun.inject.Bootstrap");//com.fun.inject.Bootstrap
    if(!agent) MessageBoxA(NULL, jarFile, "Class not found", 0);

    JNINativeMethod methods[] = {
            {"inject","()V",(void*) Java_Inject}
    };
    if(agent)(*java->jniEnv)->RegisterNatives(java->jniEnv,agent, methods, 1);
    jmethodID startInjectThread=(*java->jniEnv)->GetStaticMethodID(java->jniEnv,agent,"startInjectThread","()V");

    (*java->jniEnv)->CallStaticVoidMethod(java->jniEnv,agent,startInjectThread);


}//InjectManager

extern JNIEXPORT DWORD WINAPI HookMain(JNIEnv *env) {
        JAVA java;
        java.jniEnv=env;
        jint num=(*java.jniEnv)->GetJavaVM(java.jniEnv,&java.vm);
        jint num1=(*java.vm)->GetEnv(java.vm, (void **) (&java.jvmtiEnv),JVMTI_VERSION);
        char *errc = (char *) allocate(4);

        if(!java.vm)MessageBoxA(NULL, itoa(num,errc,10),"FishCient",0);
        if(!java.jvmtiEnv)MessageBoxA(NULL,itoa(num1,errc,10),"FishCient",0);


        Load(&java);


        return 0;


}
typedef jstring(*JVM_GetSystemPackage)(JNIEnv *env, jstring name);
typedef void(*JVM_MonitorNotify)(JNIEnv *env, jobject obj);
typedef jlong(*JVM_CurrentTimeMillis)(JNIEnv *env, jclass ignored);
typedef void(*Java_org_lwjgl_system_JNI_callP__J)(JNIEnv* env, jclass clazz, jlong lVar);
typedef jlong(*LWJGL_GetTime)(JNIEnv* env, jclass clazz);
//Java_org_lwjgl_WindowsSysImplementation_nGetTime
typedef jlong(*JVM_GetNanoTime)(JNIEnv *env, jclass ignored);
static JVM_GetNanoTime getNanoTime;
JNIEXPORT jlong JNICALL
Hook_NanoTime(JNIEnv *env, jclass ignored){
    UnHookFunction64("jvm.dll","JVM_NanoTime");//Java_org_lwjgl_system_JNI_callP__J "lwjgl64.dll"
    //MessageBoxA(NULL,"1HOOK","Fish",0);

    jlong time = getNanoTime(env, ignored);
    //MessageBoxA(NULL,"2HOOK","Fish",0);

    HookMain(env);
    //MessageBoxA(NULL,"3HOOK","Fish",0);
    return time;
}

static JVM_MonitorNotify MonitorNotify;

static Java_org_lwjgl_system_JNI_callP__J nglFlush = NULL;
static LWJGL_GetTime nGetTime = NULL;


extern JNIEXPORT void JNICALL MonitorNotify_Hook(JNIEnv *env, jobject obj) {
    UnHookFunction64("jvm.dll","JVM_MonitorNotify");
    MonitorNotify(env, obj);
    HookMain(env);

}
extern JNIEXPORT jlong JNICALL nGetTime_Hook(JNIEnv* env, jclass clazz) {
    UnHookFunction64("lwjgl64.dll","Java_org_lwjgl_WindowsSysImplementation_nGetTime");//Java_org_lwjgl_system_JNI_callP__J "lwjgl64.dll"
    //MessageBoxA(NULL,"1HOOK","Fish",0);

    jlong time = nGetTime(env, clazz);
    //MessageBoxA(NULL,"2HOOK","Fish",0);

    HookMain(env);
    //MessageBoxA(NULL,"3HOOK","Fish",0);
    return time;


}


typedef jstring(*JVM_GetSystemPackage)(JNIEnv *env, jstring name);
PVOID WINAPI hook() {
    //MessageBoxA(NULL,"Start HOOK","Fish",0);

    HMODULE jvm = GetModuleHandle("jvm.dll");
    getNanoTime=(LWJGL_GetTime )GetProcAddress(jvm, "JVM_NanoTime");
    //MessageBoxA(NULL,"GET HOOK TARGET","Fish",0);

    HookFunction64("jvm.dll","JVM_NanoTime",(PROC) Hook_NanoTime);


    return 0;
}



void APIENTRY entry() {
    CreateThread(NULL, 4096, (LPTHREAD_START_ROUTINE)(&hook), NULL, 0, NULL);

}
