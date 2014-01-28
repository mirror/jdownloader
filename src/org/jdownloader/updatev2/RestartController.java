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
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.logging.LogController;
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
        IGNORE_COMMAND_SWITCHES.add("forceupdate");
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

    private File            root = Application.getTemp().getParentFile();
    private LogSource       logger;

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

        if (root == null) root = Application.getTemp().getParentFile();

        log("Set Root: " + root.getAbsolutePath());
        this.root = root;
    }

    /**
     * Create a new instance of RestartController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    protected RestartController() {
        logger = LogController.getInstance().getLogger(RestartController.class.getName());
        final Restarter restarter = Restarter.getInstance(this);

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            {
                this.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                if (shutdownRequest instanceof RestartRequest) {
                    restarter.restart(getRoot(), getFilteredRestartParameters(((RestartRequest) shutdownRequest).getArguments()));
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

    public void directRestart(RestartRequest request) {
        log("Direct Restart: ");
        ShutdownController.getInstance().requestShutdown(request);
    }

    public void asyncRestart(final RestartRequest request) {
        log("Asynch Restart: ");
        new Thread("RestartAsynch") {
            public void run() {
                log("Synch Restart NOW");
                ShutdownController.getInstance().requestShutdown(request);
            }
        }.start();

    }

    protected void log(String string) {
        try {
            // this code may run in a shutdown hook. Classloading problems may be possible
            System.out.println(string);
            logger.info(string);
        } catch (Throwable e) {

        }
    }

    public void exitAsynch(final ShutdownRequest filter) {
        if (filter == null) throw new NullPointerException();
        new Thread("ExitAsynch") {
            public void run() {
                ShutdownController.getInstance().requestShutdown(filter);
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
    public void onShutdownVetoRequest(ShutdownRequest shutdownVetoExceptions) throws ShutdownVetoException {
        if (shutdownVetoExceptions.hasVetos()) { return; }
        if (shutdownVetoExceptions.isSilent()) return;
        try {
            if (shutdownVetoExceptions instanceof RestartRequest) {

                new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _UPDATE._.RestartController_confirmTorestart_title(), _UPDATE._.RestartController_confirmTorestart_msg(), AWUTheme.I().getIcon(IconKey.ICON_RESTART, 32), null, null) {

                    @Override
                    public String getDontShowAgainKey() {
                        return "Exit - Are you sure?";
                    }

                }.show().throwCloseExceptions();

            } else {
                // if you do not want to ask here, use
                // ShutdownController.getInstance().removeShutdownVetoListener(RestartController.getInstance());

                new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _UPDATE._.RestartController_confirmToExit_(), _UPDATE._.RestartController_confirmToExit_msg(), AWUTheme.I().getIcon(IconKey.ICON_EXIT, 32), null, null) {

                    @Override
                    public String getDontShowAgainKey() {
                        return "Exit - Are you sure?";
                    }

                }.show().throwCloseExceptions();

            }
        } catch (DialogNoAnswerException e) {
            throw new ShutdownVetoException("Really Exit question denied", this);
        }

    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

}
