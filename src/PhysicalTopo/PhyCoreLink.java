package PhysicalTopo;

import Utils.LinkType;
import VirtualTopo.VirtSwitchPort;

import static Utils.LinkType.*;

/**
 * Created by pravein on 15/12/16.
 */
public class PhyCoreLink {
    String ID;
    Double Capacity; /* In Gbps */
    PhySwitchPort []endPoints = new PhySwitchPort[2];
    String hc;
    LinkType linkType;
    public PhyCoreLink(String ID, PhySwitchPort sp1, PhySwitchPort sp2) {
        this.ID = ID;
        endPoints[0] = sp1;
        endPoints[1] = sp2;
        hc = endPoints[0]+"-"+endPoints[1];
        setLinkType();
    }

    private void setLinkType() {
        if (endPoints[0].getParentSwitch().equals(endPoints[1].getParentSwitch())) {
            /* Connection within the same switch : Its a loop link */
            linkType = LOOP;
        } else {
            linkType = CORE;
        }
    }
    public PhySwitchPort[] getEndPoints() {
        return endPoints;
    }

    @Override
    public int hashCode() {
        return hc.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        PhyCoreLink compareVCL = (PhyCoreLink) arg;
        if (this.endPoints[0].equals(compareVCL.endPoints[0]) && this.endPoints[1].equals(compareVCL.endPoints[1])){
            return true;
        }
        /* Also the reverse, Since the link is duplex by design */
        if (this.endPoints[0].equals(compareVCL.endPoints[1]) && this.endPoints[1].equals(compareVCL.endPoints[0])){
            return true;
        }
        return false;
    }
    public String toString() {
        return this.linkType +"("+ this.Capacity+")"+ ": " + this.endPoints[0]+"<->"+ this.endPoints[1];
    }
    public void setCapacity(Double Capacity) {
        this.Capacity = Capacity;
    }

    public double getCapacity() {
        return this.Capacity;
    }
}
