package org.jdownloader.startup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.appwork.app.launcher.parameterparser.CommandSwitch;
import org.appwork.app.launcher.parameterparser.CommandSwitchListener;
import org.appwork.app.launcher.parameterparser.ParameterParser;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.singleapp.InstanceMessageListener;
import org.jdownloader.logging.LogController;
import org.jdownloader.startup.commands.AbstractStartupCommand;
import org.jdownloader.startup.commands.AddContainerCommand;
import org.jdownloader.startup.commands.AddExtractionPasswordsCommand;
import org.jdownloader.startup.commands.AddLinkCommand;
import org.jdownloader.startup.commands.BRDebugCommand;
import org.jdownloader.startup.commands.GuiFocusCommand;
import org.jdownloader.startup.commands.GuiMinimizeCommand;
import org.jdownloader.startup.commands.HelpCommand;
import org.jdownloader.startup.commands.JACShowCommand;
import org.jdownloader.startup.commands.ReconnectCommand;
import org.jdownloader.updatev2.RestartController;

public class ParameterHandler implements InstanceMessageListener, CommandSwitchListener {
    private HashMap<String, StartupCommand> commandMap;
    private LogSource                       logger;
    private ArrayList<StartupCommand>       commands;
    private ParameterParser                 startupParameters;

    public ParameterHandler() {
        logger = LogController.getInstance().getLogger("StartupParameterHandler");
        commandMap = new HashMap<String, StartupCommand>();
        commands = new ArrayList<StartupCommand>();
        addCommand(new AddContainerCommand());
        addCommand(new AddExtractionPasswordsCommand());
        addCommand(new AddLinkCommand());
        addCommand(new GuiFocusCommand());
        addCommand(new GuiMinimizeCommand());
        addCommand(new HelpCommand(this));
        addCommand(new JACShowCommand());
        addCommand(new ReconnectCommand());
        addCommand(new BRDebugCommand());
        addCommand(new AbstractStartupCommand("n") {

            @Override
            public void run(String command, String... parameters) {
            }

            @Override
            public String getDescription() {
                return "Force a new Instance.";
            }

        });

        addCommand(new AbstractStartupCommand("console") {

            @Override
            public void run(String command, String... parameters) {

            }

            @Override
            public String getDescription() {
                return "Write all Logs to STDOUt or STDERR";
            }

        });
    }

    private void addCommand(StartupCommand helpCommand) {
        for (String s : helpCommand.getCommandSwitches()) {
            if (commandMap.containsKey(s)) throw new IllegalStateException("COmmand " + s + " already is used");
            commandMap.put(s, helpCommand);
        }
        commands.add(helpCommand);
    }

    @Override
    public void parseMessage(String[] args) {
        ParameterParser pp = new ParameterParser(args);
        pp.getEventSender().addListener(this);
        pp.parse(null);
    }

    @Override
    public void executeCommandSwitch(CommandSwitch event) {
        StartupCommand command = commandMap.get(event.getSwitchCommand());
        if (command != null && command.isRunningInstanceEnabled()) {
            command.run(event.getSwitchCommand(), event.getParameters());
        } else {
            logger.warning("Invalid Command: " + event.getSwitchCommand() + " - " + Arrays.toString(event.getParameters()));
        }
    }

    public List<StartupCommand> getCommands() {
        return commands;
    }

    public void onStartup(String[] args) {
        startupParameters = RestartController.getInstance().getParameterParser(args);

        CommandSwitchListener list = new CommandSwitchListener() {

            @Override
            public void executeCommandSwitch(CommandSwitch event) {
                StartupCommand command = commandMap.get(event.getSwitchCommand());
                if (command != null) {
                    command.run(event.getSwitchCommand(), event.getParameters());
                } else {
                    logger.warning("Invalid Command: " + event.getSwitchCommand() + " - " + Arrays.toString(event.getParameters()));
                }
            }

        };
        startupParameters.getEventSender().addListener(list);
        try {
            startupParameters.parse(null);
        } finally {
            startupParameters.getEventSender().removeListener(list);
        }

    }

}
