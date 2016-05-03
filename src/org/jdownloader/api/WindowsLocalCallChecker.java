package org.jdownloader.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public class WindowsLocalCallChecker {

    public static interface Iphlpapi extends StdCallLibrary {
        int GetExtendedTcpTable(MIB_TCPTABLE_OWNER_PID pTcpTable, IntByReference pdwSize, boolean bOrder, long ulAf, int table, long reserved);
    }

    public static class MIB_TCPROW_OWNER_PID extends Structure {
        public DWORD dwState;
        public DWORD dwLocalAddr;
        public DWORD dwLocalPort;
        public DWORD dwRemoteAddr;
        public DWORD dwRemotePort;
        public DWORD dwOwningPid;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "dwState", "dwLocalAddr", "dwLocalPort", "dwRemoteAddr", "dwRemotePort", "dwOwningPid" });
        }

    }

    public static class MIB_TCPTABLE_OWNER_PID extends Structure {
        public MIB_TCPTABLE_OWNER_PID(Memory ptr) {
            super(ptr);
        }

        public DWORD                  dwNumEntries;
        public MIB_TCPROW_OWNER_PID[] table = new MIB_TCPROW_OWNER_PID[1];

        public MIB_TCPTABLE_OWNER_PID() {
        }

        public MIB_TCPTABLE_OWNER_PID(int size) {
            this.dwNumEntries = new DWORD(size);
            this.table = (MIB_TCPROW_OWNER_PID[]) new MIB_TCPROW_OWNER_PID().toArray(size);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "dwNumEntries", "table" });
        }

    }

    /**
     * Stores the information about a Win32 process.
     */
    public static final class ProcessInfo {
        /** Process id. */
        private int    processId;

        /** Parent process id. */
        private int    parentProcessId;

        /** Path to this process's image. */
        private String imageName;

        /**
         * Constructs a new ProcessInfo object.
         *
         * @param processId
         *            Process id.
         * @param parentProcessId
         *            Parent process id.
         * @param imageName
         *            Process image name.
         */
        public ProcessInfo(final int processId, final int parentProcessId, final String imageName) {
            this.processId = processId;
            this.parentProcessId = parentProcessId;
            this.imageName = imageName;
        }

        /**
         * Returns the process id.
         *
         * @return The process id.
         */
        public int getProcessId() {
            return processId;
        }

        /**
         * Returns the parent process id.
         *
         * @return The parent process id.
         */
        public int getParentProcessId() {
            return parentProcessId;
        }

        /**
         * Returns the image name.
         *
         * @return The image name.
         */
        public String getImageName() {
            return imageName;
        }
    }

    private static Kernel32 kernel32;
    private static psapi    psapi;
    private static Iphlpapi iphlpapi;

    public static List<ProcessInfo> getProcessList() throws Exception {
        /* Initialize the empty process list. */
        List<ProcessInfo> processList = new ArrayList<ProcessInfo>();

        /* Create the process snapshot. */
        HANDLE snapshot = ProcessPathKernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new DWORD(0));

        Tlhelp32.PROCESSENTRY32.ByReference pe = new Tlhelp32.PROCESSENTRY32.ByReference();
        for (boolean more = Kernel32.INSTANCE.Process32First(snapshot, pe); more; more = Kernel32.INSTANCE.Process32Next(snapshot, pe)) {
            /* Open this process; ignore processes that we cannot open. */
            HANDLE hProcess = Kernel32.INSTANCE.OpenProcess(0x1000, /* PROCESS_QUERY_LIMITED_INFORMATION */
                    false, pe.th32ProcessID.intValue());
            if (hProcess == null) {
                continue;
            }

            /* Get the image name. */
            char[] imageNameChars = new char[1024];
            IntByReference imageNameLen = new IntByReference(imageNameChars.length);
            if (!ProcessPathKernel32.INSTANCE.QueryFullProcessImageName(hProcess, new DWORD(0), imageNameChars, imageNameLen)) {
                throw new Exception("Couldn't get process image name for " + pe.th32ProcessID.intValue());
            }

            /* Add the process info to our list. */
            processList.add(new ProcessInfo(pe.th32ProcessID.intValue(), pe.th32ParentProcessID.intValue(), new String(imageNameChars, 0, imageNameLen.getValue())));

            /* Close the process handle. */
            Kernel32.INSTANCE.CloseHandle(hProcess);
        }

        /* Close the process snapshot. */
        Kernel32.INSTANCE.CloseHandle(snapshot);

        /* Return the process list. */
        return processList;
    }

    public static void main(String[] args) throws Exception {
        List<ProcessInfo> lst = getProcessList();
        for (ProcessInfo p : lst) {
            System.out.println(p.getImageName() + " " + p.getProcessId());
        }
        kernel32 = (Kernel32) com.sun.jna.Native.loadLibrary("kernel32", Kernel32.class);
        psapi = (psapi) Native.loadLibrary("psapi", psapi.class);
        iphlpapi = (Iphlpapi) com.sun.jna.Native.loadLibrary("iphlpapi", Iphlpapi.class);
        MIB_TCPTABLE_OWNER_PID table = new MIB_TCPTABLE_OWNER_PID();
        IntByReference psize = new IntByReference(table.size());

        int status = iphlpapi.GetExtendedTcpTable(table, psize, false, 2, 5, 0);
        if (status == 122) {
            table = new MIB_TCPTABLE_OWNER_PID((psize.getValue() - 4) / table.table[0].size());
            psize.setValue(table.size());

            status = iphlpapi.GetExtendedTcpTable(table, psize, false, 2, 5, 0);
            for (MIB_TCPROW_OWNER_PID e : table.table) {

                System.out.println(StringUtils.fillPost(getInt(e.dwLocalAddr) + ":" + switchbytes(e.dwLocalPort), " ", 50) + "\t" + StringUtils.fillPost(getInt(e.dwRemoteAddr) + ":" + switchbytes(e.dwRemotePort), " ", 50) + "\tState:" + StringUtils.fillPost(e.dwState + "", " ", 10) + "\t" + e.dwOwningPid + "\t" + getProcessInfo(e.dwOwningPid.intValue()));
            }
            System.out.println("Entries: " + table);
            // Buffer
            // Native.getDirectBufferPointer(b)
        } else {
            System.out.println("BAD BUFFER");
        }
    }

    public static interface ProcessPathKernel32 extends Kernel32 {
        class MODULEENTRY32 extends Structure {
            public static class ByReference extends MODULEENTRY32 implements Structure.ByReference {
                public ByReference() {
                }

                public ByReference(Pointer memory) {
                    super(memory);
                }
            }

            public MODULEENTRY32() {
                dwSize = new WinDef.DWORD(size());
            }

            public MODULEENTRY32(Pointer memory) {
                super(memory);
                read();
            }

            public DWORD   dwSize;
            public DWORD   th32ModuleID;
            public DWORD   th32ProcessID;
            public DWORD   GlblcntUsage;
            public DWORD   ProccntUsage;
            public Pointer modBaseAddr;
            public DWORD   modBaseSize;
            public HMODULE hModule;
            public char[]  szModule  = new char[255 + 1]; // MAX_MODULE_NAME32
            public char[]  szExePath = new char[MAX_PATH];

            public String szModule() {
                return Native.toString(this.szModule);
            }

            public String szExePath() {
                return Native.toString(this.szExePath);
            }

            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[] { "dwSize", "th32ModuleID", "th32ProcessID", "GlblcntUsage", "ProccntUsage", "modBaseAddr", "modBaseSize", "hModule", "szModule", "szExePath" });
            }
        }

        boolean QueryFullProcessImageName(HANDLE hProcess, DWORD dwFlags, char[] lpExeName, IntByReference lpdwSize);

        ProcessPathKernel32 INSTANCE = (ProcessPathKernel32) Native.loadLibrary(ProcessPathKernel32.class, W32APIOptions.UNICODE_OPTIONS);

        boolean Module32First(HANDLE hSnapshot, MODULEENTRY32.ByReference lpme);

        boolean Module32Next(HANDLE hSnapshot, MODULEENTRY32.ByReference lpme);
    }

    private interface psapi extends StdCallLibrary {
        // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683198%28v=vs.85%29.aspx
        int GetModuleFileNameExA(HANDLE process, Pointer hModule, byte[] lpString, int nMaxCount);
    };

    private static String getProcessInfo(int dwOwningPid) {
        // final IntByReference pid = new IntByReference();
        String superPath = "";
        // S ystem.out.print(processEntry.th32ProcessID + "\t" + Native.toString(processEntry.szExeFile) + "\t");
        WinNT.HANDLE moduleSnapshot = kernel32.CreateToolhelp32Snapshot(new DWORD(Tlhelp32.TH32CS_SNAPMODULE.intValue() | Tlhelp32.TH32CS_SNAPMODULE32.intValue()), new DWORD(dwOwningPid));
        try {
            ProcessPathKernel32.MODULEENTRY32.ByReference me = new ProcessPathKernel32.MODULEENTRY32.ByReference();
            ProcessPathKernel32.INSTANCE.Module32First(moduleSnapshot, me);
            superPath = me.szExePath();
            System.out.print(": " + me.szModule());
            System.out.println();
        } finally {
            kernel32.CloseHandle(moduleSnapshot);
        }

        final HANDLE process = Kernel32.INSTANCE.OpenProcess(0x1000 /* PROCESS_QUERY_INFORMATION */
                , false, dwOwningPid);
        if (process != null) {
            try {
                final Pointer zero = new Pointer(0);
                final byte[] exePathname = new byte[1024];
                final int result = psapi.GetModuleFileNameExA(process, zero, exePathname, 512);
                final String ret = Native.toString(exePathname).substring(0, result);
                if (ret != null) {
                    return ret.trim() + "(" + superPath + ")";
                }
            } finally {
                kernel32.CloseHandle(process);
            }
        }

        return Kernel32Util.formatMessageFromLastErrorCode(Kernel32.INSTANCE.GetLastError()) + "(" + superPath + ")";
    }

    public static byte[] int32toBytes(int hex) {
        byte[] b = new byte[4];
        b[3] = (byte) ((hex & 0xFF000000) >> 24);
        b[2] = (byte) ((hex & 0x00FF0000) >> 16);
        b[1] = (byte) ((hex & 0x0000FF00) >> 8);
        b[0] = (byte) (hex & 0x000000FF);
        return b;

    }

    public static String switchbytes(DWORD value) {
        int b1 = (value.intValue() & 0x0000FF00) >> 8;
        int b2 = value.intValue() & 0x000000FF;
        int port = (b2 << 8) + b1;
        return port + "";
    }

    private static String getInt(DWORD d) {
        int dwLocalAddr = d.intValue();
        try {

            if (dwLocalAddr == 0) {
                return "0";
            }
            byte[] bytes = int32toBytes(dwLocalAddr);

            InetAddress address = InetAddress.getByAddress(bytes);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            // e.printStackTrace();
            
        }
        return dwLocalAddr + "";

    }
}
