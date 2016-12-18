package VirtualTopo;

import java.util.ArrayList;

/**
 * Created by pravein on 15/12/16.
 */
public class VirtSwitchPort {
    private String ID;
    private VirtSwitch parentSwitch = null;
    String hc = new String();
    int TCAM;
    public VirtSwitchPort(String ID, VirtSwitch parentSwitch) {
        this.ID = ID;
        this.parentSwitch = parentSwitch;
    }

    public String getID() {
        return ID;
    }


    public VirtSwitch getParentSwitch() {
        return this.parentSwitch;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        VirtSwitchPort compareVSP = (VirtSwitchPort) arg;
        if (this.ID.equals(compareVSP.getID())){
            return true;
        }
        return false;
    }
    public String toString() {
        return "VSP:"+ID;
    }

    public void setTCAM(int TCAM) {
        this.TCAM = TCAM;
    }
    public int getTCAM() {
        return TCAM;
    }
}
