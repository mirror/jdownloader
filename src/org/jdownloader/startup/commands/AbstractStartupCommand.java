package org.jdownloader.startup.commands;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.startup.StartupCommand;

public abstract class AbstractStartupCommand implements StartupCommand {

    protected final String[]  commands;
    protected final LogSource logger;

    public AbstractStartupCommand(String... commands) {
        this.commands = commands;
        logger = LogController.getFastPluginLogger(getClass().getName());
    }

    @Override
    public abstract void run(String command, String... parameters);

    @Override
    public String[] getCommandSwitches() {
        return commands;
    }

    @Override
    public boolean isRunningInstanceEnabled() {
        return true;
    }

    @Override
    public String help() {
        final StringBuilder sb = new StringBuilder();
        for (final String s : getCommandSwitches()) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            if (StringUtils.isNotEmpty(s)) {
                sb.append("-").append(s);
            }
        }
        if (getParameterHelp() != null) {
            sb.append(" ").append(getParameterHelp());
        } else {
            sb.append("\t");
        }
        if (StringUtils.isNotEmpty(getDescription())) {
            sb.append(" ");
            sb.append(getDescription());
        }
        return sb.toString();
    }

    abstract public String getDescription();

    public String getParameterHelp() {
        return null;
    }
}
