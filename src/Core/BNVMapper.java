package Core;


import PhysicalTopo.PhyTopo;
import VirtualTopo.VirtTopo;
import gurobi.GRB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Created by pravein on 13/12/16.
 */
public class BNVMapper {
    public static void main(String [] args) throws Exception {
        /*
         * Usage : Core.Mapper.BNVMapper physicalTopoFile virtTopofile
         */
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File("randomgraphmapper.csv"),true));

        if (args.length < 4) {
            System.out.println("Usage : BNVMapper <\"bnvirt\"/\"ncl\"> physicalTopoFile virtTopofile <optimal/safe> loopports <Optional : Update file>  ");
            return;
        }


        Global.environment = args[0];
        String phyTopoFile = args[1];
        String virtTopoFile = args[2];
        String type = args[3];
        /* Default to 12 */
        int loop = 12;
        if (args.length > 4) {
            loop = Integer.parseInt(args[4]);
        }
        PhyTopo physicalTopo = new PhyTopo();
        if (Global.environment.equals("bnvirt")) {
            physicalTopo.loadPhyTopology(phyTopoFile, loop);
        } else if (Global.environment.equals("ncl")){
            physicalTopo.loadPhyTopologyNCL(phyTopoFile);
        }

        VirtTopo virtualTopo = new VirtTopo();

        if (virtTopoFile.contains("RandomGraph")) {
            CreateRandomGraphMapper(phyTopoFile, type, pw);
            pw.close();
            return;
        }  else if (virtTopoFile.contains("FatTree")){
            CreateFatTreeMapper(physicalTopo, type, pw);
            pw.close();
            return;
        }
        else {
            if (Global.environment.equals("bnvirt")) {
                virtualTopo.loadVirtTopology(virtTopoFile);
            } else {
                virtualTopo.loadVirtTopologyNCL(virtTopoFile);
            }
        }


        /* Load it to the Core.Mapper */

        Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
        System.out.println("-----------------------------------------------");
        int status = 0;
        if (type.equals("safe")) {
            System.out.println("Using Safe allocation.");
            status = myMapper.allocateSafe();
        } else if (type.equals("optimal")) {
            System.out.println("Using Optimal allocation.");
            status = myMapper.allocateOptimal();
        } else if (type.equals("fast")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastSafe();
        } else if (type.equals("fastopt")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastOpt();
        } else if (type.equals("fastpath")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastSafePaths();
        }
        pw.close();


        if (args.length > 5) {
            System.out.println("--------------------------Update Virt Topology------------------------");
            String updVirtTopoFile = args[5];
            updateTenantMapping(updVirtTopoFile, myMapper);
        }

        System.exit(status);
    }

    private static void updateTenantMapping(String updVirtTopoFile, Mapper myMapper) {
        try {
            VirtTopo updVirtTopo = new VirtTopo();
            updVirtTopo.loadVirtTopology(updVirtTopoFile);
            myMapper.updateMapping(updVirtTopo);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void CreateFatTreeMapper(PhyTopo physicalTopo, String type, PrintWriter pw) {
        int maxLoops = 12;
        int degree = 4;
        VirtTopo virtualTopo = new VirtTopo();
        virtualTopo.loadFatTreeTopo(degree);
        Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
        int status = -1;
        if (type.equals("safe")) {
            System.out.println("Using Safe allocation.");
            status = myMapper.allocateSafe();
        } else if (type.equals("optimal")) {
            System.out.println("Using Optimal allocation.");
            status = myMapper.allocateOptimal();
        } else if (type.equals("fast")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastSafe();
        } else if (type.equals("fastopt")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastOpt();
        } else if (type.equals("fastpath")) {
            System.out.println("Using Fast allocation.");
            status = myMapper.allocateFastSafePaths();
        }
        if (status == GRB.OPTIMAL) {
            System.out.println("Success!!!");

        } else {
            System.out.println("Failed!!!");
        }
        /* Below code needs to be enabled to try and embed various fat tree degrees to the substrate network */
//        for (int loop=12; loop <= maxLoops ;loop++) {
//            PhyTopo physicalTopo = new PhyTopo();
//            physicalTopo.loadPhyTopology(phyTopoFile, loop);
//            System.out.println("Starting with "+ loop +" loopbacks...");
//
//            for (int degree = 0; ; degree+=2) {
//                VirtTopo virtualTopo = new VirtTopo();
//                virtualTopo.loadFatTreeTopo(degree);
//                Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
//                int status = myMapper.allocateOptimal();
//                System.out.println("Trying FatTree" +degree + "with "+ loop + "loops");
//                if (status == GRB.OPTIMAL) {
//                    pw.println(loop+","+degree);
//                    System.out.println("Success!!!");
//
//                } else {
//                    System.out.println("Failed!!!");
//                    break;
//                }
//            }
//        }
    }


    public static void CreateRandomGraphMapper (String phyTopoFile, String type, PrintWriter pw) {
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
                    int status = myMapper.allocateOptimal();
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
                    int status = myMapper.allocateOptimal();
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

