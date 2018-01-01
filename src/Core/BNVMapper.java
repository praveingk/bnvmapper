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
        PrintWriter pw = new PrintWriter(new FileOutputStream(new File("bnvmapper.csv"),true));

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

        if (virtTopoFile.contains("multi-star")) {
            printStaticAssignFileStar(physicalTopo);
            return;
        }

        if (virtTopoFile.contains("multi-clos")) {
            printStaticAssignFileClos(physicalTopo);
            return;
        }

        if (virtTopoFile.contains("RandomGraph")) {
            CreateRandomGraphMapper(phyTopoFile, type, pw);
            pw.close();
            return;
        }  else if (virtTopoFile.contains("FatTree")){
            CreateFatTreeMapper(physicalTopo, type, pw, loop);
            pw.close();
            return;
        } else if (virtTopoFile.contains("topo-zoo")) {
            CreateTopoZooMapper(phyTopoFile, "topo-zoo", type, pw, loop);
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
        String ofctrl = "pc19c";
        String node1 = "pc20d";
        String ofport1 = "ofport20d";
        String node2 = "pc23d";
        String ofport2 = "ofport23d";
        String node3 = "pc19b";
        String ofport3 = "ofport19b";
        String node4 = "pc20b";
        String ofport4 = "ofport20b";

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

        System.out.println("switch1p3 ofport47d");
        System.out.println("switch2p3 ofport47b");
        System.out.println("End Nodes");

        System.out.println("Edges:");
        System.out.println("linksimple/link1/node1:0,switch1p1:1 direct link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)" + " link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)");
        System.out.println("linksimple/link2/node2:0,switch1p2:1 direct link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)" + " link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)");
        System.out.println("linksimple/link3/node3:0,switch2p1:1 direct link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)" + " link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)");
        System.out.println("linksimple/link4/node4:0,switch2p2:1 direct link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)" + " link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)");
        System.out.println("linksimple/link10/switch1p3:1,switch2p3:1 direct link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0) link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0)");
        System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 interswitch link-" + ofctrl + ":eth0-HPClus4Expt:(null) (" + ofctrl + "/eth0,(null)) link-HPCore1:HPClus4Expt (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1)");

        System.out.println("linklan/ofswitchswitch1/switch1p1:0 direct link-"+ofport1+":eth1-HPClus1SDN(null) ("+ofport1+"/eth1,null)) link-"+ofport1+":eth1-HPClus1SDN:(null) ("+ofport1+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p2:0 direct link-"+ofport2+":eth1-HPClus1SDN(null) ("+ofport2+"/eth1,null)) link-"+ofport2+":eth1-HPClus1SDN:(null) ("+ofport2+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p3:0 direct link-ofport47d:eth1-HPClus1SDN(null) (ofport47d/eth1,null)) link-ofport47d:eth1-HPClus1SDN:(null) (ofport47d/eth1,null))" );

        System.out.println("linklan/ofswitchswitch2/switch2p1:0 direct link-"+ofport3+":eth1-HPClus3SDN(null) ("+ofport3+"/eth1,null)) link-"+ofport3+":eth1-HPClus3SDN:(null) ("+ofport3+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch2/switch2p2:0 direct link-"+ofport4+":eth1-HPClus3SDN(null) ("+ofport4+"/eth1,null)) link-"+ofport4+":eth1-HPClus3SDN:(null) ("+ofport4+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch2/switch2p3:0 direct link-ofport47b:eth1-HPClus3SDN(null) (ofport47b/eth1,null)) link-ofport47b:eth1-HPClus3SDN:(null) (ofport47b/eth1,null))" );
        System.out.println("End Edges");
        System.out.println("End solution");

    }

    private static void printStaticAssignFileStar(PhyTopo physicalTopo) throws Exception {
        HashMap<String, Double> backboneStatMap = new HashMap<>();
        BufferedWriter bw = new BufferedWriter((new FileWriter("backboneStats")));

        ArrayList<PhyHost> reservedNodes = new ArrayList<>();
        String ofctrl = "pc4c";
        String node1 = "pc12d";
        String ofport1 = "ofport12d";
        String node2 = "pc20d";
        String ofport2 = "ofport20d";
        String node3 = "pc5d";
        String ofport3 = "ofport5d";
        String node4 = "pc3d";
        String ofport4 = "ofport3d";
        String node5 = "pc24a";
        String ofport5 = "ofport24a";
        String node6 = "pc3a";
        String ofport6 = "ofport3a";
        String node7 = "pc5a";
        String ofport7 = "ofport5a";
        String node8 = "pc6a";
        String ofport8 = "ofport6a";




        System.out.println("Nodes:");
        System.out.println("namenode "+node1);
        System.out.println("datanode1 "+node2);
        System.out.println("datanode2 "+node3);
        System.out.println("datanode3 "+node4);
        System.out.println("datanode4 "+node5);
        System.out.println("datanode5 "+node6);
        System.out.println("datanode6 "+node7);
        System.out.println("datanode7 "+node8);
        System.out.println("ofctrl "+ofctrl);
        System.out.println("ovx ovx");

        System.out.println("switch1p1 "+ofport1);
        System.out.println("switch1p2 "+ofport2);
        System.out.println("switch1p3 "+ofport3);
        System.out.println("switch1p4 "+ofport4);
        System.out.println("switch1p5 ofport47d");
        System.out.println("switch1p6 ofport48d");
        System.out.println("switch2p1 ofport47b");
        System.out.println("switch2p2 ofport48b");
        System.out.println("switch3p1 ofport47c");
        System.out.println("switch3p2 ofport48c");
        System.out.println("switch4p1 "+ofport5);
        System.out.println("switch4p2 "+ofport6);
        System.out.println("switch4p3 "+ofport7);
        System.out.println("switch4p4 "+ofport8);
        System.out.println("switch4p5 ofport47a");
        System.out.println("switch4p6 ofport48a");

        System.out.println("End Nodes");

        System.out.println("Edges:");
        System.out.println("linksimple/link1/namenode:0,switch1p1:1 direct link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)" + " link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)");
        System.out.println("linksimple/link2/datanode1:0,switch1p2:1 direct link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)" + " link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)");
        System.out.println("linksimple/link3/datanode2:0,switch1p3:1 direct link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)" + " link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)");
        System.out.println("linksimple/link4/datanode3:0,switch1p4:1 direct link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)" + " link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)");
        System.out.println("linksimple/link5/datanode4:0,switch1p5:1 direct link-"+node5+":eth3-"+ofport5+":eth0 ("+node5+"/eth3,eth0)" + " link-"+node5+":eth3-"+ofport5+":eth0 ("+node5+"/eth3,eth0)");
        System.out.println("linksimple/link6/datanode5:0,switch1p6:1 direct link-"+node6+":eth3-"+ofport6+":eth0 ("+node6+"/eth3,eth0)" + " link-"+node6+":eth3-"+ofport6+":eth0 ("+node6+"/eth3,eth0)");
        System.out.println("linksimple/link7/datanode6:0,switch1p7:1 direct link-"+node7+":eth3-"+ofport7+":eth0 ("+node7+"/eth3,eth0)" + " link-"+node7+":eth3-"+ofport7+":eth0 ("+node7+"/eth3,eth0)");
        System.out.println("linksimple/link8/datanode7:0,switch1p8:1 direct link-"+node8+":eth3-"+ofport8+":eth0 ("+node8+"/eth3,eth0)" + " link-"+node8+":eth3-"+ofport8+":eth0 ("+node8+"/eth3,eth0)");

        System.out.println("linksimple/link10/switch1p5:1,switch2p1:1 direct link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0) link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0)");
        System.out.println("linksimple/link11/switch1p6:1,switch3p1:1 direct link-ofport48d:eth0-ofport47c:eth0 (ofport48d/eth0,eth0) link-ofport48d:eth0-ofport47c:eth0 (ofport48d/eth0,eth0)");
        System.out.println("linksimple/link12/switch4p5:1,switch2p2:1 direct link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0) link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0)");
        System.out.println("linksimple/link13/switch4p6:1,switch3p2:1 direct link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0) link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0)");

        System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 interswitch link-" + ofctrl + ":eth0-HPClus4Expt:(null) (" + ofctrl + "/eth0,(null)) link-HPCore1:HPClus4Expt (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1)");

        System.out.println("linklan/ofswitchswitch1/switch1p1:0 direct link-"+ofport1+":eth1-HPClus1SDN(null) ("+ofport1+"/eth1,null)) link-"+ofport1+":eth1-HPClus1SDN:(null) ("+ofport1+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p2:0 direct link-"+ofport2+":eth1-HPClus1SDN(null) ("+ofport2+"/eth1,null)) link-"+ofport2+":eth1-HPClus1SDN:(null) ("+ofport2+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p3:0 direct link-"+ofport3+":eth1-HPClus1SDN(null) ("+ofport3+"/eth1,null)) link-"+ofport3+":eth1-HPClus1SDN:(null) ("+ofport3+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p4:0 direct link-"+ofport4+":eth1-HPClus1SDN(null) ("+ofport4+"/eth1,null)) link-"+ofport4+":eth1-HPClus1SDN:(null) ("+ofport4+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p5:0 direct link-ofport47d:eth1-HPClus1SDN(null) (ofport47d/eth1,null)) link-ofport47d:eth1-HPClus1SDN:(null) (ofport47d/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p6:0 direct link-ofport48d:eth1-HPClus1SDN(null) (ofport48d/eth1,null)) link-ofport48d:eth1-HPClus1SDN:(null) (ofport48d/eth1,null))" );

        System.out.println("linklan/ofswitchswitch1/switch1p5:0 direct link-"+ofport5+":eth1-HPClus2SDN(null) ("+ofport5+"/eth1,null)) link-"+ofport5+":eth1-HPClus2SDN:(null) ("+ofport5+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p6:0 direct link-"+ofport6+":eth1-HPClus2SDN(null) ("+ofport6+"/eth1,null)) link-"+ofport6+":eth1-HPClus2SDN:(null) ("+ofport6+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p7:0 direct link-"+ofport7+":eth1-HPClus2SDN(null) ("+ofport7+"/eth1,null)) link-"+ofport7+":eth1-HPClus2SDN:(null) ("+ofport7+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p8:0 direct link-"+ofport8+":eth1-HPClus2SDN(null) ("+ofport8+"/eth1,null)) link-"+ofport8+":eth1-HPClus2SDN:(null) ("+ofport8+"/eth1,null))" );

        System.out.println("End Edges");
        System.out.println("End solution");

    }



    private static void printStaticAssignFileClos(PhyTopo physicalTopo) throws Exception {
        HashMap<String, Double> backboneStatMap = new HashMap<>();
        BufferedWriter bw = new BufferedWriter((new FileWriter("backboneStats")));

        ArrayList<PhyHost> reservedNodes = new ArrayList<>();
        String ofctrl = "pc5c";
        String node1 = "pc9d";
        String ofport1 = "ofport9d";
        String node2 = "pc12d";
        String ofport2 = "ofport12d";
        String node3 = "pc23d";
        String ofport3 = "ofport23d";
        String node4 = "pc3d";
        String ofport4 = "ofport3d";
        String node5 = "pc11a";
        String ofport5 = "ofport11a";
        String node6 = "pc23a";
        String ofport6 = "ofport23a";
        String node7 = "pc21a";
        String ofport7 = "ofport21a";
        String node8 = "pc7a";
        String ofport8 = "ofport7a";

        System.out.println("Nodes:");
        System.out.println("namenode "+node1);
        System.out.println("datanode1 "+node2);
        System.out.println("datanode2 "+node3);
        System.out.println("datanode3 "+node4);
        System.out.println("datanode4 "+node5);
        System.out.println("datanode5 "+node6);
        System.out.println("datanode6 "+node7);
        System.out.println("datanode7 "+node8);
        System.out.println("ofctrl "+ofctrl);
        System.out.println("ovx ovx");

        System.out.println("switch1p1 "+ofport1);
        System.out.println("switch1p2 "+ofport2);
        System.out.println("switch1p3 "+ofport3);
        System.out.println("switch1p4 "+ofport4);
        System.out.println("switch1p5 ofport47d");
        System.out.println("switch1p6 ofport48d");
        System.out.println("switch2p1 ofport47b");
        System.out.println("switch2p2 ofport48b");
        System.out.println("switch3p1 ofport47c");
        System.out.println("switch3p2 ofport48c");
        System.out.println("switch4p1 "+ofport5);
        System.out.println("switch4p2 "+ofport6);
        System.out.println("switch4p3 "+ofport7);
        System.out.println("switch4p4 "+ofport8);
        System.out.println("switch4p5 ofport47a");
        System.out.println("switch4p6 ofport48a");

        System.out.println("End Nodes");

        System.out.println("Edges:");
        System.out.println("linksimple/link1/namenode:0,switch1p1:1 direct link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)" + " link-"+node1+":eth3-"+ofport1+":eth0 ("+node1+"/eth3,eth0)");
        System.out.println("linksimple/link2/datanode1:0,switch1p2:1 direct link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)" + " link-"+node2+":eth3-"+ofport2+":eth0 ("+node2+"/eth3,eth0)");
        System.out.println("linksimple/link3/datanode2:0,switch1p3:1 direct link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)" + " link-"+node3+":eth3-"+ofport3+":eth0 ("+node3+"/eth3,eth0)");
        System.out.println("linksimple/link4/datanode3:0,switch1p4:1 direct link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)" + " link-"+node4+":eth3-"+ofport4+":eth0 ("+node4+"/eth3,eth0)");
        System.out.println("linksimple/link5/datanode4:0,switch4p1:1 direct link-"+node5+":eth3-"+ofport5+":eth0 ("+node5+"/eth3,eth0)" + " link-"+node5+":eth3-"+ofport5+":eth0 ("+node5+"/eth3,eth0)");
        System.out.println("linksimple/link6/datanode5:0,switch4p2:1 direct link-"+node6+":eth3-"+ofport6+":eth0 ("+node6+"/eth3,eth0)" + " link-"+node6+":eth3-"+ofport6+":eth0 ("+node6+"/eth3,eth0)");
        System.out.println("linksimple/link7/datanode6:0,switch4p3:1 direct link-"+node7+":eth3-"+ofport7+":eth0 ("+node7+"/eth3,eth0)" + " link-"+node7+":eth3-"+ofport7+":eth0 ("+node7+"/eth3,eth0)");
        System.out.println("linksimple/link8/datanode7:0,switch4p4:1 direct link-"+node8+":eth3-"+ofport8+":eth0 ("+node8+"/eth3,eth0)" + " link-"+node8+":eth3-"+ofport8+":eth0 ("+node8+"/eth3,eth0)");

        System.out.println("linksimple/link10/switch1p5:1,switch2p1:1 direct link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0) link-ofport47d:eth0-ofport47b:eth0 (ofport47d/eth0,eth0)");
        System.out.println("linksimple/link11/switch1p6:1,switch3p1:1 direct link-ofport48d:eth0-ofport47c:eth0 (ofport48d/eth0,eth0) link-ofport48d:eth0-ofport47c:eth0 (ofport48d/eth0,eth0)");
        System.out.println("linksimple/link12/switch4p5:1,switch2p2:1 direct link-ofport47a:eth0-ofport48b:eth0 (ofport47a/eth0,eth0) link-ofport47a:eth0-ofport48b:eth0 (ofport47a/eth0,eth0)");
        System.out.println("linksimple/link13/switch4p6:1,switch3p2:1 direct link-ofport48a:eth0-ofport48c:eth0 (ofport48a/eth0,eth0) link-ofport48a:eth0-ofport48c:eth0 (ofport48a/eth0,eth0)");

        System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 interswitch link-" + ofctrl + ":eth0-HPClus4Expt:(null) (" + ofctrl + "/eth0,(null)) link-HPCore1:HPClus4Expt (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1)");

        System.out.println("linklan/ofswitchswitch1/switch1p1:0 direct link-"+ofport1+":eth1-HPClus1SDN(null) ("+ofport1+"/eth1,null)) link-"+ofport1+":eth1-HPClus1SDN:(null) ("+ofport1+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p2:0 direct link-"+ofport2+":eth1-HPClus1SDN(null) ("+ofport2+"/eth1,null)) link-"+ofport2+":eth1-HPClus1SDN:(null) ("+ofport2+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p3:0 direct link-"+ofport3+":eth1-HPClus1SDN(null) ("+ofport3+"/eth1,null)) link-"+ofport3+":eth1-HPClus1SDN:(null) ("+ofport3+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p4:0 direct link-"+ofport4+":eth1-HPClus1SDN(null) ("+ofport4+"/eth1,null)) link-"+ofport4+":eth1-HPClus1SDN:(null) ("+ofport4+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p5:0 direct link-ofport47d:eth1-HPClus1SDN(null) (ofport47d/eth1,null)) link-ofport47d:eth1-HPClus1SDN:(null) (ofport47d/eth1,null))" );
        System.out.println("linklan/ofswitchswitch1/switch1p6:0 direct link-ofport48d:eth1-HPClus1SDN(null) (ofport48d/eth1,null)) link-ofport48d:eth1-HPClus1SDN:(null) (ofport48d/eth1,null))" );


        System.out.println("linklan/ofswitchswitch4/switch4p1:0 direct link-"+ofport5+":eth1-HPClus2SDN(null) ("+ofport5+"/eth1,null)) link-"+ofport5+":eth1-HPClus2SDN:(null) ("+ofport5+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch4/switch4p2:0 direct link-"+ofport6+":eth1-HPClus2SDN(null) ("+ofport6+"/eth1,null)) link-"+ofport6+":eth1-HPClus2SDN:(null) ("+ofport6+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch4/switch4p3:0 direct link-"+ofport7+":eth1-HPClus2SDN(null) ("+ofport7+"/eth1,null)) link-"+ofport7+":eth1-HPClus2SDN:(null) ("+ofport7+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch4/switch4p4:0 direct link-"+ofport8+":eth1-HPClus2SDN(null) ("+ofport8+"/eth1,null)) link-"+ofport8+":eth1-HPClus2SDN:(null) ("+ofport8+"/eth1,null))" );
        System.out.println("linklan/ofswitchswitch4/switch4p5:0 direct link-ofport47a:eth1-HPClus2SDN(null) (ofport47a/eth1,null)) link-ofport47a:eth1-HPClus2SDN:(null) (ofport47a/eth1,null))" );
        System.out.println("linklan/ofswitchswitch4/switch4p6:0 direct link-ofport48a:eth1-HPClus2SDN(null) (ofport48a/eth1,null)) link-ofport48a:eth1-HPClus2SDN:(null) (ofport48a/eth1,null))" );

        System.out.println("linklan/ofswitchswitch2/switch2p1:0 direct link-ofport47b:eth1-HPClus3SDN(null) (ofport47d/eth1,null)) link-ofport47b:eth1-HPClus3SDN:(null) (ofport47b/eth1,null))" );
        System.out.println("linklan/ofswitchswitch2/switch2p2:0 direct link-ofport48b:eth1-HPClus3SDN(null) (ofport48d/eth1,null)) link-ofport48b:eth1-HPClus3SDN:(null) (ofport48b/eth1,null))" );

        System.out.println("linklan/ofswitchswitch3/switch3p1:0 direct link-ofport47c:eth1-HPClus4SDN(null) (ofport47c/eth1,null)) link-ofport47c:eth1-HPClus4SDN:(null) (ofport47c/eth1,null))" );
        System.out.println("linklan/ofswitchswitch3/switch3p2:0 direct link-ofport48c:eth1-HPClus4SDN(null) (ofport48c/eth1,null)) link-ofport48c:eth1-HPClus4SDN:(null) (ofport48c/eth1,null))" );



        System.out.println("End Edges");
        System.out.println("End solution");

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

    public static void CreateFatTreeMapper(PhyTopo physicalTopo, String type, PrintWriter pw, int loop) {
        int maxLoops = 12;
        //int degree = 4;
        //VirtTopo virtualTopo = new VirtTopo();
        //virtualTopo.loadFatTreeTopo(degree);
        //Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
        int status = -1;
//        if (type.equals("safe")) {
//            System.out.println("Using Safe allocation.");
//            status = myMapper.allocateSafe();
//        } else if (type.equals("optimal")) {
//            System.out.println("Using Optimal allocation.");
//            status = myMapper.allocateOptimal();
//        } else if (type.equals("fast")) {
//            System.out.println("Using Fast allocation.");
//            status = myMapper.allocateFastSafe();
//        } else if (type.equals("fastopt")) {
//            System.out.println("Using Fast allocation.");
//            status = myMapper.allocateFastOpt();
//        } else if (type.equals("fastpath")) {
//            System.out.println("Using Fast allocation.");
//            status = myMapper.allocateFastSafePaths();
//        } else if (type.equals("linksafe")) {
//            System.out.println("Using link safe  allocation.");
//            status = myMapper.allocateLinkSafe();
//        }
//        if (status == GRB.OPTIMAL) {
//            System.out.println("Success!!!");
//
//        } else {
//            System.out.println("Failed!!!");
//        }
//        /* Below code needs to be enabled to try and embed various fat tree degrees to the substrate network */
//        for (int loop=12; loop <= maxLoops ;loop++) {
            System.out.println("Starting with "+ loop +" loopbacks...");

            for (int degree = 0; ; degree+=2) {
                VirtTopo virtualTopo = new VirtTopo();
                virtualTopo.loadFatTreeTopo(degree);
                Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                if (type.equals("linksafe")) {
                    System.out.println("Using link safe allocation.");
                    status = myMapper.allocateLinkSafe();

                } else {
                    status = myMapper.allocateLinkOptimal();
                }
                System.out.println("Trying FatTree" +degree + "with "+ loop + "loops");
                if (status == GRB.OPTIMAL) {
                    try {
                        int backbone = myMapper.getBackboneAlloc();
                        pw.println(degree + "," + backbone);
                        System.out.println("%%%%%%%%%  "+ degree+","+ backbone);
                        System.out.println("Success!!!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Failed!!!");
                    break;
                }
            }
//        }
    }

    public static void CreateTopoZooMapper (String phyTopoFile, String virtTopoDir, String type, PrintWriter pw,int loop) {
        File folder = new File(virtTopoDir);
        File[] listofFiles = folder.listFiles();
        PhyTopo physicalTopo = new PhyTopo();
        physicalTopo.loadPhyTopology(phyTopoFile, loop);
        for (File file : listofFiles) {
            if (file.isFile()) {
                if (!file.getAbsolutePath().endsWith("gml")) {
                    continue;
                }
                System.out.println("Processing "+ file.getAbsolutePath().toString());
                VirtTopo virtualTopo = new VirtTopo();
                virtualTopo.loadVirtTopologyZoo(file.getAbsolutePath());
                Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                int status = 0;
                if (type.equals("linksafe")) {
                    System.out.println("Using link safe  allocation.");
                    status = myMapper.allocateLinkSafe();

                } else {
                    myMapper.allocateLinkOptimal();
                }
                if (status == GRB.OPTIMAL) {
                    try {
                        int backbone = myMapper.getBackboneAlloc();
                        pw.println(virtualTopo.getCoreLinks().size() + "," + file.toString() + ","+ +backbone+", SUCCESS");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    pw.println(virtualTopo.getCoreLinks().size()+","+file.toString() + ", FAIL");
                }
                break;
            }
        }
    }


    public static void CreateRandomGraphMapper (String phyTopoFile, String type, PrintWriter pw) {
        int maxLoops = 24;
        for (int loop = 24; loop <= maxLoops; loop+=12) {
            PhyTopo physicalTopo = new PhyTopo();
            physicalTopo.loadPhyTopology(phyTopoFile, loop);
            System.out.println("Starting with " + loop + " loopbacks...");
            int switches = 70;
            int firstfail = 0;
            int secfail  = 0;
            for (; ; switches += 5) {
                if (firstfail == 0) {
                    int links = 2 * switches;
                    VirtTopo virtualTopo = new VirtTopo();
                    virtualTopo.loadRandomGraphTopo(switches, links);
                    Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
                    int status = myMapper.allocateLinkSafe();
                    if (status == GRB.OPTIMAL) {
                        pw.println(loop + "," + switches + "," + links);
                        System.out.println("Success!!!");
                        firstfail = 1;
                    } else {
                        System.out.println("Failed!!!");
                        firstfail = 1;
                    }

                }
//
//                if (secfail == 0) {
//                    int links = (int) Math.ceil(Math.sqrt(switches) * switches);
//                    VirtTopo virtualTopo = new VirtTopo();
//                    virtualTopo.loadRandomGraphTopo(switches, links);
//                    Mapper myMapper = new Mapper(virtualTopo, physicalTopo);
//                    int status = myMapper.allocateOptimal();
//                    if (status == GRB.OPTIMAL) {
//                        pw.println(loop + "," + switches + "," + links);
//                        System.out.println("Success!!!");
//
//                    } else {
//                        System.out.println("Failed!!!");
//                        secfail = 1;
//                    }
//                }

                if (firstfail == 1){// && secfail == 1) {
                    break;
                }
            }

        }
    }
}

