package VirtualTopo;

import Core.Global;
import PhysicalTopo.*;
import Utils.LinkType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
/**
 * Created by pravein on 15/12/16.
 */
public class VirtTopo {
    private ArrayList<VirtHost> Hosts = new ArrayList<>();
    private ArrayList<VirtSwitch> Switches = new ArrayList<>();
    private ArrayList<VirtSwitchPort> switchPorts = new ArrayList<>();
    private ArrayList<VirtCoreLink> coreLinks = new ArrayList<>();
    private ArrayList<VirtHostLink> hostLinks = new ArrayList<>();

    private HashMap<String, VirtHost> HostMapper = new HashMap<>();
    private HashMap<String, VirtSwitch> SwitchMapper = new HashMap<>();
    private HashMap<String, VirtSwitchPort> SwitchPortMapper = new HashMap<>();

    public ArrayList<VirtHost> getHosts() {
        return Hosts;
    }
    public ArrayList<VirtSwitch> getSwitches() {
        return Switches;
    }
    public ArrayList<VirtSwitchPort> getSwitchPorts() {
        return switchPorts;
    }
    public ArrayList<VirtCoreLink> getCoreLinks() {
        return coreLinks;
    }
    public ArrayList<VirtHostLink> getHostLinks() {
        return hostLinks;
    }

    public ArrayList<VirtLinkPair> coreLinkPairs = new ArrayList<>();

    public void setTCAMCaps() {
        for (int i=0;i< this.Switches.size();i++) {
            VirtSwitch mySwitch = this.Switches.get(i);

            int indivTcam = Math.round(mySwitch.getTCAMCapacity() / mySwitch.getSwitchPorts().size());
            for (int j=0;j< mySwitch.getSwitchPorts().size();j++) {
                mySwitch.getSwitchPorts().get(j).setTCAM(indivTcam);
            }
            for (int j=0;j< mySwitch.getCoreLinks().size();j++) {
                mySwitch.getCoreLinks().get(j).setTCAM(indivTcam);
            }
            for (int j=0;j< mySwitch.getHostLinks().size();j++) {
                mySwitch.getHostLinks().get(j).setTCAM(indivTcam);
            }
        }
    }


    public void addEdge(VirtSwitch Switch1, VirtSwitch Switch2, Double Bandwidth) {
        int curPort1 = Switch1.getSwitchPorts().size();
        int curPort2 = Switch2.getSwitchPorts().size();

        VirtSwitchPort Switch1Port = new VirtSwitchPort(Switch1.getID()+"/"+curPort1, Switch1);
        VirtSwitchPort Switch2Port = new VirtSwitchPort(Switch2.getID()+"/"+curPort2, Switch2);
        Switch1.addSwitchPort(Switch1Port);
        Switch2.addSwitchPort(Switch2Port);
        switchPorts.add(Switch1Port);
        switchPorts.add(Switch2Port);
        int linkNum = coreLinks.size();
        VirtCoreLink coreLink = new VirtCoreLink("clink"+linkNum, Switch1Port, Switch2Port);
        coreLink.setBandWidth(Bandwidth);
        coreLinks.add(coreLink);
        Switch1Port.getParentSwitch().addCoreLink(coreLink);

        if (Global.duplex == 1) {
            linkNum++;
            VirtCoreLink coreLinkRev = new VirtCoreLink("clink"+linkNum, Switch2Port, Switch1Port);
            coreLinkRev.setIsRev();
            coreLinkRev.setBandWidth(Bandwidth);
            coreLinks.add(coreLinkRev);
            Switch2Port.getParentSwitch().addCoreLink(coreLinkRev);
        }
    }

    public void addEdge(VirtSwitch Switch1, VirtHost host, Double Bandwidth) {
        int curPort = Switch1.getSwitchPorts().size();


        VirtSwitchPort Switch1Port = new VirtSwitchPort(Switch1.getID()+"/"+curPort, Switch1);
        Switch1.addSwitchPort(Switch1Port);
        switchPorts.add(Switch1Port);
        int linkNum = hostLinks.size();
        VirtHostLink hostLink = new VirtHostLink("hlink"+linkNum, Switch1Port, host);
        hostLink.setBandWidth(Bandwidth);
        hostLinks.add(hostLink);
        Switch1Port.getParentSwitch().addHostLink(hostLink);

    }

    public int indexOf(VirtSwitchPort mySwitchPort) {
        if (!switchPorts.contains(mySwitchPort)) {
            return -1;
        }
        return switchPorts.indexOf(mySwitchPort);
    }

    public void loadRandomGraphTopo(int numSwitches, int numLinks) {
        int SwitchRuleSize = 100;
        System.out.println("Creating a random graph with "+ numSwitches +" and link : "+ numLinks);
        ArrayList<String> links = new ArrayList<>();
        for (int i=0;i< numSwitches; i++) {
            VirtSwitch mySwitch = new VirtSwitch("Switch"+i);
            mySwitch.setTcamCapacity(SwitchRuleSize);
            Switches.add(mySwitch);
            SwitchMapper.put(mySwitch.getID(), mySwitch);
            System.out.println(mySwitch.toString());
        }
        Random r = new Random();
        for (int i=0;i< numLinks;i++) {
            while(true) {
                String switch1 = "Switch" + r.nextInt(numSwitches);
                String switch2 = "Switch" + r.nextInt(numSwitches);
                String linkholder = switch1 + switch2;
                String linkholder2 = switch2+ switch1;
                if (switch1.equals(switch2)) {
                    continue;
                }
                if (links.contains(linkholder) || links.contains(linkholder2)) {
                    continue;
                }
                links.add(linkholder);
                links.add(linkholder2);
                if (SwitchMapper.get(switch1) == null) {
                    System.out.println(switch1+"  is null");
                }
                addEdge(SwitchMapper.get(switch1), SwitchMapper.get(switch2), 1.0);
                System.out.println("Created link between "+ switch1+ " and "+ switch2);
                break;
            }
        }
        setTCAMCaps();
        dumpLinks();
        groupAllLinkPairs();
    }
    public  void loadFatTreeTopo(int degree) {

        System.out.println("Loading FatTree with degree "+ degree);
        List<VirtSwitch> coreSwitches = new ArrayList<VirtSwitch>(degree);
        List<VirtSwitch> edgeSwitches = new ArrayList<VirtSwitch>();

        int SwitchRuleSize = 100;
		/* Create Core Switches */
        int coresNum = (degree / 2) * (degree / 2); // (k/2)^2
        System.out.println("Creating Core Switches : ");
        for (int i = 0; i < coresNum; i++) {
            VirtSwitch vertex = new VirtSwitch("Switch"+i);
            vertex.setTcamCapacity(SwitchRuleSize);
            Switches.add(vertex);
            SwitchMapper.put(vertex.getID(), vertex);
            coreSwitches.add(vertex);
            System.out.println("Switch" +i);

        }

        int switch_pr = coreSwitches.size();
		/* Create Pods */
        System.out.println("Creating Pods : ");
        for (int i = 0; i < degree; i++) {
            //System.out.println("Pod " + i);
            List<VirtSwitch> podSwitches = new ArrayList<VirtSwitch>(degree);
            //pod_prefix = ipod_prefix * (i+1);
            //edge_prefix = iedge_prefix *(i+1);
            for (int j = 0; j < degree / 2; j++) {
                VirtSwitch aggSw = new VirtSwitch("Switch"+switch_pr++);//(pod_prefix + j);
                aggSw.setTcamCapacity(SwitchRuleSize);
                Switches.add(aggSw);
                SwitchMapper.put(aggSw.getID(), aggSw);
                podSwitches.add(aggSw);
                //System.out.println("AGG " + aggSw.getID());

				/* Create Link from core to Pods */
                for (int t = 0; t < degree / 2; t++) {
                    addEdge(aggSw, coreSwitches.get(j * degree / 2 + t), 1.0);
                    //addEdge(coreSwitches.get(j * degree / 2 + t), aggSw, 1.0);
                }
            }

            for (int j = 0; j < degree / 2; j++) {
                VirtSwitch edgesw = new VirtSwitch("Switch"+switch_pr++);//(edge_prefix + j);
                edgesw.setTcamCapacity(SwitchRuleSize);
                Switches.add(edgesw);
                SwitchMapper.put(edgesw.getID(), edgesw);
                edgeSwitches.add(edgesw);
                System.out.println("AGG2 " + edgesw.getID());
				/* Intra - Pod Links */
                for (int k = 0; k < degree / 2; k++) {
                    System.out.println("Creating Link bwt " + podSwitches.get(k).getID() + " and " + edgesw.getID());
                    addEdge(podSwitches.get(k), edgesw, 1.0);
                    //addEdge(edgesw, podSwitches.get(k), 1.0);
                }
				/*
				for (int k = 0 ; k < degree/2 ; k++) {
					BaseVertex edgeSw = new Vertex(edge_prefix + k);
					vertexList.add(edgeSw);
					idVertexIndex.put(edgeSw.getId(), edgeSw);
					System.out.println("Edge Switch : "+ edgeSw.getId());
					addEdge(edgeSw.getId(), agg2Sw.getId(), 1);
					System.out.println("Creating Link bwt "+ edgeSw.getId() + " and " +agg2Sw.getId());
				}*/
            }

        }
        int hostID = 1;
        for (int i=0;i<edgeSwitches.size();i++) {
            VirtHost host1 = new VirtHost("Host"+ hostID);
            Hosts.add(host1);
            HostMapper.put(host1.getID(), host1);
            hostID++;
            VirtHost host2 = new VirtHost("Host"+ hostID);
            Hosts.add(host2);
            HostMapper.put(host2.getID(), host2);
            hostID++;
            System.out.println("Creating hosts "+ host1.getID() + " and "+ host2.getID());
            addEdge(edgeSwitches.get(i), host1, 1.0);
            addEdge(edgeSwitches.get(i), host2, 1.0);
            System.out.println("Creating Link bwt " + edgeSwitches.get(i).getID() + " and " + host1.getID());
            System.out.println("Creating Link bwt " + edgeSwitches.get(i).getID() + " and " + host2.getID());

        }
        System.out.println("Total HostLinks = "+ hostLinks.size());
        System.out.println("Total CoreLinks = "+ coreLinks.size());
        setTCAMCaps();
        dumpLinks();
        groupAllLinkPairs();
    }


    public boolean isNCLHost(String name) {
        if (name.contains("pc")) return true;
        return false;
    }

    public boolean isNCLSwitch(String name) {
        if (name.contains("ofswitch")) return true;
        return false;
    }

    public boolean isNCLSwitchPort(String name) {
        if (name.contains("ofcore") || name.contains("ofedge")) return true;
        return false;
    }
    public void groupAllLinkPairs() {
        for (VirtCoreLink pcl1 : coreLinks) {
            for (VirtCoreLink pcl2 : coreLinks) {
                if (pcl1.isRev(pcl2)) {
                    VirtLinkPair plp = new VirtLinkPair(pcl1, pcl2);
                    if (!coreLinkPairs.contains(plp))
                        coreLinkPairs.add(plp);
                }
            }
        }
        System.out.println("Core link pairs..");
        for (VirtLinkPair vcl : coreLinkPairs) {
            System.out.println(vcl.toString());
        }
    }
    public ArrayList<VirtLinkPair> getCoreLinkPairs() {
        return coreLinkPairs;
    }
    public void dumpLinks() {
        System.out.println("Dumping Links..:");
        for (VirtSwitch mySwitch : Switches) {
            System.out.println(mySwitch.toString());
            System.out.println(mySwitch.getHostLinks().toString());
            System.out.println(mySwitch.getCoreLinks().toString());
            System.out.println("-----------------------------------");
        }
    }
    public void loadVirtTopologyNCL (String phyTopoFile) {
        System.out.println("Loading Virtual Topology..");
        try {
            BufferedReader br = new BufferedReader((new FileReader(phyTopoFile)));
            String line;
            int hostLinkNum = 0;
            int coreLinkNum = 0;
            int connectedIndex = 13;
            int linkIndex = 5;
            // Need One More pass for reading the switches and Ports
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    /* Ignore Comments */
                    continue;
                }
                String[] tokens = line.split(" ");
                String type = tokens[0];
                if (type.equals("link") ) {
                    String linkName = tokens[1].split("/")[1];
                    if (isNCLSwitch(linkName)) {
                        VirtSwitch ps = null;
                        if (SwitchMapper.containsKey(linkName)) {
                            ps = SwitchMapper.get(linkName);
                        } else {
                            ps = new VirtSwitch(linkName);
                            ps.setTcamCapacity(100);
                            Switches.add(ps);
                            SwitchMapper.put(linkName, ps);
                        }
                        String members = tokens[1].split("/")[2];
                        String[] switchPortsS = members.split(",");
                        System.out.println(ps.toString());
                        for (int i = 0; i < switchPortsS.length; i++) {
                            VirtSwitchPort vsp = new VirtSwitchPort(switchPortsS[i].split(":")[0], ps);
                            SwitchPortMapper.put(switchPortsS[i].split(":")[0], vsp);
                            ps.addSwitchPort(vsp);
                            switchPorts.add(vsp);
                            System.out.println(vsp.toString());
                        }

                    }
                }
            }
            br = new BufferedReader((new FileReader(phyTopoFile)));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    /* Ignore Comments */
                    continue;
                }
                String[] tokens = line.split(" ");
                String type = tokens[0];
                String nodeType = tokens[2];
                if (type.equals("node")) {
                    // Could be Host, Switch-Port or Switch
                    if (isNCLHost(nodeType)) {
                        // Its a Host
                        String hostname = tokens[1];
                        VirtHost ph = new VirtHost(hostname);
                        Hosts.add(ph);
                        HostMapper.put(hostname, ph);
                        System.out.println(ph.toString());
                    }
                } else if (type.equals("link")) {

                    String linkName = tokens[1].split("/")[1];
                    //System.out.println("link "+ linkName);
                    if (!isNCLSwitch(linkName)) {
                        String members = tokens[1].split("/")[2];
                        String[] switchPorts = members.split(",");
                        String node1 = switchPorts[0].split(":")[0];
                        String node2 = switchPorts[1].split(":")[0];
                        //System.out.println(node1 +", "+ node2 );
                        if (HostMapper.containsKey(node1)) {
                            VirtHost host = HostMapper.get(node1);
                            VirtSwitchPort vsp = SwitchPortMapper.get(node2);
                            VirtHostLink vhl = new VirtHostLink(linkName, vsp, host);
                            System.out.println(vhl.toString());
                            vhl.setBandWidth(1.0);
                            hostLinks.add(vhl);
                            vsp.getParentSwitch().addHostLink(vhl);
                        } else if (HostMapper.containsKey(node2)) {
                            VirtHost host = HostMapper.get(node2);
                            VirtSwitchPort vsp = SwitchPortMapper.get(node1);
                            VirtHostLink vhl = new VirtHostLink(linkName, vsp, host);
                            System.out.println(vhl.toString());
                            vhl.setBandWidth(1.0);
                            hostLinks.add(vhl);
                            vsp.getParentSwitch().addHostLink(vhl);
                        } else {
                            VirtSwitchPort vsp1 = SwitchPortMapper.get(node1);
                            VirtSwitchPort vsp2 = SwitchPortMapper.get(node2);
                            if (vsp1 ==null || vsp2 == null) continue;
                            VirtCoreLink vcl = new VirtCoreLink(linkName, vsp1, vsp2);
                            coreLinks.add(vcl);
                            vcl.setBandWidth(1.0);
                            System.out.println(vcl.toString());
                            vsp1.getParentSwitch().addCoreLink(vcl);
                            /* Below code is only for link mapper */
                            if (Global.duplex == 1) {
                                VirtCoreLink vclrev = new VirtCoreLink(linkName, vsp2, vsp1);
                                vclrev.setIsRev();
                                coreLinks.add(vclrev);
                                vclrev.setBandWidth(1.0);
                                System.out.println(vclrev.toString());
                                vsp2.getParentSwitch().addCoreLink(vclrev);
                            }
                        }
                    }
                }
            }
            setTCAMCaps();
            dumpLinks();
            groupAllLinkPairs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void loadVirtTopology (String phyTopoFile) {
        try {
            System.out.println("Loading Virtual Topology..");
            BufferedReader br = new BufferedReader((new FileReader(phyTopoFile)));
            String line;
            int hostLinkNum = 0;
            int coreLinkNum = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    /* Ignore Comments */
                    continue;
                }
                String []tokens = line.split(":");
                String type = tokens[0];
                switch(type) {
                    case "H":
                        String hostname = tokens[1];
                        VirtHost ph = new VirtHost(hostname);
                        Hosts.add(ph);
                        HostMapper.put(hostname, ph);
                        System.out.println(ph.toString());
                        break;
                    case "S":
                        String switchName = tokens[1];
                        int tcap = Integer.parseInt(tokens[2]);
                        VirtSwitch ps = new VirtSwitch(switchName);
                        ps.setTcamCapacity(tcap);
                        Switches.add(ps);
                        SwitchMapper.put(switchName, ps);
                        System.out.println(ps.toString());
                        break;
                    case "HL":
                        hostLinkNum++;
                        String linkID = "HostLink"+ hostLinkNum;
                        String ep1 = tokens[1];
                        String []swTok = tokens[2].split("/");
                        Double cap = Double.parseDouble(tokens[3]);
                        String ep2 = swTok[0];
                        String port = swTok[1];

                        VirtHost host = null;
                        VirtSwitch sw = null;
                        host = HostMapper.get(ep1);
                        sw = SwitchMapper.get(ep2);
                        VirtSwitchPort psp = new VirtSwitchPort(ep2+port, sw);
                        sw.addSwitchPort(psp);
                        switchPorts.add(psp);
                        if (host != null && sw != null) {
                            VirtHostLink phl = new VirtHostLink(linkID, psp, host);
                            phl.setBandWidth(cap);
                            hostLinks.add(phl);
                            System.out.println(phl.toString());
                            psp.getParentSwitch().addHostLink(phl);
                        } else {
                            System.out.println("Error in Line :"+ line + " Cannot find Mapping!");
                        }
                        break;
                    case "CL":
                        coreLinkNum++;
                        linkID = "CoreLink"+ coreLinkNum;
                        String []swTok1 = tokens[1].split("/");
                        String []swTok2 = tokens[2].split("/");
                        cap = Double.parseDouble(tokens[3]);
                        ep1 = swTok1[0];
                        String port1 = swTok1[1];
                        ep2 = swTok2[0];
                        String port2 = swTok2[1];

                        VirtSwitch sw1 = null;
                        VirtSwitch sw2 = null;
                        sw1 = SwitchMapper.get(ep1);
                        sw2 = SwitchMapper.get(ep2);
                        VirtSwitchPort psp1 = new VirtSwitchPort(ep1+port1, sw1);
                        sw1.addSwitchPort(psp1);
                        VirtSwitchPort psp2 = new VirtSwitchPort(ep2+port2, sw2);
                        sw2.addSwitchPort(psp2);
                        switchPorts.add(psp1);
                        switchPorts.add(psp2);
                        if (sw1 != null && sw2 != null) {
                            VirtCoreLink pcl = new VirtCoreLink(linkID, psp1, psp2);
                            pcl.setBandWidth(cap);
                            coreLinks.add(pcl);
                            System.out.println(pcl.toString());
                            psp1.getParentSwitch().addCoreLink(pcl);
                            /* Do not use the below in case of switch-port based mappings */
                            if (Global.duplex == 1) {
                                VirtCoreLink pclrev = new VirtCoreLink(linkID, psp2, psp1);
                                pclrev.setBandWidth(cap);
                                coreLinks.add(pclrev);
                                System.out.println(pclrev.toString());
                                psp2.getParentSwitch().addCoreLink(pclrev);
                            }
                        } else {
                            System.out.println("Error in Line :"+ line + " Cannot find Mapping!");
                        }
                        break;
                }
            }
            setTCAMCaps();
            dumpLinks();
            groupAllLinkPairs();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
