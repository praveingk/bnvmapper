package PhysicalTopo;

/**
 * Created by pravein on 15/12/16.
 */
public class PhyHost {
    private String ID;
    String hc = new String();

    public PhyHost(String ID) {
        this.ID = ID;
    }

    public String getID() {
        return ID;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
    @Override
    public boolean equals(Object arg) {
        PhyHost compareVH = (PhyHost) arg;
        if (this.ID.equals(compareVH.getID())){
            return true;
        }
        return false;
    }
    public String toString() {
        return "H:"+ID;
    }
}
