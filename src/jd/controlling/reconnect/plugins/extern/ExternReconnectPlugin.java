package jd.controlling.reconnect.plugins.extern;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;

import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.plugins.extern.translate.T;
import jd.gui.UserIO;
import jd.gui.swing.components.ComboBrowseFile;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.TextComponentChangeListener;
import org.jdownloader.images.NewTheme;

/**
 * Plugin to use an extern tool for reconnection
 */
public class ExternReconnectPlugin extends RouterPlugin implements ActionListener {

    private static final String COMMAND                 = "COMMAND";

    private static final String DUMMY_BATCH_ENABLED     = "DUMMY_BATCH_ENABLED";

    private static final String WAIT_FOR_RETURN_SECONDS = "WAIT_FOR_RETURN_SECONDS";

    private static final String PARAMETER               = "PARAMETER";

    public static final String  ID                      = "ExternReconnect";

    private JTextPane           txtParameter;

    private JCheckBox           chbDummyBatch;

    private ComboBrowseFile     browse;

    private ImageIcon           icon;

    public ExternReconnectPlugin() {
        super();
        icon = NewTheme.I().getIcon("console", 16);
    }

    @Override
    public ImageIcon getIcon16() {
        return icon;
    }

    private Storage getStorage() {
        return JSonStorage.getPlainStorage(this.getID());
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.browse) {
            this.setCommand(this.browse.getText());
        } else {
            this.setDummyBatchEnabled(this.chbDummyBatch.isSelected());
        }
    }

    /**
     * Returns the path to the tool
     * 
     * @return
     */
    private String getCommand() {
        // TODO Auto-generated method stub
        return this.getStorage().get(ExternReconnectPlugin.COMMAND, JDUtilities.getConfiguration().getStringProperty("InteractionExternReconnect_Command", ""));
    }

    /**
     * get next available DummyBat for reconnect
     * 
     * @return
     */
    private File getDummyBat() {
        int number = 0;
        while (true) {
            if (number == 100) {
                RouterPlugin.LOG.severe("Cannot create dummy Bat file, please delete all recon_*.bat files in tmp folder!");
                return null;
            }
            final File tmp = JDUtilities.getResourceFile("tmp/recon_" + number + ".bat", true);
            if (tmp.exists()) {
                if (tmp.delete()) { return tmp; }
                tmp.deleteOnExit();
            } else {
                return tmp;
            }
            number++;
        }
    }

    @Override
    public JComponent getGUI() {

        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "[][grow,fill]", "[][][grow,fill][]"));
        this.browse = new ComboBrowseFile(this.getID());
        this.browse.setEditable(true);
        this.browse.setFileSelectionMode(UserIO.FILES_ONLY);
        this.browse.setDialogType(JFileChooser.OPEN_DIALOG);
        this.txtParameter = new JTextPane();
        this.chbDummyBatch = new JCheckBox();

        p.add(new JLabel(T._.interaction_externreconnect_command()), "sg left");
        p.add(this.browse);
        this.browse.addActionListener(this);
        p.add(new JLabel(T._.interaction_externreconnect_parameter()), "newline,spanx,sg left");
        p.add(new JScrollPane(this.txtParameter), "spanx,newline,pushx,growx");
        if (CrossSystem.isWindows()) {
            p.add(new JLabel(T._.interaction_externreconnect_dummybat()), "newline,sg left");
            p.add(this.chbDummyBatch);
        }
        this.chbDummyBatch.addActionListener(this);

        new TextComponentChangeListener(this.txtParameter) {
            @Override
            protected void onChanged(final DocumentEvent e) {

                ExternReconnectPlugin.this.setParameter(ExternReconnectPlugin.this.txtParameter.getText());

            }
        };
        this.updateGUI();
        return p;
    }

    @Override
    public String getID() {
        return ExternReconnectPlugin.ID;
    }

    @Override
    public String getName() {
        return T._.jd_controlling_reconnect_plugins_extern_ExternReconnectPlugin_getName();
    }

    /**
     * Returns parameterstring. each line belongs to one parameter
     * 
     * @return
     */
    private String getParameterString() {

        return this.getStorage().get(ExternReconnectPlugin.PARAMETER, JDUtilities.getConfiguration().getStringProperty("EXTERN_RECONNECT__PARAMETER"));

    }

    /**
     * returns how long the execution should wait for the extern process to
     * return
     * 
     * @return
     */
    private int getWaitForReturn() {

        return this.getStorage().get(ExternReconnectPlugin.WAIT_FOR_RETURN_SECONDS, JDUtilities.getConfiguration().getIntegerProperty("WAIT_FOR_RETURN5", 0));

    }

    /**
     * Returns of dummy batch usage is enabled. for windows we create a
     * temporary batchfile that calls our external tool and redirect its streams
     * to nul. This is a workaround for many IO STream caused errors
     * 
     * @return
     */
    private boolean isDummyBatchEnabled() {

        return this.getStorage().get(ExternReconnectPlugin.DUMMY_BATCH_ENABLED, JDUtilities.getConfiguration().getBooleanProperty("PROPERTY_RECONNECT_DUMMYBAT", true));
    }

    @Override
    public boolean isReconnectionEnabled() {
        return true;
    }

    @Override
    protected void performReconnect() throws ReconnectException {
        final int waitForReturn = this.getWaitForReturn();
        final String command = this.getCommand();
        if (command.length() == 0) { throw new ReconnectException("Command Invalid: " + command); }

        final File f = new File(command);
        if (!f.exists()) { throw new ReconnectException("Command does not exist: " + f.getAbsolutePath());

        }

        final String t = f.getAbsolutePath();
        final String executeIn = t.substring(0, t.indexOf(f.getName()) - 1).trim();
        if (CrossSystem.isWindows() && this.isDummyBatchEnabled()) {
            /*
             * for windows we create a temporary batchfile that calls our
             * external tool and redirect its streams to nul
             */
            final File bat = this.getDummyBat();
            if (bat == null) { throw new ReconnectException("Could not create Dummy Batch");

            }
            try {
                final BufferedWriter output = new BufferedWriter(new FileWriter(bat));
                if (executeIn.contains(" ")) {
                    output.write("cd \"" + executeIn + "\"\r\n");
                } else {
                    output.write("cd " + executeIn + "\r\n");
                }
                final String parameter = this.getParameterString();
                final String[] params = org.appwork.utils.Regex.getLines(parameter);
                final StringBuilder sb = new StringBuilder(" ");
                for (final String param : params) {
                    sb.append(param);
                    sb.append(" ");
                }
                if (executeIn.contains(" ")) {
                    output.write("\"" + command + "\"" + sb.toString() + " >nul 2>nul");
                } else {
                    output.write(command + " " + sb.toString() + ">nul 2>nul");
                }
                output.close();
            } catch (final Exception e) {
                JDLogger.exception(e);
                throw new ReconnectException(e);

            }
            RouterPlugin.LOG.finer("Execute Returns: " + JDUtilities.runCommand(bat.toString(), new String[0], executeIn, waitForReturn));
        } else {
            /* other os, normal handling */
            final String parameter = this.getParameterString();
            RouterPlugin.LOG.finer("Execute Returns: " + JDUtilities.runCommand(command, org.appwork.utils.Regex.getLines(parameter), executeIn, waitForReturn));
        }
    }

    private void setCommand(final String text) {
        this.getStorage().put(ExternReconnectPlugin.COMMAND, text);
        this.updateGUI();
    }

    private void setDummyBatchEnabled(final boolean selected) {
        this.getStorage().put(ExternReconnectPlugin.DUMMY_BATCH_ENABLED, selected);
        this.updateGUI();
    }

    private void setParameter(final String text) {
        this.getStorage().put(ExternReconnectPlugin.PARAMETER, text);
        this.updateGUI();
    }

    private void updateGUI() {
        new EDTRunner() {
            protected void runInEDT() {
                try {
                    ExternReconnectPlugin.this.browse.setText(ExternReconnectPlugin.this.getCommand());
                } catch (final IllegalStateException e) {
                    // throws an java.lang.IllegalStateException if the caller
                    // is a changelistener of this field's document
                }
                try {
                    ExternReconnectPlugin.this.txtParameter.setText(ExternReconnectPlugin.this.getParameterString());
                } catch (final IllegalStateException e) {
                    // throws an java.lang.IllegalStateException if the caller
                    // is a changelistener of this field's document
                }
                try {
                    ExternReconnectPlugin.this.chbDummyBatch.setSelected(ExternReconnectPlugin.this.isDummyBatchEnabled());
                } catch (final IllegalStateException e) {
                    // throws an java.lang.IllegalStateException if the caller
                    // is a changelistener of this field's document
                }
            }

        };

    }

}