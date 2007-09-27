package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

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

public class SWTWarnings extends Dialog {
    private Image image;
    private String name;
    private GUIConfig guiConfig;
    public SWTWarnings(Shell parent, GUIConfig guiConfig, String name, int style) {
        super(parent, style);
        image = parent.getDisplay().getSystemImage(SWT.ICON_WARNING);
        this.name = name;
        this.guiConfig = guiConfig;
    }

    public SWTWarnings(Shell parent, GUIConfig guiConfig, String name) {
        this(parent, guiConfig, name, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    public void open() {
        if(!guiConfig.warnings.containsKey(name))
            guiConfig.warnings.put(name, false);
        else if(guiConfig.warnings.get(name)) return;
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
        label.setText(JDSWTUtilities.getSWTResourceString("SWTWarnings."+name+".text"));

        final Button checkButton = new Button(shell, SWT.CHECK);
        checkButton.setText(JDSWTUtilities.getSWTResourceString("SWTWarnings.checkButton.name"));
        checkButton.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                guiConfig.warnings.put(name, checkButton.getSelection());
            }
        });

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
        checkButton.addListener(SWT.KeyDown, new Listener(){
            public void handleEvent(Event e) {
                if (e.keyCode == JDSWTUtilities.getSWTResourceMnemChar(checkButton.getText()))
                    checkButton.setSelection(!checkButton.getSelection());
                else if (e.keyCode == JDSWTUtilities.getSWTResourceMnemChar(ok.getText()))
                    shell.dispose();
                
            }
            
        });
        shell.setDefaultButton(ok);
    }
}
