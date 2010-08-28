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

/**
 * Library used as a wrapper for the librtmp of rtmpdump
 * 
 * @author mike0007
 */
public interface LibRtmp extends Library {

    /**
     * Allocate memory for a rtmp session
     * 
     * @return <tt>Pointer</tt> to the session of this dowanlod/request
     */
    public Pointer RTMP_Alloc();

    /**
     * Initializes a rtmp session
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     */
    public void RTMP_Init(Pointer session);

    /**
     * Frees the memory of a rtmp session
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     */
    public void RTMP_Free(Pointer session);

    /**
     * closes the stream of a rtmp session
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     */
    public void RTMP_Close(Pointer session);

    /**
     * Setup a rtmp session by passing URL with parameters
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param url
     *            <tt>String</tt> with the url and the parameters
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_SetupURL(Pointer session, String url);

    /**
     * Enable write possibility to the rtmp session
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     */
    public void RTMP_EnableWrite(Pointer session);

    /**
     * Read bytes from the stream
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param buf
     *            <tt>byte[]</tt> array used as buffer for the data
     * @param size
     *            Size of the byte array
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_Read(Pointer session, byte[] buf, int size);

    /**
     * Write bytes to the stream
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param buf
     *            <tt>byte[]</tt> array used as buffer for the data
     * @param size
     *            Size of the byte array
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_Write(Pointer session, byte[] buf, int size);

    /**
     * Connect the session with the server
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param rtmpPacket
     *            <tt>Pointer</tt> to the RTMPPacket (use <tt>Pointer.NULL</tt>)
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_Connect(Pointer session, Pointer rtmpPacket);

    /**
     * Connect the stream with the server
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param seekTime
     *            Time to start from ?
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_ConnectStream(Pointer session, int seekTime);

    /**
     * Reconnect the stream with the server
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @param seekTime
     *            Time to start from ?
     * @see LibRtmp#RTMP_Alloc()
     */
    public int RTMP_ReconnectStream(Pointer session, int seekTime);

    /**
     * Get the version number of the lib
     * 
     * @return <tt>Integer</tt> coded in hex values (use
     *         <tt>Integer.toHexString(...)</tt> to show correctly)
     */
    public int RTMP_LibVersion();

    /**
     * Sends an Interrupt signal
     */
    public void RTMP_UserInterrupt();

    /**
     * ?
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     * @return
     */
    public int RTMP_ToggleStream(Pointer session);

    /**
     * ?
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     * @return
     */
    public int RTMP_DeleteStream(Pointer session);

    /**
     * Returns the length of the data in seconds
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     * @return <tt>Double</tt> indicate the length of the source in seconds
     */
    public double RTMP_GetDuration(Pointer session);

    /**
     * Returns the state of the stream connection
     * 
     * @param session
     *            <tt>Pointer</tt> to the rtmp session
     * @see LibRtmp#RTMP_Alloc()
     * @return 1 if the is connected. 0 otherwise
     */
    public int RTMP_IsConnected(Pointer session);

}
