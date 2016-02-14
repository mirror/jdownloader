package org.jdownloader.jna.windows;

//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {
    public static Kernel32 INSTANCE = (Kernel32) com.sun.jna.Native.loadLibrary("kernel32", Kernel32.class);

    void SetThreadExecutionState(int value);

    int ES_DISPLAY_REQUIRED = 0x00000002;
    int ES_SYSTEM_REQUIRED  = 0x00000001;
    int ES_CONTINUOUS       = 0x80000000;

}
