package org.jdownloader.startup;

import org.jdownloader.startup.commands.AbstractStartupCommand;

public class DummyCommand extends AbstractStartupCommand {

    public DummyCommand(String... string) {
        super(string);
    }

    @Override
    public void run(String command, String... parameters) {
    }

    @Override
    public String getDescription() {
        return null;
    }

}
