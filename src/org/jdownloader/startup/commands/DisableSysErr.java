package org.jdownloader.startup.commands;

import java.io.OutputStream;
import java.io.PrintStream;

public class DisableSysErr extends AbstractStartupCommand {

    public DisableSysErr() {
        super("noerr");
    }

    @Override
    public void run(String command, String... parameters) {

        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        }));
    }

    @Override
    public String getDescription() {
        return "Disable System.err";
    }

}
