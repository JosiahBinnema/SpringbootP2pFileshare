package com.springboot;

/**
 * This is just a class to define HostInfo. Convenient for passing around details to make a socket connection.
 */
public class HostInfo {
    private String hostUsername;
    private String hostIp;
    private int hostPort;

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getHostPort() {
        return hostPort;
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
    }
}
