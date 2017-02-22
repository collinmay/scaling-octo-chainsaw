package whs.common.net;

/**
 * Created by misson20000 on 2/13/17.
 */
public class Handshake {
    private String message = "handshake";
    private String protocolName;
    private String protocolVersion;

    public Handshake() {
    }

    public Handshake(String protocolName, String protocolVersion) {
        this.protocolName = protocolName;
        this.protocolVersion = protocolVersion;
    }

    public String toString() {
        return protocolName + " " + protocolVersion;
    }

    public boolean equals(Object o) {
        return o instanceof Handshake && ((Handshake) o).protocolVersion.equals(protocolVersion) && ((Handshake) o).protocolName.equals(protocolName);
    }
}
