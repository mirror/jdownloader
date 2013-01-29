package org.jdownloader.startup.commands;

import org.jdownloader.startup.ParameterHandler;
import org.jdownloader.startup.StartupCommand;

public class HelpCommand extends AbstractStartupCommand {

    private static final String DIV = "==================================================================================================";
    private ParameterHandler    handler;

    public HelpCommand(ParameterHandler parameterHandler) {
        super("h", "help", "?");
        handler = parameterHandler;
    }

    @Override
    public boolean isRunningInstanceEnabled() {
        return false;
    }

    @Override
    public void run(String command, String... parameters) {

        final String[][] help = new String[][] { { "-s/--show\t", "Show JAC prepared captchas" }, { "-t/--train\t", "Train a JAC method" }, { "-C/--captcha <filepath or url> <method>", "Get code from image using JAntiCaptcha" }, { "-p/--add-password(s)", "Add passwords" }, { "-n --new-instance", "Force new instance if another jD is running" } };
        write(DIV);
        write(DIV);
        write("                         ---  JDownloader2 CommandLine Help  ---       ");
        write(DIV);
        write("http://www.jdownloader2.com");
        for (StartupCommand suc : handler.getCommands()) {
            write(suc.help());
        }
        for (final String helpLine[] : help) {
            write(helpLine[0] + "\t" + helpLine[1]);
        }
        write(DIV);
        write(DIV);
        System.exit(0);
    }

    private void write(String string) {
        System.out.println("   |" + string);
    }

    @Override
    public String getDescription() {
        return "Show this help";
    }

}
