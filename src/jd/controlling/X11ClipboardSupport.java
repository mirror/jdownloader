package jd.controlling;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Atom;
import com.sun.jna.platform.unix.X11.AtomByReference;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.Window;
import com.sun.jna.platform.unix.X11.WindowByReference;
import com.sun.jna.platform.unix.X11.XTextProperty;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

public class X11ClipboardSupport {
    public interface X11Extended extends X11 {
        X11Extended INSTANCE = Native.load("X11", X11Extended.class);

        X11.Window XGetSelectionOwner(Display display, Atom atom);
    }

    public static X11Extended x11 = X11Extended.INSTANCE;

    public static void main(String[] args) throws Exception {
        Display disp = x11.XOpenDisplay(null);
        if (disp == null) {
            return;
        }
        try {
            Thread.sleep(5000);
            X11.Atom atom_selection = x11.XInternAtom(disp, "CLIPBOARD", false);
            X11.Atom atom_net_wm_name = x11.XInternAtom(disp, "_NET_WM_NAME", false);
            X11.Atom atom_wm_name = x11.XInternAtom(disp, "WM_NAME", false);
            System.out.println(atom_selection);
            System.out.println(atom_net_wm_name);
            System.out.println(atom_wm_name);
            Window window = x11.XGetSelectionOwner(disp, atom_selection);
            System.out.println(window.longValue());// apps often create dummy unmapped window for clipboard handling
            Window[] sub = getSubwindows(disp, window);
            if (sub != null) {
                for (Window w : sub) {
                    System.out.println(w.longValue());
                }
            }
            Window parent = getParent(disp, window);
            System.out.println("parent " + (parent != null ? parent.longValue() : -1));
            Window root = getRoot(disp, window);
            System.out.println("root " + (root != null ? root.longValue() : -1));
            if (true) {
                X11.XTextProperty name = new X11.XTextProperty();
                x11.XGetWMName(disp, window, name);
                System.out.println(name.value);
                PointerByReference nameRef = new PointerByReference();
                try {
                    int status = x11.INSTANCE.XFetchName(disp, window, nameRef);
                    if (nameRef.getValue() != null) {
                        System.out.println(nameRef.getValue().getString(0));
                    }
                } finally {
                    if (nameRef.getValue() != null) {
                        x11.INSTANCE.XFree(nameRef.getValue());
                    }
                }
            }
            Window active = get_active_window(disp);
            System.out.println("pid(clipboard):" + get_window_pid(disp, window));
            System.out.println("pid(active):" + get_window_pid(disp, active));
            XTextProperty name = new X11.XTextProperty();
            x11.XGetWMName(disp, active, name);
            System.out.println("active:" + name.value);
            // readlink /proc/10452/exe
            // /proc/dsadada/cmdline
            System.out.println(get_property_as_utf8_string(disp, window, x11.XInternAtom(disp, "UTF8_STRING", false), "_NET_WM_NAME"));
            System.out.println(get_property_as_utf8_string(disp, window, Atom.None, "_NET_WM_NAME"));
            System.out.println(get_property_as_string(disp, window, X11.XA_STRING, "WM_NAME"));
            System.out.println(get_property_as_string(disp, window, Atom.None, "WM_NAME"));
            System.out.println(get_property_as_string(disp, window, X11.XA_STRING, "WM_CLIENT_MACHINE"));
            System.out.println(get_property_as_utf8_string(disp, active, x11.XInternAtom(disp, "UTF8_STRING", false), "_NET_WM_NAME"));
            System.out.println(get_property_as_utf8_string(disp, active, Atom.None, "_NET_WM_NAME"));
            System.out.println(get_property_as_string(disp, active, X11.XA_STRING, "WM_NAME"));
            System.out.println(get_property_as_string(disp, active, Atom.None, "WM_NAME"));
            System.out.println(get_property_as_string(disp, active, X11.XA_STRING, "WM_CLIENT_MACHINE"));
        } finally {
            x11.XCloseDisplay(disp);
        }
    }

    private static Window get_property_as_window(final Display disp, final Window win, final Atom xa_prop_type, final String prop_name) {
        Window ret = null;
        final Pointer prop = get_property(disp, win, xa_prop_type, prop_name, null);
        if (prop != null) {
            ret = new Window(prop.getLong(0));
            g_free(prop);
        }
        return ret;
    }

    public static Window get_active_window(final Display disp) {
        return get_property_as_window(disp, x11.XDefaultRootWindow(disp), X11.XA_WINDOW, "_NET_ACTIVE_WINDOW");
    }

    public static Window getParent(final Display disp, final Window win) throws Exception {
        WindowByReference root = new WindowByReference();
        WindowByReference parent = new WindowByReference();
        PointerByReference children = new PointerByReference();
        IntByReference childCount = new IntByReference();
        if (x11.XQueryTree(disp, win, root, parent, children, childCount) == 0) {
            throw new Exception("Can't query subwindows");
        }
        return parent.getValue();
    }

    public static Window getRoot(final Display disp, final Window win) throws Exception {
        WindowByReference root = new WindowByReference();
        WindowByReference parent = new WindowByReference();
        PointerByReference children = new PointerByReference();
        IntByReference childCount = new IntByReference();
        if (x11.XQueryTree(disp, win, root, parent, children, childCount) == 0) {
            throw new Exception("Can't query subwindows");
        }
        return root.getValue();
    }

    public static Window[] getSubwindows(final Display disp, final Window win) throws Exception {
        WindowByReference root = new WindowByReference();
        WindowByReference parent = new WindowByReference();
        PointerByReference children = new PointerByReference();
        IntByReference childCount = new IntByReference();
        if (x11.XQueryTree(disp, win, root, parent, children, childCount) == 0) {
            throw new Exception("Can't query subwindows");
        }
        if (childCount.getValue() == 0) {
            return null;
        }
        Window[] retVal = new Window[childCount.getValue()];
        // Depending on if we're running on 64-bit or 32-bit systems,
        // the Window ID size may be different; we need to make sure that
        // we get the data properly no matter what
        if (X11.XID.SIZE == 4) {
            int[] windows = children.getValue().getIntArray(0, childCount.getValue());
            for (int x = 0; x < retVal.length; x++) {
                X11.Window ret = new X11.Window(windows[x]);
                retVal[x] = ret;
            }
        } else {
            long[] windows = children.getValue().getLongArray(0, childCount.getValue());
            for (int x = 0; x < retVal.length; x++) {
                X11.Window ret = new X11.Window(windows[x]);
                retVal[x] = ret;
            }
        }
        x11.XFree(children.getValue());
        return retVal;
    }

    private static Integer get_property_as_int(final Display disp, final Window win, final Atom xa_prop_type, final String prop_name) {
        Integer intProp = null;
        final Pointer prop = get_property(disp, win, xa_prop_type, prop_name, null);
        if (prop != null) {
            intProp = prop.getInt(0);
            g_free(prop);
        }
        return intProp;
    }

    public static int get_window_pid(final Display disp, final Window win) {
        final Integer pid = get_property_as_int(disp, win, X11.XA_CARDINAL, "_NET_WM_PID");
        return (pid == null) ? -1 : pid.intValue();
    }

    private static String get_property_as_string(final Display disp, final Window win, final Atom xa_prop_type, final String prop_name) {
        String strProp = null;
        final Pointer prop = get_property(disp, win, xa_prop_type, prop_name, null);
        if (prop != null) {
            strProp = g_strdup(prop);
            g_free(prop);
        }
        return strProp;
    }

    private static void g_free(final Pointer pointer) {
        if (pointer != null) {
            x11.XFree(pointer);
        }
    }

    // https://github.com/wangzhengbo/JWMCtrl/blob/4a99de11ea5aa5d8b9846c3ea5c9445183f9270d/LICENSE
    private static final int MAX_PROPERTY_VALUE_LEN = 4096;

    private static String get_property_as_utf8_string(final Display disp, final Window win, final Atom xa_prop_type, final String prop_name) {
        String strProp = null;
        final Pointer prop = get_property(disp, win, xa_prop_type, prop_name, null);
        if (prop != null) {
            strProp = g_locale_to_utf8(prop);
            g_free(prop);
        }
        return strProp;
    }

    private static String g_strdup(final Pointer pointer) {
        final String value = pointer.getString(0);
        // g_free(pointer);
        return value;
    }

    private static String g_locale_to_utf8(final Pointer pointer) {
        return g_strdup(pointer);
    }

    public static Pointer get_property(final Display disp, final Window win, Atom xa_prop_type, final String prop_name, final NativeLongByReference size) {
        final AtomByReference xa_ret_type = new AtomByReference();
        final IntByReference ret_format = new IntByReference();
        final NativeLongByReference ret_nitems = new NativeLongByReference();
        final NativeLongByReference ret_bytes_after = new NativeLongByReference();
        final PointerByReference ret_prop = new PointerByReference();
        final Atom xa_prop_name = x11.XInternAtom(disp, prop_name, false);
        /*
         * MAX_PROPERTY_VALUE_LEN / 4 explanation (XGetWindowProperty manpage):
         *
         * long_length = Specifies the length in 32-bit multiples of the data to be retrieved.
         *
         * NOTE: see http://mail.gnome.org/archives/wm-spec-list/2003-March/msg00067.html In particular:
         *
         * When the X window system was ported to 64-bit architectures, a rather peculiar design decision was made, 32-bit quantities such
         * as Window IDs, atoms, etc, were kept as longs in the client side APIs, even when long was changed to 64 bit.
         */
        if (x11.XGetWindowProperty(disp, win, xa_prop_name, new NativeLong(0), new NativeLong(MAX_PROPERTY_VALUE_LEN / 4), false, xa_prop_type, xa_ret_type, ret_format, ret_nitems, ret_bytes_after, ret_prop) != X11.Success) {
            return null;
        }
        if ((xa_ret_type.getValue() == null) || (xa_prop_type != null && xa_ret_type.getValue().longValue() != xa_prop_type.longValue())) {
            g_free(ret_prop.getPointer());
            return null;
        }
        if (size != null) {
            long tmp_size = (ret_format.getValue() / 8) * ret_nitems.getValue().longValue();
            /* Correct 64 Architecture implementation of 32 bit data */
            if (ret_format.getValue() == 32) {
                tmp_size *= NativeLong.SIZE / 4;
            }
            size.setValue(new NativeLong(tmp_size));
        }
        return ret_prop.getValue();
    }
}
