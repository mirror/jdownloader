package org.jdownloader.nativ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NativeProcessBuilder {

    private final List<String> command;

    // private File directory;
    // private Map<String, String> environment;
    // private boolean redirectErrorStream;

    public NativeProcessBuilder(final List<String> command) {
        if (command == null) { throw new NullPointerException(); }
        this.command = command;
    }

    public NativeProcessBuilder(final String... command) {
        this.command = new ArrayList<String>(command.length);
        for (final String arg : command) {
            this.command.add(arg);
        }
    }

    public NativeProcess start() throws IOException {
        return null;
    }
}
