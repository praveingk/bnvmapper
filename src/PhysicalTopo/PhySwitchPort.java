package PhysicalTopo;

/**
 * Created by pravein on 15/12/16.
 */
public class PhySwitchPort {
    private String ID;
    private PhySwitch parentSwitch = null;
    String hc = new String();
    public PhySwitchPort(String ID, PhySwitch parentSwitch) {
        this.ID = ID;
        this.parentSwitch = parentSwitch;
    }


    public String getID() {
        return ID;
    }


    public PhySwitch getParentSwitch() {
        return this.parentSwitch;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        PhySwitchPort compareVSP = (PhySwitchPort) arg;
        if (this.ID.equals(compareVSP.getID())){
            return true;
        }
        return false;
    }
    public String toString() {
        return "PSP:"+ID;
    }


}
