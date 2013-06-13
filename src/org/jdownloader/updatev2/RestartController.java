package org.jdownloader.updatev2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.resources.AWUTheme;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoFilter;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.ConfirmDialogInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.updatev2.restart.Restarter;

public class RestartController implements ShutdownVetoListener {
    private static final RestartController INSTANCE                = new RestartController();
    private final static HashSet<String>   IGNORE_COMMAND_SWITCHES = new HashSet<String>();
    static {
        IGNORE_COMMAND_SWITCHES.add("update");
        IGNORE_COMMAND_SWITCHES.add("selftest");
        IGNORE_COMMAND_SWITCHES.add("selfupdateerror");
        IGNORE_COMMAND_SWITCHES.add("afterupdate");
        IGNORE_COMMAND_SWITCHES.add("restart");
    }

    /**
     * get the only existing instance of RestartController. This is a singleton
     * 
     * @return
     */
    public static RestartController getInstance() {
        return RestartController.INSTANCE;
    }

    private ParameterParser startupParameters;

    private File            root = Application.getResource("tmp").getParentFile();

    // public String updaterJar = "Updater.jar";
    //
    // public String exe = "JDownloader.exe";
    //
    // public String jar = "JDownloader.jar";
    //
    // public String app = "JDownloader.app";

    // private String[] startArguments;

    // private boolean silentShutDownEnabled = false;

    public void setRoot(File root) {
        if (root == null) root = Application.getResource("tmp").getParentFile();
        this.root = root;
    }

    /**
     * Create a new instance of RestartController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    protected RestartController() {

        final Restarter restarter = Restarter.getInstance(this);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            {
                this.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void onShutdown(final Object shutdownRequest) {
                if (shutdownRequest instanceof RestartShutdownRequest) {
                    restarter.restart(getRoot(), getFilteredRestartParameters(((RestartShutdownRequest) shutdownRequest).getArguments()));
                }
            }
        });

    }

    private File getRoot() {
        return root;
    }

    public List<String> getFilteredRestartParameters(String... arguments) {
        ArrayList<String> ret = new ArrayList<String>();
        if (startupParameters != null) {
            for (Entry<String, CommandSwitch> es : startupParameters.getMap().entrySet()) {
                if (IGNORE_COMMAND_SWITCHES.contains(es.getKey() == null ? null : es.getKey().toLowerCase(Locale.ENGLISH))) continue;

                if (es.getKey() != null) ret.add("-" + es.getKey());
                for (String p : es.getValue().getParameters()) {
                    ret.add(p);
                }
            }
        }
        if (arguments != null) {
            for (String s : arguments) {
                if (s != null) ret.add(s);
            }
        }

        return ret;
    }

    public void directRestart(final ShutdownVetoFilter filter, String... strings) {
        Log.L.info("direct Restart");
        ShutdownController.getInstance().requestShutdown(false, filter, new RestartShutdownRequest(strings));
    }

    public void asyncRestart(final ShutdownVetoFilter filter, final String... strings) {
        new Thread("RestartAsynch") {
            public void run() {
                Log.L.info("asyn Restart");
                ShutdownController.getInstance().requestShutdown(false, filter, new RestartShutdownRequest(strings));
            }
        }.start();

    }

    public void exitAsynch(final ShutdownVetoFilter filter) {
        new Thread("ExitAsynch") {
            public void run() {
                ShutdownController.getInstance().requestShutdown(false, filter);
            }
        }.start();
    }

    public synchronized ParameterParser getParameterParser(String[] args) {
        if (startupParameters == null) {
            if (args == null) throw new IllegalStateException();
            startupParameters = new ParameterParser(args);
        }
        if (args != null) {
            startupParameters.setRawArguments(args);
        }
        return startupParameters;
    }

    @Override
    public void onShutdown(boolean silent) {
    }

    @Override
    public void onShutdownVeto(ShutdownVetoException[] shutdownVetoExceptions) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
        if (shutdownVetoExceptions.length > 0) { return; }
        try {

            // if you do not want to ask here, use
            // ShutdownController.getInstance().removeShutdownVetoListener(RestartController.getInstance());
            ConfirmDialog cd = new ConfirmDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _UPDATE._.RestartController_confirmToExit_(), _UPDATE._.RestartController_confirmToExit_msg(), AWUTheme.I().getIcon("exit", 32), null, null) {

                @Override
                public String getDontShowAgainKey() {
                    return "Exit - Are you sure?";
                }

            };

            UIOManager.I().show(ConfirmDialogInterface.class, cd);
            cd.checkCloseReason();
        } catch (DialogNoAnswerException e) {
            throw new ShutdownVetoException("Really Exit question denied", this);
        }

    }

    @Override
    public void onSilentShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

}
