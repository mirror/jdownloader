package org.jdownloader.nativ;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jd.utils.JDUtilities;

public class NativeProcess {

    private static native int sendCtrlCEvent(int pid);

    private static native boolean sendSignal(int pid, int signal);

    private int                  pid         = -1;
    private final FileDescriptor inStreamFd  = new FileDescriptor();
    private final FileDescriptor outStreamFd = new FileDescriptor();
    private final FileDescriptor errStreamFd = new FileDescriptor();
    private OutputStream         stdin_stream;
    private InputStream          stdout_stream;
    private InputStream          stderr_stream;

    static {
        /* these libs are 32bit */
        try {
            System.load(JDUtilities.getResourceFile("tools/Windows/rtmpdump/NativeProcessx86.dll").getAbsolutePath());
        } catch (final Throwable e) {
            System.out.println("Error loading 32bit: " + e);
            /* these libs are 64bit */
            try {
                System.load(JDUtilities.getResourceFile("tools/Windows/rtmpdump/NativeProcessx64.dll").getAbsolutePath());
            } catch (final Throwable e2) {
                System.out.println("Error loading 64bit: " + e2);
            }
        }
    }

    private static native int createProcess(String cmd, String param, boolean hidden, FileDescriptor in, FileDescriptor out, FileDescriptor err);

    private static native boolean terminateProcess(int pid);

    private static native boolean terminateProcessTree(int pid);

    public NativeProcess(final String cmd, final String param) throws Exception {

        if ((pid = createProcess(cmd, param, true, inStreamFd, outStreamFd, errStreamFd)) < 0) { throw new Exception("Error invalid PID while creating process - (pid < 0)"); }

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                stdin_stream = new BufferedOutputStream(new FileOutputStream(inStreamFd));
                stdout_stream = new BufferedInputStream(new FileInputStream(outStreamFd));
                stderr_stream = new BufferedInputStream(new FileInputStream(errStreamFd));
                return null;
            }
        });
    }

    /**
     * Forces to terminates this process (not its children)
     */
    public void forceTerminate() {
        if (pid >= 0) {
            terminateProcess(pid);
        }
    }

    /**
     * Forces to terminates this process with all of its children
     */
    public void forceTerminateTree() {
        if (pid >= 0) {
            terminateProcessTree(pid);
        }
    }

    public InputStream getErrorStream() {
        return stderr_stream;
    }

    public InputStream getInputStream() {
        return stdout_stream;
    }

    public OutputStream getOutputStream() {
        return stdin_stream;
    }

    /**
     * Request to terminate this process by sending CTRL + C Signal
     */
    public void sendCtrlCSignal() {
        if (pid >= 0) {
            try {
                Runtime.getRuntime().exec(".\\tools\\Windows\\rtmpdump\\SendSignal.exe " + String.valueOf(pid));
            } catch (final Exception e) {
            }

        }
    }

    /**
     * Request to terminate this process by sending CTRL + C Signal
     */
    public int sendCtrlCSignalNative() {
        int retVal = -1;
        if (pid >= 0) {
            retVal = sendCtrlCEvent(pid);
        }
        return retVal;
    }

    public void sendSignalTerminate() {
        if (pid >= 0) {
            sendSignal(pid, 0);
        }
    }
}
