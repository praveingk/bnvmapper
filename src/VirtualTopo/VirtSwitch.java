package VirtualTopo;

import PhysicalTopo.PhySwitchPort;

import java.util.ArrayList;

/**
 * Created by pravein on 15/12/16.
 */
public class VirtSwitch extends VirtNode {
    private String ID;
    private ArrayList<VirtSwitchPort> switchPorts = new ArrayList<>();
    String hc = new String();
    private int TcamCapacity;
    public VirtSwitch(String ID) {
        this.ID = ID;
    }

    public String getID() {
        return ID;
    }

    public ArrayList<VirtSwitchPort> getSwitchPorts(){
        return switchPorts;
    }

    public void addSwitchPort(VirtSwitchPort psp) {
        switchPorts.add(psp);
    }
    public boolean contains(VirtSwitchPort switchPort) {
        return switchPorts.contains(switchPort);
    }

    public void printSwitchPorts() {
        System.out.println(this.switchPorts.toString());
    }

    public int getTCAMCapacity() {
        return TcamCapacity;
    }
    public void setTcamCapacity(int cap) {
        this.TcamCapacity = cap;
    }
    @Override
    public int hashCode() {
        return ID.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        VirtSwitch compareVS = (VirtSwitch) arg;
        if (this.ID.equals(compareVS.getID())){
            return true;
        }
        return false;
    }
    public String toString() {
        return "VS:"+ID;
    }
}
