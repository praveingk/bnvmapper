package VirtualTopo;


/**
 * Created by pravein on 15/12/16.
 */
public class VirtLinkPair {
    VirtCoreLink []endPoints = new VirtCoreLink[2];
    String hc;
    public VirtLinkPair(VirtCoreLink sp1, VirtCoreLink sp2) {
        endPoints[0] = sp1;
        endPoints[1] = sp2;
        hc = endPoints[0]+"-"+endPoints[1];

    }

    public VirtCoreLink[] getEndPoints() {
        return endPoints;
    }

    @Override
    public int hashCode() {
        return hc.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        VirtLinkPair compareVCL = (VirtLinkPair) arg;
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
        return this.endPoints[0]+"<==>"+ this.endPoints[1];
    }

}
