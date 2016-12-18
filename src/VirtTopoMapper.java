import PhysicalTopo.PhyTopo;
import VirtualTopo.VirtTopo;

/**
 * Created by pravein on 13/12/16.
 */
public class VirtTopoMapper {
    public static void main(String [] args) {
        /*
         * Usage : VirtTopoMapper physicalTopoFile virtTopofile
         */

        if (args.length < 2) {
            System.out.println("Usage : VirtTopoMapper physicalTopoFile virtTopofile ");
            return;
        }


        String phyTopoFile = args[0];
        String virtTopoFile = args[1];

        PhyTopo physicalTopo = new PhyTopo();
        physicalTopo.loadPhyTopology(phyTopoFile);
        VirtTopo virtualTopo = new VirtTopo();
        virtualTopo.loadVirtTopology(virtTopoFile);


        /* Load it to the Mapper */
        Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
        myMapper.allocate();
    }
}
