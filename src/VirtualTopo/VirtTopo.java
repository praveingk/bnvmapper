package VirtualTopo;

import PhysicalTopo.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pravein on 15/12/16.
 */
public class VirtTopo {
    private ArrayList<VirtHost> Hosts = new ArrayList<>();
    private ArrayList<VirtSwitch> Switches = new ArrayList<>();
    private ArrayList<VirtCoreLink> coreLinks = new ArrayList<>();
    private ArrayList<VirtHostLink> hostLinks = new ArrayList<>();

    private HashMap<String, VirtHost> HostMapper = new HashMap<>();
    private HashMap<String, VirtSwitch> SwitchMapper = new HashMap<>();

    public void loadPhyTopology (String phyTopoFile) {
        try {
            BufferedReader br = new BufferedReader((new FileReader(phyTopoFile)));
            String line;
            int hostLinkNum = 0;
            int coreLinkNum = 0;
            while ((line = br.readLine()) != null) {
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
                        VirtSwitch ps = new VirtSwitch(switchName);
                        Switches.add(ps);
                        SwitchMapper.put(switchName, ps);
                        System.out.println(ps.toString());
                        break;
                    case "HL":
                        hostLinkNum++;
                        String linkID = "HostLink"+ hostLinkNum;
                        String ep1 = tokens[1];
                        String []swTok = tokens[2].split("/");
                        String ep2 = swTok[0];
                        String port = swTok[1];

                        VirtHost host = null;
                        VirtSwitch sw = null;
                        host = HostMapper.get(ep1);
                        sw = SwitchMapper.get(ep2);
                        VirtSwitchPort psp = new VirtSwitchPort(ep2+port, sw);
                        sw.addSwitchPort(psp);
                        if (host != null && sw != null) {
                            VirtHostLink phl = new VirtHostLink(linkID, psp, host);
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
                        if (sw1 != null && sw2 != null) {
                            VirtCoreLink pcl = new VirtCoreLink(linkID, psp1, psp2);
                            coreLinks.add(pcl);
                            System.out.println(pcl.toString());
                        } else {
                            System.out.println("Error in Line :"+ line + " Cannot find Mapping!");
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
