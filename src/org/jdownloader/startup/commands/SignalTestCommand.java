package org.jdownloader.startup.commands;

import java.util.HashMap;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class SignalTestCommand extends AbstractStartupCommand implements sun.misc.SignalHandler {

    public SignalTestCommand() {
        super("signal");

    }

    // http://twit88.com/blog/2008/02/06/java-signal-handling/
    @Override
    public void run(String command, String... parameters) {
        logger.info("Rescan Plugins and Extensions");

        reg("SEGV");
        reg("ILL");
        reg("FPE");
        reg("BUS");
        reg("SYS");
        reg("CPU");
        reg("FSZ");
        reg("ABRT");
        reg("INT");
        reg("TERM");
        reg("HUP");
        reg("USR1");
        reg("QUIT");
        reg("BREAK");
        reg("TRAP");
        reg("PIPE");
        reg("ABRT");

    }

    private HashMap<String, SignalHandler> oldHandlers = new HashMap<String, SignalHandler>();

    private void reg(String signal) {
        try {
            Signal diagSignal = new Signal(signal);
            oldHandlers.put(signal, Signal.handle(diagSignal, this));
            logger.info("Can handle: " + signal);
        } catch (Exception e) {
            logger.info("Cannot handle: " + signal);
        }

    }

    @Override
    public String getDescription() {
        return "Test to handle os signals";
    }

    public void handle(Signal signal) {
        logger.info("Signal handler called for signal " + signal);
        try {

            signalAction(signal);

            SignalHandler oldHandler = oldHandlers.get(signal.getName());
            // Chain back to previous handler, if one exists
            if (oldHandler != null && oldHandler != SIG_DFL && oldHandler != SIG_IGN) {
                oldHandler.handle(signal);
            }

        } catch (Exception e) {

            logger.log(e);
        }
    }

    public void signalAction(Signal signal) {
        logger.info("Handling " + signal.getName());
        logger.info("Just sleep for 5 seconds.");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.log(e);

        }
    }

}
