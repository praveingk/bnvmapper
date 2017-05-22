package VirtualTopo;

/**
 * Created by pravein on 15/12/16.
 */
public class VirtHostLink {
    String ID;
    Double bandWidth; /* In Gbps */
    VirtSwitchPort switchPort;
    VirtHost hostPort;
    String hc;
    public VirtHostLink(String ID, VirtSwitchPort sp, VirtHost host) {
        this.ID = ID;
        this.switchPort = sp;
        this.hostPort = host;
        hc = switchPort+"-"+hostPort;
    }

    @Override
    public int hashCode() {
        return hc.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        VirtHostLink compareVHL = (VirtHostLink) arg;
        if (this.switchPort.equals(compareVHL.switchPort) && this.hostPort.equals(compareVHL.hostPort)){
            return true;
        }

        return false;
    }

    public VirtHost getHostPort(){
        return hostPort;
    }

    public String getID(){
        return ID;
    }

    public VirtSwitchPort getSwitchPort() {
        return switchPort;
    }

    public String toString() {
        return this.hostPort+"<->"+ this.switchPort;
    }

    public void setBandWidth (Double bandWidth) {
        this.bandWidth = bandWidth;
    }
}
