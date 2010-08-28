package jd.network.rtmp.rtmp;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

// Native Type, Size, Java Type, Common Windows Types
// char 8-bit integer byte BYTE, TCHAR
// short 16-bit integer short WORD
// wchar_t 16/32-bit character char TCHAR
// int 32-bit integer int DWORD
// int boolean value boolean BOOL
// long 32/64-bit integer NativeLong LONG
// long long 64-bit integer long __int64
// float 32-bit FP float
// double 64-bit FP double
// char* C string String LPTCSTR
// void* pointer Pointer LPVOID, HANDLE, LPXXX

public interface LibRtmp extends Library {

    public Pointer RTMP_Alloc();

    public void RTMP_Init(Pointer r);

    public void RTMP_Free(Pointer r);

    public void RTMP_Close(Pointer r);

    public int RTMP_SetupURL(Pointer r, String url);

    public void RTMP_EnableWrite(Pointer r);

    public int RTMP_Read(Pointer r, byte[] buf, int size);

    public int RTMP_Write(Pointer r, byte[] buf, int size);

    public int RTMP_Connect(Pointer r, Pointer cp);

    public int RTMP_ConnectStream(Pointer r, int seekTime);

    public int RTMP_LibVersion();

    public void RTMP_UserInterrupt();

    public double RTMP_GetDuration(Pointer r);

    public int RTMP_IsConnected(Pointer r);

}
