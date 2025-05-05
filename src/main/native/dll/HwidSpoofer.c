//
// Created by fenge on 2025/1/4.
//
//
#include <windows.h>
#include "../java/jni.h"
#include "utils.h"
#include "../java/jvmti.h"
#include <stdlib.h>
#include <time.h>
#include <stdio.h>

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
jbyteArray __fastcall HookedGetMacAddr0(
        JNIEnv*  env,
        __int64 a2,
        __int64 a3,
        __int64 a4,
        unsigned int a5)
{
    jbyteArray macAddress = (*env)->NewByteArray(env, 6);
    if (macAddress == NULL) {
        return 0; // 内存分配失败
    }

    // 初始化随机数生成器
    srand((unsigned int)time(NULL));

    // 生成随机字节
    jbyte bytes[6];
    for (int i = 0; i < 6; i++) {
        bytes[i] = (jbyte)(rand() % 256);
    }

    // 将字节复制到 Java 数组中
    (*env)->SetByteArrayRegion(env, macAddress, 0, 6, bytes);

    return macAddress;
}
VOID JNICALL Remote(){
    HookFunction64("net.dll", "Java_java_net_NetworkInterface_getMacAddr0",(void*)HookedGetMacAddr0);

}
void entry(){
    CreateThread(NULL, 4096, (LPTHREAD_START_ROUTINE)(&Remote), NULL, 0, NULL);

}
