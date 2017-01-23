package PhysicalTopo;

import VirtualTopo.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pravein on 15/12/16.
 */
public class PhyTopo {
    private ArrayList<PhyHost> Hosts = new ArrayList<>();
    private ArrayList<PhySwitch> Switches = new ArrayList<>();
    private ArrayList<PhySwitchPort> switchPorts = new ArrayList<>();

    private ArrayList<PhyCoreLink> coreLinks = new ArrayList<>();
    private ArrayList<PhyHostLink> hostLinks = new ArrayList<>();
    private ArrayList<PhySwitchPort> coreSwitchPorts = new ArrayList<>();
    private HashMap<String, PhyHost> HostMapper = new HashMap<>();
    private HashMap<String, PhySwitch> SwitchMapper = new HashMap<>();


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

    public void setLoopbackCount(int loop) {
        HashMap<String,Integer> SwitchMap = new HashMap<>();
        this.disableAllLinks();
        int dis = 0;

        for (int i=0;i< coreLinks.size();i++) {
            PhySwitchPort[] switchPorts = coreLinks.get(i).getEndPoints();
            PhySwitch sw1 = switchPorts[0].getParentSwitch();
            PhySwitch sw2 = switchPorts[1].getParentSwitch();
            System.out.println(coreLinks.get(i).isEnabled());
            if (!sw1.equals(sw2)) {
                /* Physical Core link */
                coreLinks.get(i).enableLink();
                continue;
            }
            /* Must be a loop-back link */
            Integer count = SwitchMap.get(sw1.getID());
            if (count == null)
                count =0;
            if (count < loop) {
                /* Already disable enough loop ports */
                coreLinks.get(i).enableLink();
                System.out.println("enabling link " + coreLinks.get(i).toString());
                dis++;
                count++;
                SwitchMap.put(sw1.getID(),count);
            }

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
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
