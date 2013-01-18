package org.jdownloader.updatev2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.logging.Log;
import org.jdownloader.updatev2.restart.Restarter;

public class RestartController {
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

    private Actions         toDo;
    private Restarter       restarter;
    private ParameterParser startupParameters;
    private String[]        arguments;

    // public String updaterJar = "Updater.jar";
    //
    // public String exe = "JDownloader.exe";
    //
    // public String jar = "JDownloader.jar";
    //
    // public String app = "JDownloader.app";

    // private String[] startArguments;

    // private boolean silentShutDownEnabled = false;

    /**
     * Create a new instance of RestartController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    protected RestartController() {

        // ShutdownController.getInstance().addShutdownVetoListener(this);

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            {
                this.setHookPriority(Integer.MIN_VALUE);
            }

            @Override
            public void run() {
                finalHook();
            }
        });
        restarter = Restarter.getInstance(this);

    }

    protected void finalHook() {

        if (toDo == null) return;
        switch (toDo) {
        case RESTART:
            restarter.restart(getFilteredRestartParameters());
            break;
        }
    }

    public List<String> getFilteredRestartParameters() {
        ArrayList<String> ret = new ArrayList<String>();
        if (startupParameters != null) {
            for (Entry<String, CommandSwitch> es : startupParameters.getMap().entrySet()) {
                if (IGNORE_COMMAND_SWITCHES.contains(es.getKey().toLowerCase(Locale.ENGLISH))) continue;

                ret.add("-" + es.getKey());
                for (String p : es.getValue().getParameters()) {

                    ret.add(p);
                }
            }
        }
        if (arguments != null) {
            for (String s : arguments) {
                ret.add(s);
            }
        }

        return ret;
    }

    private enum Actions {
        RESTART;
    }

    public void directRestart(String... strings) {

        toDo = Actions.RESTART;
        arguments = strings;
        Log.L.info("direct Restart");
        // ShutdownController.getInstance().removeShutdownEvent(SilentUpdaterEvent.getInstance());
        // ShutdownController.getInstance().removeShutdownEvent(RestartDirectEvent.getInstance());
        // ShutdownController.getInstance().removeShutdownEvent(RestartViaUpdaterEvent.getInstance());

        // RlyExitListener.getInstance().setText(_UPDATE._.rlyrestart());
        // RlyExitListener.getInstance().setTitle(_UPDATE._.rlyrestart_restart());
        //
        // ShutdownController.getInstance().addShutdownEvent(RestartDirectEvent.getInstance());

        ShutdownController.getInstance().requestShutdown(false);
    }

    public void exitAsynch() {
        new Thread("ExitAsynch") {
            public void run() {
                ShutdownController.getInstance().requestShutdown();
            }
        }.start();

    }

    public synchronized ParameterParser getParameterParser(String[] args) {
        if (startupParameters == null) {
            startupParameters = new ParameterParser(args);
        }
        return startupParameters;
    }

    // public String getRestartCommandLine() {
    // final StringBuilder sb = new StringBuilder();
    // Log.L.info("Restart Parameters");
    // for (final String s : this.getRestartParameters()) {
    // if (sb.length() > 0) {
    // sb.append(" ");
    // }
    //
    // Log.L.info(s);
    // if (s.contains(" ")) {
    // sb.append("\"");
    // sb.append(s);
    // sb.append("\"");
    // } else {
    // sb.append(s);
    // }
    //
    // }
    //
    // return sb.toString();
    // }
    //

}
