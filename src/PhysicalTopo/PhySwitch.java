package PhysicalTopo;

import java.util.ArrayList;

/**
 * Created by pravein on 15/12/16.
 */
public class PhySwitch {
    private String ID;
    private ArrayList<PhySwitchPort> switchPorts = new ArrayList<>();
    String hc = new String();
    private int TcamCapacity;
    private PhyCorePath corePath;
    public PhySwitch(String ID) {
        this.ID = ID;
    }

    public String getID() {
        return ID;
    }

    public ArrayList<PhySwitchPort> getSwitchPorts(){
        return switchPorts;
    }

    public void addSwitchPort(PhySwitchPort psp) {
        switchPorts.add(psp);
    }

    public void setTcamCapacity(int cap) {
        this.TcamCapacity = cap;
    }

    public int getTCAMCapacity() {
        return TcamCapacity;
    }
    public boolean contains(PhySwitchPort switchPort) {
        return switchPorts.contains(switchPort);
    }

    public void printSwitchPorts() {
        System.out.println(this.switchPorts.toString());
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        PhySwitch compareVS = (PhySwitch) arg;
        if (this.ID.equals(compareVS.getID())){
            return true;
        }
        return false;
    }
    public String toString() {
        return "PS:"+ID;
    }

    public void addCorePath(PhyCorePath corePath) {
        this.corePath = corePath;
        System.out.println("IN Switch : "+ this.ID +  " Adding Core Path " + corePath.toString());

    }
    public PhyCorePath getCorePath() {
        return corePath;
    }
}
