package VirtualTopo;

import Utils.LinkType;


/**
 * Created by pravein on 15/12/16.
 */
public class VirtCoreLink {
    String ID;
    Double bandWidth; /* In Gbps */
    VirtSwitchPort []endPoints = new VirtSwitchPort[2];
    String hc;
    public VirtCoreLink(String ID, VirtSwitchPort sp1, VirtSwitchPort sp2) {
        this.ID = ID;
        endPoints[0] = sp1;
        endPoints[1] = sp2;
        hc = endPoints[0]+"-"+endPoints[1];
    }

    public VirtSwitchPort[] getEndPoints() {
        return endPoints;
    }

    @Override
    public int hashCode() {
        return hc.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        VirtCoreLink compareVCL = (VirtCoreLink) arg;
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
        return this.endPoints[0]+"<->"+ this.endPoints[1];
    }

    public void setBandWidth (Double bandWidth) {
        this.bandWidth = bandWidth;
    }

    public double getBandwidth() {
        return bandWidth;
    }


    public String getID(){
        return ID;
    }
}
