package jd.wizardry7.view;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

abstract public class DefaultWizardPage extends JPanel {

    private static final long serialVersionUID = 34944600760403685L;

    /**
     * Sometimes it is necessary to share data between WizardPages - e.g. one
     * page creates a list of files, which have to be uploaded by the next page.
     */
    protected static Object dataStoreBetweenWizardPages;

    private Component footer;
    private Component header;

    protected DefaultWizardPage() {
        super(new BorderLayout(0, 0));
        initComponents();
        this.add(createBody(), BorderLayout.CENTER);
    }

    protected DefaultWizardPage(Object doNothing) {
        super(new BorderLayout(0, 0));
    }

    public String backwardValidation() {
        System.out.println("backwardValidation");
        return "";
    }

    public String cancelValidation() {
        System.out.println("cancelValidation");
        return "";
    }

    protected abstract Component createBody();

    public void enterWizardPageAfterForward() {
    }

    public void exitWizardPage() {
    }

    public String forwardValidation() {
        System.out.println("forwardValidation");
        return "";
    }

    public Component getFooter() {
        return footer;
    }

    public Component getHeader() {
        return header;
    }

    protected void initComponents() {
    }

    public void setFooter(Component footer) {
        this.footer = footer;
        this.add(this.footer, BorderLayout.SOUTH);
    }

    public void setHeader(Component header) {
        this.header = header;
        this.add(this.header, BorderLayout.NORTH);
    }

}
