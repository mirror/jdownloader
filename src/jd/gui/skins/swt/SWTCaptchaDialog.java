package jd.gui.skins.swt;


import java.io.File;

import jd.plugins.Plugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class SWTCaptchaDialog extends Dialog {
    
    private Image image;
    public SWTCaptchaDialog(Shell parent, Plugin plugin, File file, int style) {
        super(parent, style);
        image = parent.getDisplay().getSystemImage(SWT.ICON_WARNING);
    }

    public SWTCaptchaDialog(Shell parent, Plugin plugin, File file) {
        this(parent, plugin, file, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    public void open() {
        Shell shell = new Shell(getParent(), getStyle());
        shell.setText(JDSWTUtilities.getSWTResourceString("SWTWarnings.name"));
        shell.setImage(image);
        createContents(shell);
        shell.pack();
        shell.setLocation(getParent().getBounds().x + (getParent().getSize().x - shell.getSize().x) / 2, getParent().getBounds().y + (getParent().getSize().y - shell.getSize().y) / 2);
        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void createContents(final Shell shell) {

        GridLayout thisLayout = new GridLayout(1, false);
        thisLayout.marginHeight = 5;
        thisLayout.horizontalSpacing = 5;
        thisLayout.marginWidth = 5;
        thisLayout.verticalSpacing = 5;
        shell.setLayout(thisLayout);

        CLabel label = new CLabel(shell, SWT.LEFT);
        label.setImage(image);

        final Button ok = new Button(shell, SWT.PUSH);
        ok.setText(JDSWTUtilities.getSWTResourceString(JDSWTUtilities.getSWTResourceString("SWTWarnings.ok.name")));
        ok.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                shell.dispose();
            }
        });

        GridData data = new GridData();
        ok.pack();
        data.widthHint = ok.getSize().x + 50;
        data.horizontalAlignment = GridData.CENTER;
        ok.setLayoutData(data);

        shell.setDefaultButton(ok);
    }
    
}
