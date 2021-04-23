package jd.gui.swing;

import java.awt.Image;

import org.appwork.utils.JVMVersion;
import org.appwork.utils.ReflectionUtils;

public class MacOSApplicationAdapter {
    public static void setDockIcon(final Image icon) {
        try {
            if (JVMVersion.isMinimum(JVMVersion.JAVA_9)) {
                final String className = "java.awt.Taskbar";
                final boolean isTaskbarSupported = ReflectionUtils.invoke(className, "isTaskbarSupported", null, boolean.class);
                if (isTaskbarSupported) {
                    // check for Taskbar.Feature.ICON_IMAGE
                    final Object taskBar = ReflectionUtils.invoke(className, "getTaskbar", null, Class.forName(className));
                    ReflectionUtils.invoke(taskBar.getClass(), "setIconImage", taskBar, void.class, icon);
                    return;
                }
            }
            com.apple.eawt.Application.getApplication().setDockIconImage(icon);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}
