package jd.controlling.reconnect.pluginsinc.extern;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.RouterPlugin;
import jd.controlling.reconnect.pluginsinc.extern.translate.T;
import jd.gui.UserIO;
import jd.gui.swing.components.ComboBrowseFile;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Hash;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.TextComponentChangeListener;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
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

    private ReconnectInvoker    invoker;

    public ExternReconnectPlugin() {
        super();
        icon = NewTheme.I().getIcon("console", 16);
        invoker = new ReconnectInvoker(this) {
            @Override
            protected void testRun() throws ReconnectException, InterruptedException {
                run();
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
                        logger.severe("Cannot create dummy Bat file, please delete all recon_*.bat files in tmp folder!");
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
            public void run() throws ReconnectException {
                final int waitForReturn = getWaitForReturn();
                final String command = getCommand();
                if (command.length() == 0) { throw new ReconnectException("Command Invalid: " + command); }

                final File f = new File(command);
                if (!f.exists()) { throw new ReconnectException("Command does not exist: " + f.getAbsolutePath());

                }

                final String t = f.getAbsolutePath();
                final String executeIn = t.substring(0, t.indexOf(f.getName()) - 1).trim();
                if (CrossSystem.isWindows() && isDummyBatchEnabled()) {
                    /*
                     * for windows we create a temporary batchfile that calls our external tool and redirect its streams to nul
                     */
                    final File bat = getDummyBat();
                    if (bat == null) { throw new ReconnectException("Could not create Dummy Batch");

                    }
                    BufferedWriter output = null;
                    try {
                        output = new BufferedWriter(new FileWriter(bat));
                        if (executeIn.contains(" ")) {
                            output.write("cd \"" + executeIn + "\"\r\n");
                        } else {
                            output.write("cd " + executeIn + "\r\n");
                        }
                        final String parameter = getParameterString();
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
                        logger.log(e);
                        throw new ReconnectException(e);
                    } finally {
                        try {
                            output.close();
                        } catch (final Throwable e) {
                        }
                    }
                    logger.finer("Execute Returns: " + JDUtilities.runCommand(bat.toString(), new String[0], executeIn, waitForReturn));
                } else {
                    /* other os, normal handling */
                    final String parameter = getParameterString();
                    logger.finer("Execute Returns: " + JDUtilities.runCommand(command, org.appwork.utils.Regex.getLines(parameter), executeIn, waitForReturn));
                }
            }
        };
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

    public static void main(String[] args) {
        ExtFileChooserDialog d = new ExtFileChooserDialog(0, "", null, null);
        d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        try {
            Dialog.getInstance().showDialog(d);

            System.out.println(Hash.getMD5(d.getSelectedFile()));

            Browser br = new Browser();

            URLConnectionAdapter con = br.openPostConnection("http://highresaudio.com/download.php", "userData={\"response_status\":\"JSONOBJECT\",\"userID\":\"72\",\"surname\":\"Thomas\",\"lastname\":\"Rechenmacher\",\"sessionID\":\"0270650278cf074bcd51152f2c859bc90a538095\"}&paidID=11203&formatID=30737&trackID=58241&");

            br.download(new File(d.getSelectedFile().getAbsolutePath() + "_2"), con);

            System.out.println("Finished: " + Hash.getMD5(new File(d.getSelectedFile().getAbsolutePath() + "_2")));
        } catch (DialogClosedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public JComponent getGUI() {

        final JPanel p = new JPanel(new MigLayout("ins 0,wrap 2", "[][grow,fill]", "[][][grow,fill][]"));
        p.setOpaque(false);
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
     * returns how long the execution should wait for the extern process to return
     * 
     * @return
     */
    private int getWaitForReturn() {

        return this.getStorage().get(ExternReconnectPlugin.WAIT_FOR_RETURN_SECONDS, JDUtilities.getConfiguration().getIntegerProperty("WAIT_FOR_RETURN5", 0));

    }

    /**
     * Returns of dummy batch usage is enabled. for windows we create a temporary batchfile that calls our external tool and redirect its
     * streams to nul. This is a workaround for many IO STream caused errors
     * 
     * @return
     */
    private boolean isDummyBatchEnabled() {

        return this.getStorage().get(ExternReconnectPlugin.DUMMY_BATCH_ENABLED, JDUtilities.getConfiguration().getBooleanProperty("PROPERTY_RECONNECT_DUMMYBAT", true));
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

    @Override
    public ReconnectInvoker getReconnectInvoker() {
        return invoker;
    }

}