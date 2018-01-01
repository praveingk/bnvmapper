package PhysicalTopo;

import VirtualTopo.VirtHost;
import VirtualTopo.VirtSwitchPort;

/**
 * Created by pravein on 15/12/16.
 */
public class PhyHostLink {
    String ID;
    Double Capacity; /* In Gbps */
    PhySwitchPort switchPort;
    PhyHost hostPort;
    String hc;
    public PhyHostLink(String ID, PhySwitchPort sp, PhyHost host) {
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
        PhyHostLink compareVHL = (PhyHostLink) arg;
        if (this.switchPort.equals(compareVHL.switchPort) && this.hostPort.equals(compareVHL.hostPort)){
            return true;
        }

        return false;
    }
    public String toString() {
        return  "("+ this.Capacity+")"+this.hostPort+"<->"+ this.switchPort;
    }

    public void setCapacity(Double Capacity) {
        this.Capacity = Capacity;
    }

    public PhyHost getHostPort(){
        return hostPort;
    }

    public String getID(){
        return ID;
    }

    public PhySwitchPort getSwitchPort() {
        return switchPort;
    }
}
