package jd.gui.simpleSWT;

import java.awt.Dimension;
import java.awt.Toolkit;
import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Main {
    public static void main(String[] args) {
        showGUI();
    }


    public static void showGUI() {

        Display display = Display.getDefault();
        final Shell shell = new Shell(display);
        MainGui inst = new MainGui(shell, SWT.NULL);
        Point size = inst.getSize();
        shell.setText(JDSWTUtilities.getSWTResourceString("MainGui.name"));
        shell.setLayout(new FillLayout());
        Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
        shell.setSize(shellBounds.width, shellBounds.height);
        shell.addListener(SWT.Close, inst.guiListeners.initMainGuiCloseListener(shell));
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        shell.setLocation((d.width - inst.guiConfig.GUIsize[0]) / 2, (d.height - inst.guiConfig.GUIsize[1]) / 2);
        shell.open();
     

        if(inst.guiConfig.isMaximized)
        {
        shell.setLocation(shell.getLocation());
        shell.setMaximized(true);
        }
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

}
