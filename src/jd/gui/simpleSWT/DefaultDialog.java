package jd.gui.simpleSWT;

import jd.utils.JDSWTUtilities;

import org.eclipse.swt.*;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class DefaultDialog extends Dialog {
    private String message="",input="",inputBoxText="", inputBoxLbl="";
    private Image image;

    /**
     * DefaultDialog constructor
     * @param parent
     */
    public DefaultDialog(Shell parent) {
        // Pass the default styles here
        this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    /**
     * DefaultDialog constructor
     * @param parent
     * @param style
     */
    public DefaultDialog(Shell parent, int style) {
        // Let users override the default styles
        super(parent, style);
    }

    /**
     * setzt die Nachricht
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * setzt den Text neben der InputBox
     * @param inputBoxLbl
     */
    public void setInputBoxLbl(String inputBoxLbl) {
        this.inputBoxLbl = inputBoxLbl;
    }
    /**
     * setzt den Text der InputBox
     * @param text
     */
    public void setInputBoxText(String text) {
        inputBoxText = text;
    }

    /**
     * Gibt den Input aus
     * 
     * @return String
     */
    public String getInput() {
        return input;
    }

    public void setImage(Image image) {
        this.image = image;
    }
    /**
     * Oeffnet eine Dialog der den Input wiedergibt
     * 
     * @return String
     */
    public String open() {
        // Create the dialog window
        Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());
        createContents(shell);
        shell.pack();
        if (image != null)
            shell.setImage(image);
        shell.setLocation(getParent().getBounds().x + (getParent().getSize().x - shell.getSize().x) / 2, getParent().getBounds().y + (getParent().getSize().y - shell.getSize().y) / 2);

        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        // Return the entered value, or null
        return input;
    }

    /**
     * Den Inhalt erstellen
     * 
     * @param shell
     *            the dialog window
     */
    private void createContents(final Shell shell) {
        shell.setLayout(new GridLayout(2, true));


        CLabel label = new CLabel(shell, SWT.LEFT);
        label.setText(message);
        if (image != null)
            label.setImage(image);
        GridData data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        
        new CLabel(shell, SWT.LEFT).setText(inputBoxLbl);
        final Text text = new Text(shell, SWT.BORDER);
        text.setText(inputBoxText);
        text.selectAll();
        
        Button ok = new Button(shell, SWT.PUSH);
        ok.setText(JDSWTUtilities.getSWTResourceString("OK"));
        data = new GridData(GridData.FILL_HORIZONTAL);
        ok.setLayoutData(data);
        ok.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                input = text.getText();
                shell.close();
            }
        });
        Button cancel = new Button(shell, SWT.PUSH);
        cancel.setText(JDSWTUtilities.getSWTResourceString("Cancel"));
        data = new GridData(GridData.FILL_HORIZONTAL);
        cancel.setLayoutData(data);
        cancel.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                input = null;
                shell.close();
            }
        });
        shell.setDefaultButton(ok);
    }

}