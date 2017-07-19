package PhysicalTopo;

import java.util.ArrayList;

/**
 * Created by pravein on 13/7/17.
 */
public class PhyCorePath {
    Double Capacity; /* In Gbps */
    PhySwitchPort attachPoint;

    public PhyCorePath(PhySwitchPort attachPoint, Double Capacity) {
        this.attachPoint = attachPoint;
        this.Capacity = Capacity;
    }

    public PhySwitchPort getAttachPoint() {
        return attachPoint;
    }

    public Double getCapacity() {
        return Capacity;
    }

    @Override
    public int hashCode() {
        return attachPoint.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        PhyCorePath comparePCP = (PhyCorePath) arg;
        if (this.attachPoint.equals(comparePCP.attachPoint)){
            return true;
        }

        return false;
    }
    public String toString() {
        return  "("+ this.attachPoint+")";
    }

}
