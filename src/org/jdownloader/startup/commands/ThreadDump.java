package org.jdownloader.startup.commands;

import java.util.Iterator;
import java.util.Map.Entry;

import org.appwork.utils.formatter.SizeFormatter;

public class ThreadDump extends AbstractStartupCommand {
    public ThreadDump() {
        super("threaddump");
    }

    @Override
    public void run(String command, String... parameters) {
        try {
            final java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            if (memory != null) {
                final StringBuilder sb = new StringBuilder();
                sb.append("HeapUsed:" + SizeFormatter.formatBytes(memory.getUsed()));
                sb.append("|HeapComitted:" + SizeFormatter.formatBytes(memory.getCommitted()));
                sb.append("|HeapMax:" + SizeFormatter.formatBytes(memory.getMax()));
                sb.append("\r\n");
                logger.severe(sb.toString());
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        final Iterator<Entry<Thread, StackTraceElement[]>> it = Thread.getAllStackTraces().entrySet().iterator();
        while (it.hasNext()) {
            final Entry<Thread, StackTraceElement[]> next = it.next();
            final Thread thread = next.getKey();
            final StringBuilder sb = new StringBuilder();
            sb.append("Thread:" + next.getKey().getName() + "|" + next.getKey().getId() + "|Daemon:" + thread.isDaemon() + "|Alive:" + thread.isAlive() + "\r\n");
            for (final StackTraceElement stackTraceElement : next.getValue()) {
                sb.append("\tat " + stackTraceElement + "\r\n");
            }
            logger.severe(sb.toString());
        }
        logger.flush();
    }

    @Override
    public String getDescription() {
        return "prints thread dump";
    }
}
