import PhysicalTopo.PhyTopo;
import VirtualTopo.VirtTopo;
import gurobi.GRB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Created by pravein on 13/12/16.
 */
public class VirtTopoMapper {
    public static void main(String [] args) throws Exception {
        /*
         * Usage : VirtTopoMapper physicalTopoFile virtTopofile
         */
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File("randomgraphmapper.csv"),true));

        if (args.length < 3) {
            System.out.println("Usage : VirtTopoMapper physicalTopoFile virtTopofile loopports");
            return;
        }



        String phyTopoFile = args[0];
        String virtTopoFile = args[1];
        int loop = Integer.parseInt(args[2]);
        PhyTopo physicalTopo = new PhyTopo();
        physicalTopo.loadPhyTopology(phyTopoFile, loop);

        VirtTopo virtualTopo = new VirtTopo();

        if (virtTopoFile.contains("RandomGraph")) {
            CreateRandomGraphMapper(phyTopoFile, pw);
            pw.close();
            return;
        }  else if (virtTopoFile.contains("FatTree")){
            CreateFatTreeMapper(phyTopoFile, pw);
            pw.close();
            return;
        }
        else {
            virtualTopo.loadVirtTopology(virtTopoFile);
        }


        /* Load it to the Mapper */

        Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
        int status = myMapper.allocate();
        pw.close();
    }
    public static void CreateFatTreeMapper(String phyTopoFile, PrintWriter pw) {
        int maxLoops = 12;
        for (int loop=0; loop <= maxLoops ;loop++) {
            PhyTopo physicalTopo = new PhyTopo();
            physicalTopo.loadPhyTopology(phyTopoFile, loop);
            System.out.println("Starting with "+ loop +" loopbacks...");

            for (int degree = 0; ; degree+=2) {
                VirtTopo virtualTopo = new VirtTopo();
                virtualTopo.loadFatTreeTopo(degree);
                Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                int status = myMapper.allocate();
                System.out.println("Trying FatTree" +degree + "with "+ loop + "loops");
                if (status == GRB.OPTIMAL) {
                    pw.println(loop+","+degree);
                    System.out.println("Success!!!");

                } else {
                    System.out.println("Failed!!!");
                    break;
                }
            }
        }
    }


    public static void CreateRandomGraphMapper (String phyTopoFile, PrintWriter pw) {
        int maxLoops = 12;
        for (int loop = 12; loop <= maxLoops; loop+=12) {
            PhyTopo physicalTopo = new PhyTopo();
            physicalTopo.loadPhyTopology(phyTopoFile, loop);
            System.out.println("Starting with " + loop + " loopbacks...");
            int switches = 120;
            int firstfail = 0;
            int secfail  = 0;
            for (; ; switches += 5) {
                if (firstfail == 0) {
                    int links = 2 * switches;
                    VirtTopo virtualTopo = new VirtTopo();
                    virtualTopo.loadRandomGraphTopo(switches, links);
                    Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                    int status = myMapper.allocate();
                    if (status == GRB.OPTIMAL) {
                        pw.println(loop + "," + switches + "," + links);
                        System.out.println("Success!!!");

                    } else {
                        System.out.println("Failed!!!");
                        firstfail = 1;
                    }
                }

                if (secfail == 0) {
                    int links = (int) Math.ceil(Math.sqrt(switches) * switches);
                    VirtTopo virtualTopo = new VirtTopo();
                    virtualTopo.loadRandomGraphTopo(switches, links);
                    Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                    int status = myMapper.allocate();
                    if (status == GRB.OPTIMAL) {
                        pw.println(loop + "," + switches + "," + links);
                        System.out.println("Success!!!");

                    } else {
                        System.out.println("Failed!!!");
                        secfail = 1;
                    }
                }

                if (firstfail == 1 && secfail == 1) {
                    break;
                }
            }

        }
    }
}
