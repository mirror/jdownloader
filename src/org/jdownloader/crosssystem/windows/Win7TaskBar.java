package org.jdownloader.crosssystem.windows;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.peer.ComponentPeer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import org.appwork.utils.JVMVersion;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.Log;
import org.jdownloader.crosssystem.windows.apache.sanselan.BinaryConstants;
import org.jdownloader.crosssystem.windows.apache.sanselan.BinaryOutputStream;
import org.jdownloader.crosssystem.windows.apache.sanselan.ImageWriteException;

import sun.awt.AWTAccessor;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.ObjBase;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Ole32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Taken From: https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ui/Win7TaskBar.java
 *
 *
 *
 * Changes done by AppWork GmbH:
 *
 * Used org.appwork.utils.logging2.LogInterface as logging interface
 *
 * Removed dependencies to import com.intellij.openapi.application.ApplicationManager;
 *
 * Removed dependencies to import com.intellij.openapi.wm.IdeFrame;
 *
 * Removed dependencies import com.intellij.openapi.diagnostic.Logger;
 *
 * @author Alexander Lobas
 */
public class Win7TaskBar {
    private static final LogInterface     LOG                          = Log.DF;
    private static final int              TaskBarList_Methods          = 21;
    private static final int              TaskBarList_SetProgressValue = 9;
    private static final int              TaskBarList_SetProgressState = 10;
    private static final int              TaskBarList_SetOverlayIcon   = 18;
    private static final WinDef.DWORD     ICO_VERSION                  = new WinDef.DWORD(0x00030000);
    private static final WinDef.DWORD     DWORD_ZERO                   = new WinDef.DWORD(0);
    private static final WinDef.DWORD     TBPF_NOPROGRESS              = DWORD_ZERO;
    private static final WinDef.DWORD     TBPF_NORMAL                  = new WinDef.DWORD(0x2);
    private static final WinDef.DWORD     TBPF_ERROR                   = new WinDef.DWORD(0x4);
    private static final WinDef.ULONGLONG TOTAL_PROGRESS               = new WinDef.ULONGLONG(100);
    private static Pointer                myInterfacePointer;
    private static Function               mySetProgressValue;
    private static Function               mySetProgressState;
    private static Function               mySetOverlayIcon;

    public interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = (User32Ex) Native.loadLibrary("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

        int LookupIconIdFromDirectoryEx(Memory presbits, boolean fIcon, int cxDesired, int cyDesired, int Flags);

        WinDef.HICON CreateIconFromResourceEx(Pointer presbits, WinDef.DWORD dwResSize, boolean fIcon, WinDef.DWORD dwVer, int cxDesired, int cyDesired, int Flags);

        boolean FlashWindow(WinDef.HWND hwnd, boolean bInvert);
    }

    public static void writeTransparentIcoImageWithSanselan(BufferedImage src, OutputStream os) throws ImageWriteException, IOException {
        // LOG.assertTrue();
        boolean assertt = BufferedImage.TYPE_INT_ARGB == src.getType() || BufferedImage.TYPE_4BYTE_ABGR == src.getType();
        int bitCount = 32;
        // org.apache.sanselan.common.BinaryOutputStream.BinaryOutputStream(OutputStream, int)
        BinaryOutputStream bos = new BinaryOutputStream(os, BinaryConstants.BYTE_ORDER_INTEL);
        try {
            int scanline_size = (bitCount * src.getWidth() + 7) / 8;
            if ((scanline_size % 4) != 0) {
                scanline_size += 4 - (scanline_size % 4); // pad scanline to 4 byte size.
            }
            int t_scanline_size = (src.getWidth() + 7) / 8;
            if ((t_scanline_size % 4) != 0) {
                t_scanline_size += 4 - (t_scanline_size % 4); // pad scanline to 4 byte size.
            }
            int imageSize = 40 + src.getHeight() * scanline_size + src.getHeight() * t_scanline_size;
            // ICONDIR
            bos.write2Bytes(0); // reserved
            bos.write2Bytes(1); // 1=ICO, 2=CUR
            bos.write2Bytes(1); // count
            // ICONDIRENTRY
            int iconDirEntryWidth = src.getWidth();
            int iconDirEntryHeight = src.getHeight();
            if (iconDirEntryWidth > 255 || iconDirEntryHeight > 255) {
                iconDirEntryWidth = 0;
                iconDirEntryHeight = 0;
            }
            bos.write(iconDirEntryWidth);
            bos.write(iconDirEntryHeight);
            bos.write(0);
            bos.write(0); // reserved
            bos.write2Bytes(1); // color planes
            bos.write2Bytes(bitCount);
            bos.write4Bytes(imageSize);
            bos.write4Bytes(22); // image offset
            // BITMAPINFOHEADER
            bos.write4Bytes(40); // size
            bos.write4Bytes(src.getWidth());
            bos.write4Bytes(2 * src.getHeight());
            bos.write2Bytes(1); // planes
            bos.write2Bytes(bitCount);
            bos.write4Bytes(0); // compression
            bos.write4Bytes(0); // image size
            bos.write4Bytes(0); // x pixels per meter
            bos.write4Bytes(0); // y pixels per meter
            bos.write4Bytes(0); // colors used, 0 = (1 << bitCount) (ignored)
            bos.write4Bytes(0); // colors important
            int bit_cache = 0;
            int bits_in_cache = 0;
            int row_padding = scanline_size - (bitCount * src.getWidth() + 7) / 8;
            for (int y = src.getHeight() - 1; y >= 0; y--) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    bos.write(0xff & argb);
                    bos.write(0xff & (argb >> 8));
                    bos.write(0xff & (argb >> 16));
                    bos.write(0xff & (argb >> 24));
                }
                for (int x = 0; x < row_padding; x++) {
                    bos.write(0);
                }
            }
            int t_row_padding = t_scanline_size - (src.getWidth() + 7) / 8;
            for (int y = src.getHeight() - 1; y >= 0; y--) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int alpha = 0xff & (argb >> 24);
                    bit_cache <<= 1;
                    if (alpha == 0) {
                        bit_cache |= 1;
                    }
                    bits_in_cache++;
                    if (bits_in_cache >= 8) {
                        bos.write(0xff & bit_cache);
                        bit_cache = 0;
                        bits_in_cache = 0;
                    }
                }
                if (bits_in_cache > 0) {
                    bit_cache <<= (8 - bits_in_cache);
                    bos.write(0xff & bit_cache);
                    bit_cache = 0;
                    bits_in_cache = 0;
                }
                for (int x = 0; x < t_row_padding; x++) {
                    bos.write(0);
                }
            }
        } finally {
            try {
                bos.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static class MyMemory extends Memory {
        private MyMemory(long size) {
            super(size);
        }

        @Override
        public synchronized void dispose() {
            super.dispose();
        }
    }

    private static final boolean initialized = initialize();

    private static boolean initialize() {
        try {
            final Ole32 ole32 = Ole32.INSTANCE;
            ole32.CoInitializeEx(Pointer.NULL, 0);
            final Guid.GUID CLSID_TaskbarList = Ole32Util.getGUIDFromString("{56FDF344-FD6D-11d0-958A-006097C9A090}");
            final Guid.GUID IID_ITaskbarList3 = Ole32Util.getGUIDFromString("{EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF}");
            final PointerByReference p = new PointerByReference();
            final WinNT.HRESULT hr = ole32.CoCreateInstance(CLSID_TaskbarList, Pointer.NULL, ObjBase.CLSCTX_ALL, IID_ITaskbarList3, p);
            if (!W32Errors.S_OK.equals(hr)) {
                LOG.info("Win7TaskBar CoCreateInstance(IID_ITaskbarList3) hResult: " + hr);
                return false;
            }
            myInterfacePointer = p.getValue();
            final Pointer vTablePointer = myInterfacePointer.getPointer(0);
            final Pointer[] vTable = new Pointer[TaskBarList_Methods];
            vTablePointer.read(0, vTable, 0, vTable.length);
            mySetProgressValue = Function.getFunction(vTable[TaskBarList_SetProgressValue], Function.ALT_CONVENTION);
            mySetProgressState = Function.getFunction(vTable[TaskBarList_SetProgressState], Function.ALT_CONVENTION);
            mySetOverlayIcon = Function.getFunction(vTable[TaskBarList_SetOverlayIcon], Function.ALT_CONVENTION);
            return true;
        } catch (Throwable e) {
            LOG.log(e);
        }
        return false;
    }

    public static void setProgress(JFrame frame, double value, boolean isOk) {
        if (!isEnabled()) {
            return;
        } else {
            final WinDef.HWND handle = getHandle(frame);
            if (handle != null) {
                mySetProgressState.invokeInt(new Object[] { myInterfacePointer, handle, isOk ? TBPF_NORMAL : TBPF_ERROR });
                mySetProgressValue.invokeInt(new Object[] { myInterfacePointer, handle, new WinDef.ULONGLONG((long) (value * 100)), TOTAL_PROGRESS });
            }
        }
    }

    private static boolean isEnabled() {
        return initialized;
    }

    public static void hideProgress(JFrame frame) {
        if (!isEnabled()) {
            return;
        } else {
            final HWND handle = getHandle(frame);
            if (handle != null) {
                mySetProgressState.invokeInt(new Object[] { myInterfacePointer, handle, TBPF_NOPROGRESS });
            }
        }
    }

    public static void setOverlayIcon(JFrame frame, Object icon, boolean dispose) {
        if (!isEnabled()) {
            return;
        }
        if (icon == null) {
            icon = Pointer.NULL;
        }
        final HWND handle = getHandle(frame);
        if (handle != null) {
            mySetOverlayIcon.invokeInt(new Object[] { myInterfacePointer, handle, icon, Pointer.NULL });
        }
        if (dispose) {
            User32.INSTANCE.DestroyIcon((WinDef.HICON) icon);
        }
    }

    public static Object createIcon(byte[] ico) {
        if (!isEnabled()) {
            return new Object();
        }
        MyMemory memory = new MyMemory(ico.length);
        try {
            memory.write(0, ico, 0, ico.length);
            int nSize = 100;
            int offset = User32Ex.INSTANCE.LookupIconIdFromDirectoryEx(memory, true, nSize, nSize, 0);
            if (offset != 0) {
                return User32Ex.INSTANCE.CreateIconFromResourceEx(memory.share(offset), DWORD_ZERO, true, ICO_VERSION, nSize, nSize, 0);
            }
            return null;
        } finally {
            memory.dispose();
        }
    }

    public static void attention(JFrame frame, boolean critical) {
        if (!isEnabled()) {
            return;
        } else {
            final HWND handle = getHandle(frame);
            if (handle != null) {
                User32Ex.INSTANCE.FlashWindow(handle, true);
            }
        }
    }

    private static WinDef.HWND getHandle(JFrame frame) {
        try {
            final ComponentPeer peer;
            if (JVMVersion.isMinimum(JVMVersion.JAVA19)) {
                peer = AWTAccessor.getComponentAccessor().getPeer(frame);
            } else {
                peer = ((Component) frame).getPeer();
            }
            final Method getHWnd = peer.getClass().getMethod("getHWnd");
            return new WinDef.HWND(new Pointer((Long) getHWnd.invoke(peer)));
        } catch (Throwable e) {
            LOG.log(e);
            return null;
        }
    }
}
