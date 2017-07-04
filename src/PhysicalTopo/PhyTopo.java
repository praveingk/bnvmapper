package PhysicalTopo;

import Utils.LinkType;
import VirtualTopo.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
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
    private HashMap<String, PhyHost> HostMapper = new HashMap<>();
    private HashMap<String, PhySwitch> SwitchMapper = new HashMap<>();
    private HashMap<String, PhySwitchPort> SwitchPortMapper = new HashMap<>();


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

    public String ncl_environment = "STAGING";

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

    public ArrayList<PhySwitchPort> getCoreSwitchPorts() {
        return coreSwitchPorts;
    }

    public ArrayList<PhyCoreLink> getBackboneLinks() { return backboneLinks; }

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

    public void populateBackboneLinks() {

        for (int i=0;i< coreLinks.size();i++) {
            PhySwitchPort[] switchPorts = coreLinks.get(i).getEndPoints();
            PhySwitch sw1 = switchPorts[0].getParentSwitch();
            PhySwitch sw2 = switchPorts[1].getParentSwitch();
            if (!sw1.equals(sw2)) {
                /* Physical Core link */
                backboneLinks.add(coreLinks.get(i));
                System.out.println("Backbone link : "+ coreLinks.get(i).toString());
            }
        }

    }

    public void setLoopbackCount(int loop) {
        HashMap<String,Integer> SwitchMap = new HashMap<>();
        this.disableAllLinks();
        int dis = 0;

        for (int i=0;i< coreLinks.size();i++) {
            PhySwitchPort[] switchPorts = coreLinks.get(i).getEndPoints();
            PhySwitch sw1 = switchPorts[0].getParentSwitch();
            PhySwitch sw2 = switchPorts[1].getParentSwitch();
            if (!sw1.equals(sw2)) {
                /* Physical Core link */
                coreLinks.get(i).enableLink();
                backboneLinks.add(coreLinks.get(i));
                System.out.println("Backbone link : "+ coreLinks.get(i).toString());
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
                                //System.out.println(" Connected to "+ switchPort);
                                PhySwitchPort psp = SwitchPortMapper.get(switchPort);
                                if (psp == null) break;
                                String linkID = "CoreLink"+coreLinkNum++;
                                PhyCoreLink pcl = new PhyCoreLink(linkID, psp, SwitchPortMapper.get(linkEndPoint));
                                pcl.setCapacity((double)1);
                                coreLinks.add(pcl);
                                if (pcl.linkType == LinkType.CORE) {
                                    pcl.setCapacity((double)10);
                                }
                                System.out.println(pcl.toString());
                                break;
                            }
                        }
                    }

                }
            }
            findCoreSwitchPorts();
            /* This is fixed to 12 loopbacks per switch*/
            setLoopbackCount(12);
            populateBackboneLinks();
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
                            System.out.println(pcl.toString());
                        } else {
                            System.out.println("Error in Line :"+ line + " Cannot find Mapping!");
                        }
                        break;
                }
            }
            findCoreSwitchPorts();
            setLoopbackCount(loop);
            populateBackboneLinks();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
