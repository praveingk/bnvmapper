package PhysicalTopo;

import Core.Global;
import Utils.LinkType;
import VirtualTopo.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by pravein on 15/12/16.
 */
public class PhyTopo {
    private ArrayList<PhyHost> Hosts = new ArrayList<>();
    private ArrayList<PhySwitch> Switches = new ArrayList<>();
    private ArrayList<PhySwitchPort> switchPorts = new ArrayList<>();

    private ArrayList<PhyCoreLink> coreLinks = new ArrayList<>();
    private ArrayList<PhyCoreLink> backboneLinks = new ArrayList<>();
    private ArrayList<PhyHostLink> hostLinks = new ArrayList<>();
    private ArrayList<PhySwitchPort> coreSwitchPorts = new ArrayList<>();
    private ArrayList<PhySwitchPort> sdncoreSwitchPorts = new ArrayList<>();
    private HashSet<PhySwitchPortPair> diffSwitchPorts = new HashSet<>();
    private HashMap<String, PhyHost> HostMapper = new HashMap<>();
    private HashMap<String, PhySwitch> SwitchMapper = new HashMap<>();
    private HashMap<String, PhySwitchPort> SwitchPortMapper = new HashMap<>();
    private HashMap<String, Double> backboneStatMap = new HashMap<>();
    private HashMap<String, Double> PCPStatMap = new HashMap<>();
    private ArrayList<PhyCorePath> corePaths = new ArrayList<>();

    private ArrayList<PhyLinkPair> coreLinkPairs = new ArrayList<>();

    public ArrayList<PhyHost> getHosts() {
        return Hosts;
    }
    public ArrayList<PhySwitch> getSwitches() {
        return Switches;
    }
    public ArrayList<PhySwitchPort> getSwitchPorts() {
        return switchPorts;
    }
    public ArrayList<PhyCoreLink> getCoreLinks() {
        return coreLinks;
    }
    public ArrayList<PhyHostLink> getHostLinks() {
        return hostLinks;
    }
    public ArrayList<PhyCorePath> getCorePaths() { return corePaths; }
    public String ncl_environment = "STAGING";

    public String backboneStats = "backboneStats";
    public void findCoreSwitchPorts() {
        for (int i=0;i<coreLinks.size();i++) {
            PhySwitchPort[] psp = coreLinks.get(i).getEndPoints();
            if (!coreSwitchPorts.contains(psp[0])) {
                coreSwitchPorts.add(psp[0]);
            }
            if (!coreSwitchPorts.contains(psp[1])) {
                coreSwitchPorts.add(psp[1]);
            }
        }

    }

    public void pruneOldExptStat() {
        String line = null;
        File tempFile = new File("tempStats");
        File backBoneFile = new File(backboneStats) ;
        try {
            BufferedReader br = new BufferedReader((new FileReader(backBoneFile)));
            BufferedWriter bw = new BufferedWriter((new FileWriter(tempFile)));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                /* Indicator is a PhyHost, which indicates whether an experiment is still in effect */

                String indicator = tokens[0];
                System.out.println("Checking "+ indicator);
                if (this.HostMapper.containsKey(indicator)) {
                    /* Means this indicator has become free, Implies that the expt is done. Hence, this line is no longer valid */
                    System.out.println("Found a host, who is now free..");
                    continue;
                }
                // If it came here, means this link is still being used.
                bw.write(line + System.getProperty("line.separator"));
            }
            br.close();
            bw.close();
            tempFile.renameTo(backBoneFile);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void getUsedBackboneStats() {
        String line = null;
        pruneOldExptStat();
        try {
            BufferedReader br = new BufferedReader((new FileReader(backboneStats)));
            while ((line = br.readLine()) != null) {
                String []tokens = line.split(",");
                /* Indicator is a PhyHost, which indicates whether an experiment is still in effect */

                String indicator = tokens[0];
                String linkString = tokens[1];
                Double occupied = Double.parseDouble(tokens[2]);
                System.out.println("Used Backbone : "+ tokens[1] + ": "+ tokens[2]);
                if (!backboneStatMap.containsKey(linkString)) {
                    backboneStatMap.put(linkString, occupied);
                } else {
                    Double cap = backboneStatMap.get(linkString);
                    backboneStatMap.put(linkString, occupied+cap);
                }
                String endpoint = linkString.split("-")[0];
                System.out.println(endpoint);
                if (!PCPStatMap.containsKey(endpoint)) {
                    PCPStatMap.put(endpoint, occupied);
                } else {
                    Double cap = PCPStatMap.get(endpoint);
                    PCPStatMap.put(endpoint, occupied+cap);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void findDiffSwitchPorts() {
        for (int i=0;i<Switches.size();i++) {
            for (int j=0;j<Switches.size();j++) {
                if (Switches.get(i).equals(Switches.get(j))) continue;
                ArrayList<PhySwitchPort> switch1Ports = Switches.get(i).getSwitchPorts();
                ArrayList<PhySwitchPort> switch2Ports = Switches.get(j).getSwitchPorts();
                for (PhySwitchPort port1 : switch1Ports) {
                    for (PhySwitchPort port2 : switch2Ports) {
                        diffSwitchPorts.add(new PhySwitchPortPair(port1,port2));
                    }
                }
            }
        }
        //System.out.println("Printing Diff SwitchPorts : Total = "+ diffSwitchPorts.size());
//        for (PhySwitchPortPair diffSwitchPort :diffSwitchPorts) {
//            System.out.println(diffSwitchPort.toString());
//        }
    }
    public ArrayList<PhySwitchPort> getCoreSwitchPorts() {
        return coreSwitchPorts;
    }

    public ArrayList<PhyCoreLink> getBackboneLinks() { return backboneLinks; }

    public HashSet<PhySwitchPortPair> getDiffSwitchPorts() { return diffSwitchPorts; }
    public void enableAllLinks() {
        for (int i = 0; i < coreLinks.size(); i++) {
            coreLinks.get(i).enableLink();
        }
    }

    public void disableAllLinks() {
        for (int i = 0; i < coreLinks.size(); i++) {
            coreLinks.get(i).disableLink();
        }
    }

    public void groupAllLinkPairs() {
        for (PhyCoreLink pcl1 : coreLinks) {
            for (PhyCoreLink pcl2 : coreLinks) {
                if (pcl1.isRev(pcl2)) {
                    PhyLinkPair plp = new PhyLinkPair(pcl1, pcl2);
                    if (!coreLinkPairs.contains(plp))
                        coreLinkPairs.add(plp);
                }
            }
        }
        System.out.println("Core link pairs..");
        for (PhyLinkPair vcl : coreLinkPairs) {
            System.out.println(vcl.toString());
        }

        for (PhyCorePath pcp : getCorePaths()) {
            System.out.println(pcp.toString());
        }
    }
    public ArrayList<PhyLinkPair> getCoreLinkPairs() {
        return coreLinkPairs;
    }
    public void populateBackboneLinks() {

        for (int i=0;i< coreLinks.size();i++) {
            PhySwitchPort[] switchPorts = coreLinks.get(i).getEndPoints();
            PhySwitch sw1 = switchPorts[0].getParentSwitch();
            PhySwitch sw2 = switchPorts[1].getParentSwitch();
            if (!sw1.equals(sw2)) {
                /* Physical Core link */
                if (!backboneLinks.contains(coreLinks.get(i))) {
                    backboneLinks.add(coreLinks.get(i));
                    System.out.println("Backbone link : " + coreLinks.get(i).toString());
                }
            }
        }

    }

    public void setLoopbackCount(int loop) {
        HashMap<String,Integer> SwitchMap = new HashMap<>();
        this.disableAllLinks();
        int dis = 0;
        for (int i=0;i< coreLinks.size();i++) {
            PhyCoreLink pcl = coreLinks.get(i);
            Double capacity = pcl.getCapacity();
            PhySwitchPort[] switchPorts = pcl.getEndPoints();
            PhySwitch sw1 = switchPorts[0].getParentSwitch();
            PhySwitch sw2 = switchPorts[1].getParentSwitch();
            System.out.println("CORElink :"+ coreLinks.get(i).toString());
            if (!sw1.equals(sw2)) {
                /* Physical Core link */
                System.out.println("Checking for "+ pcl.getLinkIdentifier());
                if (backboneStatMap.containsKey(pcl.getLinkIdentifier())) {
                    double used = backboneStatMap.get(pcl.getLinkIdentifier());
                    capacity = capacity - used;
                    System.out.println("Used "+used + " of "+ pcl.getLinkIdentifier());
                }
                if (capacity <= 0) {
                    System.out.println(pcl.toString() +" is not available.");
                    break;
                }
                pcl.setCapacity(capacity);
                System.out.println(pcl.toString());
                PhyCorePath pcp1 = new PhyCorePath(switchPorts[0],pcl.getCapacity());
                PhyCorePath pcp2 = new PhyCorePath(switchPorts[1],pcl.getCapacity());
                if (!corePaths.contains(pcp1)) {
                    System.out.println("Adding Core Path "+ pcp1.toString() + "Capacity ="+ pcp1.getCapacity());
                    if ((PCPStatMap.containsKey(pcp1.attachPoint.getID()))) {
                        if (capacity - PCPStatMap.get(pcp1.attachPoint.getID()) > 0 ) {
                            pcp1.setCapacity(capacity - PCPStatMap.get(pcp1.attachPoint.getID()));
                            System.out.println("After Updating Cap: "+ pcp1.toString());
                            corePaths.add(pcp1);
                            sw1.addCorePath(pcp1);
                        }
                    } else {
                        corePaths.add(pcp1);
                        sw1.addCorePath(pcp1);
                    }
                }
                if (!corePaths.contains(pcp2)) {
                    System.out.println("Adding Core Path "+ pcp2.toString() + "Capacity ="+ pcp2.getCapacity());
                    if ((PCPStatMap.containsKey(pcp2.attachPoint.getID()))) {
                        if (capacity - PCPStatMap.get(pcp2.attachPoint.getID()) > 0 ) {
                            pcp2.setCapacity(capacity - PCPStatMap.get(pcp2.attachPoint.getID()));
                            System.out.println("After Updating Cap: "+ pcp2.toString());
                            corePaths.add(pcp2);
                            sw2.addCorePath(pcp2);
                        }
                    } else {
                        corePaths.add(pcp2);
                        sw2.addCorePath(pcp2);
                    }
                }
//                if (!corePaths.contains(pcp1)) {
//                    System.out.println("Adding Core Path "+ pcp1.toString() + "Capacity ="+ pcp1.getCapacity());
//                    corePaths.add(pcp1);
//                    sw1.addCorePath(pcp1);
//
//                } else {
//                    int index = corePaths.indexOf(pcp1);
//                    PhyCorePath pcp = corePaths.get(index);
//                    pcp.setCapacity(pcl.getCapacity());
//                    System.out.println("Updating Core Path "+ pcp1.toString() + "Capacity ="+ pcp1.getCapacity());
//
//                }
//                if (!corePaths.contains(pcp2)) {
//                    System.out.println("Updating Core Path "+ pcp2.toString()+ "Capacity = "+ pcp1.getCapacity());
//
//                    corePaths.add(pcp2);
//                    sw2.addCorePath(pcp2);
//                } else {
//                    int index = corePaths.indexOf(pcp2);
//                    PhyCorePath pcp = corePaths.get(index);
//                    pcp.setCapacity(pcl.getCapacity());
//                }
                coreLinks.get(i).enableLink();

                backboneLinks.add(coreLinks.get(i));
                System.out.println("Backbone link : "+i+" "+ coreLinks.get(i).toString());
                continue;
            }
            /* Must be a loop-back link */
            Integer count = SwitchMap.get(sw1.getID());
            if (count == null)
                count =0;
            if (count < loop) {
                /* Already disable enough loop ports */
                coreLinks.get(i).enableLink();
                //System.out.println("enabling link " + coreLinks.get(i).toString());
                dis++;
                count++;
                SwitchMap.put(sw1.getID(),count);
            }

        }
    }

    public boolean isNCLHost(String name) {
        if (name.contains("pc")) return true;
        return false;
    }

    public boolean isNCLSwitch(String name) {
        if (name.contains("SDN")) return true;
        return false;
    }

    public boolean isNCLSwitchPort(String name) {
        if (name.contains("ofport")) return true;
        return false;
    }

    public boolean isSDNCORE(String name) {
        if (name.contains("sdncore")) return true;
        return false;
    }

    public void dumpLinks() {
        System.out.println("Dumping Links..:");
        for (PhySwitch mySwitch : Switches) {
            System.out.println(mySwitch.toString());
            System.out.println(mySwitch.getHostLinks().toString());
            System.out.println(mySwitch.getCoreLinks().toString());
            System.out.println("-----------------------------------");
        }
    }
    public void loadPhyTopologyNCL (String phyTopoFile) {
        System.out.println("Loading Physical Topology..");
        try {
            BufferedReader br = new BufferedReader((new FileReader(phyTopoFile)));
            String line;
            int hostLinkNum = 0;
            int coreLinkNum = 0;
            int connectedIndex = 13;
            int linkIndex = 5;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    /* Ignore Comments */
                    continue;
                }

                if (line.contains("HPCore1")) {
                    ncl_environment = "PRODUCTION";
                    System.out.println("Identified Environment as Production!");
                }
                String []tokens = line.split(" ");
                String type = tokens[0];
                if (type.equals("node")) {
                    // Could be Host, Switch-Port or Switch
                    if (isNCLSwitch(tokens[1]) && tokens[2].contains("switch")) {
                        // Its definitely SDN Switch
                        String switchName = tokens[1];
                        int tcap = 3000; // Hard-code to 3000
                        PhySwitch ps = new PhySwitch(switchName);
                        ps.setTcamCapacity(tcap);
                        Switches.add(ps);
                        SwitchMapper.put(switchName, ps);
                        System.out.println(ps.toString());
                    } else if (isNCLSwitchPort(tokens[1])) {
                        // Its definitely a  Switch-Port
                        String switchPort = tokens[1];
                        for (int i=0;i<tokens.length;i++) {
                            if (isNCLSwitch(tokens[i])) {
                                // SDN Switch which is the parent
                                String SwitchCase = tokens[i].substring(connectedIndex);
                                String pSwitch = SwitchCase.split(":")[0];
                                PhySwitch sw = SwitchMapper.get(pSwitch);
                                PhySwitchPort psp = new PhySwitchPort(switchPort, sw);
                                switchPorts.add(psp);
                                sw.addSwitchPort(psp);
                                SwitchPortMapper.put(switchPort, psp);
                                System.out.println(psp.toString());
                                break;
                            }
                        }
                    } else if (isNCLHost(tokens[1])) {
                        // Its a Host
                        String hostname = tokens[1];
                        PhyHost ph = new PhyHost(hostname);
                        ph.setVMCap(1);
                        Hosts.add(ph);
                        HostMapper.put(hostname, ph);
                        System.out.println(ph.toString());
                    }
                } else if (type.equals("link")) {
                    String linkEndPoint = tokens[1].substring(linkIndex).split(":")[0];
                    //System.out.println("Link "+ linkEndPoint);
                    if (isNCLHost(linkEndPoint)) {
                        // Host Links
                        for (int i=2;i<tokens.length;i++) {
                            if (isNCLSwitchPort(tokens[i])) {
                                String switchPort = tokens[i].split(":")[0];
                                //System.out.println(" Connected to "+ switchPort);
                                PhySwitchPort psp = SwitchPortMapper.get(switchPort);
                                if (psp == null) break;
                                String linkID = "HostLink"+hostLinkNum++;
                                PhyHostLink phl = new PhyHostLink(linkID, psp, HostMapper.get(linkEndPoint));
                                phl.setCapacity((double)1);
                                hostLinks.add(phl);
                                psp.getParentSwitch().addHostLink(phl);
                                System.out.println(phl.toString());
                                break;
                            }
                        }
                    } else if (isNCLSwitchPort(linkEndPoint)) {
                        // Core Links
                        for (int i=2;i<tokens.length;i++) {
                            if (isNCLSwitchPort(tokens[i])) {
                                String switchPort = tokens[i].split(":")[0];
                                if (switchPort.equals(linkEndPoint)) continue;
                                System.out.println(" Creating a core link b/w "+ switchPort +" and "+  linkEndPoint);
                                PhySwitchPort psp = SwitchPortMapper.get(switchPort);
                                if (psp == null) {
                                    System.out.println(switchPort+" is null");
                                    break;
                                }

                                if (SwitchPortMapper.get(linkEndPoint) ==null) {
                                    System.out.println(linkEndPoint+" is null");
                                    break;
                                }
                                String linkID = "CoreLink"+coreLinkNum++;
                                PhyCoreLink pcl = new PhyCoreLink(linkID, psp, SwitchPortMapper.get(linkEndPoint));
                                pcl.setCapacity((double)1);
                                psp.getParentSwitch().addCoreLink(pcl);
                                if (pcl.linkType == LinkType.CORE) {
                                    linkID = "CoreLink"+coreLinkNum++;
                                    Double capacity = 10.0;
                                    pcl.setCapacity(capacity);
                                    PhyCoreLink pclrev = new PhyCoreLink(linkID, SwitchPortMapper.get(linkEndPoint), psp);
                                    pclrev.setCapacity(capacity);
                                    SwitchPortMapper.get(linkEndPoint).getParentSwitch().addCoreLink(pclrev);
                                    coreLinks.add(pclrev);
                                    System.out.println(pclrev.toString());
                                } else {
                                    //SwitchPortMapper.get(linkEndPoint).getParentSwitch().addCoreLink(pcl);
                                    // This should be done for Link Mapper
                                    if (Global.duplex == 1) {
                                        linkID = "CoreLink" + coreLinkNum++;
                                        PhyCoreLink pclrev = new PhyCoreLink(linkID, SwitchPortMapper.get(linkEndPoint), psp);
                                        pclrev.setCapacity((double) 1);
                                        SwitchPortMapper.get(linkEndPoint).getParentSwitch().addCoreLink(pclrev);
                                        coreLinks.add(pclrev);
                                        //System.out.println(pclrev.toString());
                                    }

                                }
                                coreLinks.add(pcl);
                                System.out.println(pcl.toString());
                                break;
                            } else if (isSDNCORE(tokens[i])) {
                                sdncoreSwitchPorts.add(SwitchPortMapper.get(linkEndPoint));
                                PhyCorePath pcp = new PhyCorePath(SwitchPortMapper.get(linkEndPoint), (double)10);
                                corePaths.add(pcp);
                                SwitchPortMapper.get(linkEndPoint).getParentSwitch().addCorePath(pcp);
                                break;
                            }
                        }
                    }

                }
            }
            for (int i=0; i< sdncoreSwitchPorts.size();i++) {
                for (int j=0;j < sdncoreSwitchPorts.size();j++) {
                    if (sdncoreSwitchPorts.get(i).equals(sdncoreSwitchPorts.get(j))) continue;
                    String linkID = "CoreLink"+coreLinkNum++;
                    PhyCoreLink pcl = new PhyCoreLink(linkID, sdncoreSwitchPorts.get(i), sdncoreSwitchPorts.get(j));
                    pcl.setCapacity((double)10);
                    if (!coreLinks.contains(pcl))
                        coreLinks.add(pcl);
                    sdncoreSwitchPorts.get(i).getParentSwitch().addCoreLink(pcl);
                    linkID = "CoreLink"+coreLinkNum++;
                    PhyCoreLink pclrev = new PhyCoreLink(linkID, sdncoreSwitchPorts.get(j), sdncoreSwitchPorts.get(i));
                    pclrev.setCapacity((double)10);
                    if (!coreLinks.contains(pclrev))
                        coreLinks.add(pclrev);
                    sdncoreSwitchPorts.get(j).getParentSwitch().addCoreLink(pclrev);

                }
            }
            getUsedBackboneStats();
            findCoreSwitchPorts();
            findDiffSwitchPorts();
            /* This is fixed to 12 loopbacks per switch */
            setLoopbackCount(24);
            groupAllLinkPairs();
            //populateBackboneLinks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void loadPhyTopology (String phyTopoFile, int loop) {
        try {
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
                        PhyHost ph = new PhyHost(hostname);
                        ph.setVMCap(1);
                        Hosts.add(ph);
                        HostMapper.put(hostname, ph);
                        System.out.println(ph.toString());
                        break;
                    case "S":
                        String switchName = tokens[1];
                        int tcap = Integer.parseInt(tokens[2]);
                        PhySwitch ps = new PhySwitch(switchName);
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

                        PhyHost host = null;
                        PhySwitch sw = null;
                        host = HostMapper.get(ep1);
                        sw = SwitchMapper.get(ep2);
                        PhySwitchPort psp = new PhySwitchPort(ep2+port, sw);
                        sw.addSwitchPort(psp);
                        switchPorts.add(psp);
                        if (host != null && sw != null) {
                            PhyHostLink phl = new PhyHostLink(linkID, psp, host);
                            phl.setCapacity(cap);
                            hostLinks.add(phl);
                            psp.getParentSwitch().addHostLink(phl);
                            System.out.println(phl.toString());
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

                        PhySwitch sw1 = null;
                        PhySwitch sw2 = null;
                        sw1 = SwitchMapper.get(ep1);
                        sw2 = SwitchMapper.get(ep2);
                        PhySwitchPort psp1 = new PhySwitchPort(ep1+port1, sw1);
                        sw1.addSwitchPort(psp1);
                        PhySwitchPort psp2 = new PhySwitchPort(ep2+port2, sw2);
                        sw2.addSwitchPort(psp2);
                        switchPorts.add(psp1);
                        switchPorts.add(psp2);
                        if (sw1 != null && sw2 != null) {
                            PhyCoreLink pcl = new PhyCoreLink(linkID, psp1, psp2);
                            pcl.setCapacity(cap);
                            coreLinks.add(pcl);
                            psp1.getParentSwitch().addCoreLink(pcl);
                            System.out.println(pcl.toString());
                            if (pcl.linkType == LinkType.CORE) {
                                linkID = "CoreLink"+coreLinkNum++;
                                PhyCoreLink pclrev = new PhyCoreLink(linkID, psp2, psp1);
                                pcl.setCapacity(cap);
                                pclrev.setCapacity(cap);
                                coreLinks.add(pclrev);
                                psp2.getParentSwitch().addCoreLink(pclrev);
                                System.out.println(pclrev.toString());
                            } else {
                                /* Below only link Mapper */
                                if (Global.duplex == 1) {
                                    linkID = "CoreLink" + coreLinkNum++;
                                    PhyCoreLink pclrev = new PhyCoreLink(linkID, psp2, psp1);
                                    pclrev.setCapacity(cap);
                                    coreLinks.add(pclrev);
                                    psp2.getParentSwitch().addCoreLink(pclrev);
                                    System.out.println(pclrev.toString());
                                }
                            }
                        } else {
                            System.out.println("Error in Line :"+ line + " Cannot find Mapping!");
                        }
                        break;
                }
            }
            findCoreSwitchPorts();
            findDiffSwitchPorts();
            setLoopbackCount(loop);
            dumpLinks();
            groupAllLinkPairs();
            //populateBackboneLinks();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
