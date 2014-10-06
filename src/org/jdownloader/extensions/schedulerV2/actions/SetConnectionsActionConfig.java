package org.jdownloader.extensions.schedulerV2.actions;

public class SetConnectionsActionConfig implements IScheduleActionConfig {
    public SetConnectionsActionConfig() {
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    private int connections = 0;

}
