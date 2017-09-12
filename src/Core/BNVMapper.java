package Core;


import PhysicalTopo.PhyHost;
import PhysicalTopo.PhySwitchPort;
import PhysicalTopo.PhyTopo;
import VirtualTopo.*;
import gurobi.GRB;
import gurobi.GRBVar;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

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
        if (type.equals("linksafe")) {
            Global.duplex = 1;
        } else {
            Global.duplex = 0;
        }
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

        /* Hack to map node explicitly in two clusters to test */

        if (virtTopoFile.contains("bnvirt-cons3")) {
            printStaticAssignFile(physicalTopo);
            return;
        }

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
        } else if (type.equals("safelinear")) {
            System.out.println("Using safe linear allocation.");
            status = myMapper.allocateSafeLinearize();
        } else if (type.equals("linksafe")) {
            System.out.println("Using link safe  allocation.");
            status = myMapper.allocateLinkSafe();
        } else if (type.equals("linkoptimal")) {
            System.out.println("Using link safe  allocation.");
            status = myMapper.allocateLinkOptimal();
        }
        pw.close();


        if (args.length > 5) {
            System.out.println("--------------------------Update Virt Topology------------------------");
            String updVirtTopoFile = args[5];
            updateTenantMapping(updVirtTopoFile, myMapper);
        }

        System.exit(status);
    }

    private static void printStaticAssignFile(PhyTopo physicalTopo) throws Exception {
        HashMap<String, Double> backboneStatMap = new HashMap<>();
        BufferedWriter bw = new BufferedWriter((new FileWriter("backboneStats")));

        ArrayList<PhyHost> reservedNodes = new ArrayList<>();
        String ofctrl = "pc1c";
        String node1 = "pc3a";
        String ofport1 = "ofport3a";
        String node2 = "pc2a";
        String ofport2 = "ofport2a";
        String node3 = "pc18d";
        String ofport3 = "ofport18d";
        String node4 = "pc11d";
        String ofport4 = "ofport11d";

        System.out.println("Nodes:");
        System.out.println("node1 "+node1);
        System.out.println("node2 "+node2);
        System.out.println("node3 "+node3);
        System.out.println("node4 "+node4);
        System.out.println("ofctrl "+ofctrl);
        System.out.println("ovx ovx");

        System.out.println("switch1p1 "+ofport1);
        System.out.println("switch1p2 "+ofport2);
        System.out.println("switch2p1 "+ofport3);
        System.out.println("switch2p2 "+ofport4);

        System.out.println("switch1p3 ofport101a");
        System.out.println("switch2p3 ofport101d");
        System.out.println("End Nodes");

        System.out.println("Edges:");
        System.out.println("linksimple/link1/node1:0,switch1p1:1 direct link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)" + " link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)");
        System.out.println("linksimple/link2/node2:0,switch1p2:1 direct link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)" + " link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)");
        System.out.println("linksimple/link3/node3:0,switch2p1:1 direct link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)" + " link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)");
        System.out.println("linksimple/link4/node4:0,switch2p2:1 direct link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)" + " link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)");
        System.out.println("linksimple/link10/switch1p3:1,switch2p3:1 direct link-ofport101a:eth0-ofport101d:eth0 (ofport101a/eth0,eth0) link-ofport101a:eth0-ofport101d:eth0 (ofport101a/eth0,eth0)");
        System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 interswitch link-" + ofctrl + ":eth0-HPClus4Expt:(null) (" + ofctrl + "/eth0,(null)) link-HPCore1:HPClus4Expt (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1)");

        System.out.println("linklan/ofswitchswitch1/switch1p1:0 direct link-"+ofport1+":eth1-HPClus2SDN(null) ("+ofport1+"/eth1,null)) link-"+ofport1+":eth1-HPClus2SDN:(null) ("+ofport1+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p2:0 direct link-"+ofport2+":eth1-HPClus2SDN(null) ("+ofport2+"/eth1,null)) link-"+ofport2+":eth1-HPClus2SDN:(null) ("+ofport2+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p3:0 direct link-ofport101a:eth1-HPClus2SDN(null) (ofport101a/eth1,null)) link-ofport101a:eth1-HPClus2SDN:(null) (ofport101a/eth1,null))" );

        System.out.println("linklan/ofswitchswitch2/switch2p1:0 direct link-"+ofport3+":eth1-HPClus1SDN(null) ("+ofport3+"/eth1,null)) link-"+ofport3+":eth1-HPClus1SDN:(null) ("+ofport3+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch2/switch2p2:0 direct link-"+ofport4+":eth1-HPClus1SDN(null) ("+ofport4+"/eth1,null)) link-"+ofport4+":eth1-HPClus1SDN:(null) ("+ofport4+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch2/switch2p3:0 direct link-ofport101d:eth1-HPClus1SDN(null) (ofport101d/eth1,null)) link-ofport101d:eth1-HPClus1SDN:(null) (ofport101d/eth1,null))" );
        System.out.println("End Edges");
        System.out.println("End solution");

    }
//    private void printStaticAssignFile(PhyTopo physicalTopo) throws Exception {
//        HashMap<String, Double> backboneStatMap = new HashMap<>();
//        BufferedWriter bw = new BufferedWriter((new FileWriter("backboneStats")));
//
//        ArrayList<PhyHost> reservedNodes = new ArrayList<>();
//        PhyHost ofctrl = null;
//        PhyHost node1 = null;
//        PhyHost node2 = null;
//        PhyHost node3 = null;
//        PhyHost node4 = null;
//
//        System.out.println("Nodes:");
//
//
//        /* Now a bruteforce allocation of ofctrl and ovx */
//        for (int i=0;i<physicalTopo.getHosts().size();i++) {
//            PhyHost myHost = physicalTopo.getHosts().get(i);
//            if (!reservedNodes.contains(myHost) && myHost.getID().endsWith("a")) {
//                System.out.println("node1 " + physicalTopo.getHosts().get(i).getID());
//                node1 = physicalTopo.getHosts().get(i);
//                reservedNodes.add(node1);
//                break;
//            }
//        }
//
//        for (int i=0;i<physicalTopo.getHosts().size();i++) {
//            PhyHost myHost = physicalTopo.getHosts().get(i);
//            if (!reservedNodes.contains(myHost) && myHost.getID().endsWith("a")) {
//                System.out.println("node2 " + physicalTopo.getHosts().get(i).getID());
//                node2 = physicalTopo.getHosts().get(i);
//                reservedNodes.add(node2);
//                break;
//            }
//        }
//
//        for (int i=0;i<physicalTopo.getHosts().size();i++) {
//            PhyHost myHost = physicalTopo.getHosts().get(i);
//            if (!reservedNodes.contains(myHost) && myHost.getID().endsWith("d")) {
//                System.out.println("node3 " + physicalTopo.getHosts().get(i).getID());
//                node2 = physicalTopo.getHosts().get(i);
//                reservedNodes.add(node2);
//                break;
//            }
//        }
//        for (int i=0;i<physicalTopo.getHosts().size();i++) {
//            PhyHost myHost = physicalTopo.getHosts().get(i);
//            if (!reservedNodes.contains(myHost) && myHost.getID().endsWith("d")) {
//                System.out.println("node3 " + physicalTopo.getHosts().get(i).getID());
//                node2 = physicalTopo.getHosts().get(i);
//                reservedNodes.add(node2);
//                break;
//            }
//        }
//        for (int i=0;i<physicalTopo.getHosts().size();i++) {
//            PhyHost myHost = physicalTopo.getHosts().get(i);
//            if (!reservedNodes.contains(myHost)) {
//                System.out.println("ofctrl " + physicalTopo.getHosts().get(i).getID());
//                ofctrl = physicalTopo.getHosts().get(i);
//                break;
//            }
//        }
//        System.out.println("ovx ovx");
//
//            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
//                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
//                    if (switchPortMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
//                        switchPortMapping.put(virtualTopo.getSwitchPorts().get(i), physicalTopo.getSwitchPorts().get(j));
//                        System.out.println(virtualTopo.getSwitchPorts().get(i).getID() + " " + physicalTopo.getSwitchPorts().get(j).getID());
//                    }
//                }
//            }
//
//            System.out.println("End Nodes");
//            System.out.println("Edges:");
//            for (int i=0;i<virtualTopo.getHostLinks().size();i++) {
//                VirtHostLink vhl = virtualTopo.getHostLinks().get(i);
//                PhyHost ph = null;
//                ph = hostMapping.get(vhl.getHostPort());
//                PhySwitchPort psp  = null;
//                psp = switchPortMapping.get(vhl.getSwitchPort());
//                System.out.println("linksimple/"+vhl.getID()+"/"+vhl.getHostPort().getID()+":0,"+vhl.getSwitchPort().getID()+":1 direct link-"+ph.getID()+":eth3-"+psp.getID()+":eth0 ("+ph.getID()+"/eth3,eth0)" + " link-"+ph.getID()+":eth3-"+psp.getID()+":eth0 ("+ph.getID()+"/eth3,eth0)");
//            }
//            for (int i=0;i<virtualTopo.getCoreLinks().size();i++) {
//                VirtCoreLink vcl = virtualTopo.getCoreLinks().get(i);
//                PhySwitchPort psp1 = null;
//                psp1 = switchPortMapping.get(vcl.getEndPoints()[0]);
//                if (psp1 == null) {
//                    System.out.println("psp1 is null\n");
//                }
//                PhySwitchPort psp2 = null;
//                psp2 = switchPortMapping.get(vcl.getEndPoints()[1]);
//                if (psp2 == null) {
//                    System.out.println("psp2 is null\n");
//                }
//                System.out.println("linksimple/"+vcl.getID()+"/"+vcl.getEndPoints()[0].getID()+":1,"+vcl.getEndPoints()[1].getID()+":1 direct link-"+psp1.getID()+":eth0-"+psp2.getID()+":eth0 ("+psp1.getID()+"/eth0,eth0)" + " link-"+psp1.getID()+":eth0-"+psp2.getID()+":eth0 ("+psp1.getID()+"/eth0,eth0)");
//                if (!psp1.getParentSwitch().equals(psp2.getParentSwitch())) {
//                    //backboneStatMap.put(psp1.getID()+"-"+psp2.getID(), virtualTopo.getCoreLinks().get(i).getBandwidth());
//                    String link = psp1.getID()+"-"+psp2.getID();
//                    String revLink = psp2.getID()+"-"+psp1.getID();
//                    if (!backboneStatMap.containsKey(link) && !backboneStatMap.containsKey(revLink)) {
//                        backboneStatMap.put(link, vcl.getBandwidth());
//                        backboneStatMap.put(revLink, vcl.getBandwidth());
//                    } else {
//                        Double cap = backboneStatMap.get(link);
//                        cap += vcl.getBandwidth();
//                        backboneStatMap.put(link, cap);
//                        backboneStatMap.put(revLink, cap);
//                    }
//
//                }
//
//            }
//            String exptswitch = switchCon.get(ofctrl.getID().substring(ofctrl.getID().length()-1));
//            // Need to find a better way of handling production/ staging env
//            if (physicalTopo.ncl_environment.equals("PRODUCTION")) {
//                System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 interswitch link-" + ofctrl.getID() + ":eth0-" + exptswitch + ":(null) (" + ofctrl.getID() + "/eth0,(null)) link-HPCore1:" + exptswitch + " (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1)");
//            } else {
//                System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 intraswitch link-" + ofctrl.getID() + ":eth0-" + exptswitch + ":(null) (" + ofctrl.getID() + "/eth0,(null)) link-ovx:eth1-" + exptswitch + ":Ten-GigabitEthernet1/0/48 (ovx/eth1,Ten-GigabitEthernet1/0/48)");
//            }
//
//            for (int i=0;i<virtualTopo.getSwitches().size();i++) {
//                VirtSwitch vs = virtualTopo.getSwitches().get(i);
//                ArrayList<VirtSwitchPort> vswitchPorts = vs.getSwitchPorts();
//
//                String linkType = "linksimple";
//                if (vswitchPorts.size() > 2) {
//                    linkType = "linklan";
//                    for (int j=0;j<vswitchPorts.size();j++) {
//                        String physwitch = switchPortMapping.get(vswitchPorts.get(j)).getID();
//                        String sdnswitch = sdnswitchCon.get(physwitch.substring(physwitch.length()-1));
//
//                        System.out.print(linkType+"/"+vs.getID()+"/");
//                        System.out.print(vswitchPorts.get(j).getID()+":0");
//                        System.out.print(" direct ");
//                        System.out.print("link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+sdnswitch+"(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null)) link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+ sdnswitch+ ":(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null))" );
//                        System.out.println();
//                    }
//                    continue;
//                }
//                System.out.print(linkType+"/"+vs.getID()+"/");
//                for (int j=0;j<vswitchPorts.size();j++) {
//                    if (j!=0) { System.out.print(","); }
//                    System.out.print(vswitchPorts.get(j).getID()+":0");
//                }
//                System.out.print(" intraswitch ");
//
//                for (int j=0;j<vswitchPorts.size();j++) {
//                    String physwitch = switchPortMapping.get(vswitchPorts.get(j)).getID();
//                    //System.out.println(switchPortMapping.get(vswitchPorts.get(j)).getID());
//                    //System.out.println(physwitch.substring(physwitch.length()-1));
//                    String sdnswitch = sdnswitchCon.get(physwitch.substring(physwitch.length()-1));
//                    System.out.print("link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+ sdnswitch + ":(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null)) ");
//                }
//                System.out.println();
//            }
//            System.out.println("End Edges");
//            System.out.println("End solution");
//            for (String pcl : backboneStatMap.keySet()) {
//                bw.write(ofctrl.getID()+","+pcl+","+backboneStatMap.get(pcl)+System.getProperty("line.separator"));
//            }
//            bw.close();
//
//    }



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
        } else if (type.equals("linksafe")) {
            System.out.println("Using link safe  allocation.");
            status = myMapper.allocateLinkSafe();
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

