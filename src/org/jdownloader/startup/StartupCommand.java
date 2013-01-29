package org.jdownloader.startup;

public interface StartupCommand {

    void run(String command, String... parameters);

    String[] getCommandSwitches();

    String help();

    boolean isRunningInstanceEnabled();

}
