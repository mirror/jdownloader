package jd.gui.swing.jdgui;

import java.io.IOException;
import java.net.URL;

import org.appwork.utils.Application;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.jdownloader.images.NewTheme;

public class SWTTest {

    public static void main(String[] args) throws IOException {
        Application.setApplication(".jd_home");
        Display display = new Display();
        Shell shell = new Shell(display);
        URL url = NewTheme.I().getImageUrl("logo/jd_logo_256_256");
        shell.setImage(new Image(display, url.openConnection().getInputStream()));
        // shell.open ();
        // fileDialog(shell);
        folderDialog(shell);
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    private static void folderDialog(Shell shell) {
        DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setFilterPath("c:\\"); // Windows specific
        System.out.println("RESULT=" + dialog.open());
    }

    public static void fileDialog(Shell shell) {
        FileDialog dialog = new FileDialog(shell, SWT.SAVE);

        String[] filterNames = new String[] { "Image Files", "All Files (*)" };
        String[] filterExtensions = new String[] { "*.gif;*.png;*.xpm;*.jpg;*.jpeg;*.tiff", "*" };
        String filterPath = "/";
        String platform = SWT.getPlatform();
        if (platform.equals("win32") || platform.equals("wpf")) {
            filterNames = new String[] { "Image Files", "All Files (*.*)" };
            filterExtensions = new String[] { "*.gif;*.png;*.bmp;*.jpg;*.jpeg;*.tiff", "*.*" };
            filterPath = "c:\\";
        }
        dialog.setFilterNames(filterNames);
        dialog.setFilterExtensions(filterExtensions);
        dialog.setFilterPath(filterPath);
        dialog.setFileName("myfile");
        System.out.println("Save to: " + dialog.open());
    }

}
