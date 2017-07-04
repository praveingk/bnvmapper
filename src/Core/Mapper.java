package Core;

import PhysicalTopo.*;
import VirtualTopo.*;
import gurobi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pravein on 16/12/16.
 */
public class Mapper {
    VirtTopo virtualTopo;
    PhyTopo physicalTopo;

    private GRBVar[][] switchPortMapper;
    private GRBVar[][] hostMapper;

    private Double[][] switchPortMapperOld;
    private Double[][] hostMapperOld;

    HashMap<VirtHost, PhyHost> hostMapping = new HashMap<>();
    HashMap<VirtSwitchPort, PhySwitchPort> switchPortMapping = new HashMap<>();

    HashMap<String, String> switchCon = new HashMap<>();
    HashMap<String, String> sdnswitchCon = new HashMap<>();

    public Mapper(VirtTopo virtualTopo, PhyTopo physicalTopo) {
        this.virtualTopo = virtualTopo;
        this.physicalTopo = physicalTopo;
        switchCon.put("c", "HPClus4Expt");
        switchCon.put("d", "HPClus1Expt");
        switchCon.put("a", "HPClus2Expt");
        switchCon.put("b", "HPClus3Expt");
        sdnswitchCon.put("c", "HPClus4SDN");
        sdnswitchCon.put("d", "HPClus1SDN");
        sdnswitchCon.put("a", "HPClus2SDN");
        sdnswitchCon.put("b", "HPClus3SDN");
    }

    public int allocateOptimal() {
        GRBEnv env;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("ModelVirt.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {

            env = new GRBEnv();
            //env.set(GRB.IntParam.PreQLinearize, 1);
            env.set(GRB.DoubleParam.TimeLimit, 200);
            GRBModel model = new GRBModel(env);

            switchPortMapper = new GRBVar[virtualTopo.getSwitchPorts().size()][physicalTopo.getSwitchPorts().size()];
            hostMapper = new GRBVar[virtualTopo.getHosts().size()][physicalTopo.getHosts().size()];

            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    String st = "X[" + i + "," + j + "]";
                    switchPortMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    String st = "Y[" + i + "," + j + "]";
                    hostMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }
            model.update();

            /* Constraint 1: Each virtual switch-port should be placed on only one Physical switch-port. */
            GRBLinExpr[] switchPortPlacement = new GRBLinExpr[virtualTopo.getSwitchPorts().size()];
            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                String st = "SwitchPortPlacement-" + i;
                switchPortPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    switchPortPlacement[i].addTerm(1.0, switchPortMapper[i][j]);
                }
                model.addConstr(switchPortPlacement[i], GRB.EQUAL, 1, st);
            }


            /* Constraint 2: Each virtual switch-port should be placed on only one Physical switch-port. */
            GRBLinExpr[] hostPlacement = new GRBLinExpr[virtualTopo.getHosts().size()];
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                String st = "HostPlacement-" + i;
                hostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    hostPlacement[i].addTerm(1.0, hostMapper[i][j]);
                }
                model.addConstr(hostPlacement[i], GRB.EQUAL, 1, st);
            }

            /* Constraint 3: Each Physical switch-port should be used only once. */
//            GRBLinExpr[] physswitchPortPlacement = new GRBLinExpr[physicalTopo.getSwitchPorts().size()];
//            for (int i=0;i< physicalTopo.getSwitchPorts().size();i++) {
//                String st = "phySwitchPortPlacement-" + i;
//                physswitchPortPlacement[i] = new GRBLinExpr();
//                for (int j = 0; j < virtualTopo.getSwitchPorts().size(); j++) {
//                    physswitchPortPlacement[i].addTerm(1.0, switchPortMapper[j][i]);
//                }
//                model.addConstr(physswitchPortPlacement[i], GRB.LESS_EQUAL, 1, st);
//            }


            /* Constraint 3: Each Physical switch-host should be used as per its VM limits. */
            GRBLinExpr[] physhostPlacement = new GRBLinExpr[physicalTopo.getHosts().size()];
            for (int i = 0; i < physicalTopo.getHosts().size(); i++) {
                String st = "phyHostPlacement-" + i;
                PhyHost phyHost = physicalTopo.getHosts().get(i);
                physhostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < virtualTopo.getHosts().size(); j++) {
                    physhostPlacement[i].addTerm(1.0, hostMapper[j][i]);
                }
                model.addConstr(physhostPlacement[i], GRB.LESS_EQUAL, phyHost.getVMCap(), st);
            }



            /* Constraint 4: Each Virtual switch-port which would be connected to a host should have a direct-link */
            for (int i = 0; i < virtualTopo.getHostLinks().size(); i++) {
                String st = "PortHost-" + i;
                System.out.println("hllll "+virtualTopo.getHostLinks().get(i).toString());
                GRBQuadExpr hostLinkPlacement = new GRBQuadExpr();
                VirtHost myHost = virtualTopo.getHostLinks().get(i).getHostPort();
                VirtSwitchPort mySwitchPort = virtualTopo.getHostLinks().get(i).getSwitchPort();
                int switchPortIndex = virtualTopo.getSwitchPorts().indexOf(mySwitchPort);
                int hostIndex = virtualTopo.getHosts().indexOf(myHost);
                System.out.println(" Index of " + mySwitchPort.toString() + " is " + switchPortIndex);
                System.out.println(" Index of " + myHost.toString() + " is " + hostIndex);
                for (int physPortIndex = 0; physPortIndex < physicalTopo.getSwitchPorts().size(); physPortIndex++) {
                    for (int physhostIndex = 0; physhostIndex < physicalTopo.getHosts().size(); physhostIndex++) {
                        hostLinkPlacement.addTerm(isDirectlyConnected(physicalTopo.getSwitchPorts().get(physPortIndex),
                                physicalTopo.getHosts().get(physhostIndex)),
                                switchPortMapper[switchPortIndex][physPortIndex],
                                hostMapper[hostIndex][physhostIndex]);
                    }
                }
                System.out.println("hostlinkplacement");
                model.addQConstr(hostLinkPlacement, GRB.EQUAL, 1.0, st);
            }

            /* Constraint 5 : Satisfy virtual link mapping to physical link constraint */
            for (int i = 0; i < virtualTopo.getCoreLinks().size(); i++) {
                String st = "PortPort-" + i;
                GRBQuadExpr coreLinkPlacement = new GRBQuadExpr();
                VirtSwitchPort[] virtendPoints = virtualTopo.getCoreLinks().get(i).getEndPoints();
                int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                for (int j = 0; j < physicalTopo.getCoreLinks().size(); j++) {
                    if (!physicalTopo.getCoreLinks().get(j).isEnabled()) {
                        continue;
                    }
                    PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(j).getEndPoints();
                    int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                    int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);
                    coreLinkPlacement.addTerm(1.0,
                            switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                }
                model.addQConstr(coreLinkPlacement, GRB.EQUAL, 1.0, st);
            }


            /* Constraint 6 : Link Bandwidth constraints */

            for (int i = 0; i < physicalTopo.getCoreLinks().size(); i++) {
                if (!physicalTopo.getCoreLinks().get(i).isEnabled()) {
                    continue;
                }
                String st = "CoreLink-" + i;
                GRBQuadExpr coreLinkBandwidth = new GRBQuadExpr();
                PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(i).getEndPoints();
                int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);

                /* Link B/W for core-links */
                for (int j = 0; j < virtualTopo.getCoreLinks().size(); j++) {
                    VirtSwitchPort[] virtendPoints = virtualTopo.getCoreLinks().get(j).getEndPoints();
                    int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                    int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                    coreLinkBandwidth.addTerm(virtualTopo.getCoreLinks().get(j).getBandwidth(),
                            switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                }
                /* Link B/w for intra-switch links */

                for (int j=0;j < virtualTopo.getSwitches().size();j++) {
                    ArrayList<VirtSwitchPort> virtSwitchPorts = virtualTopo.getSwitches().get(j).getSwitchPorts();
                    for (int port1 = 0; port1 < virtSwitchPorts.size(); port1++) {
                        for (int port2 = 0; port2 < virtSwitchPorts.size(); port2++) {
                            if (port1 == port2) continue;
                            int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
                            int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
                            coreLinkBandwidth.addTerm(1,
                                    switchPortMapper[virtport1Index][endPoint1Index],
                                    switchPortMapper[virtport2Index][endPoint2Index]);
                        }
                    }
                }


                model.addQConstr(coreLinkBandwidth, GRB.LESS_EQUAL, physicalTopo.getCoreLinks().get(i).getCapacity(), st);
            }

            /* Constraint 7 : All vSwitch Ports, must be from same physical switch-ports */

//            for (int i = 0; i < virtualTopo.getSwitches().size(); i++) {
//                String st = "SameSwitch-" + i;
//                GRBQuadExpr sameSwitch = new GRBQuadExpr();
//                ArrayList<VirtSwitchPort> virtSwitchPorts = virtualTopo.getSwitches().get(i).getSwitchPorts();
//                for (int port1 = 0; port1 < virtSwitchPorts.size(); port1++) {
//                    for (int port2 = 0; port2 < virtSwitchPorts.size(); port2++) {
//                        if (port1 == port2) continue;
//                        int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
//                        int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
//                        ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitchPorts();
//                        for (int pport1 = 0; pport1 < phySwitchPorts.size(); pport1++) {
//                            for (int pport2 = 0; pport2 < phySwitchPorts.size(); pport2++) {
//                                //int pport2 = pport1 + 1;
//                                if (pport2 >= phySwitchPorts.size())
//                                    break;
//                                if (pport2 == pport1) continue;
//                                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport1));
//                                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport2));
//                                //System.out.println("X ")
//                                if (isNotSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)) == 1.0) {
//                                    //System.out.println("Diff Switch : " + phySwitchPorts.get(pport1) + " and " + phySwitchPorts.get(pport2));
//                                }
//                                sameSwitch.addTerm(isNotSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)),
//                                        switchPortMapper[virtport1Index][phyport1Index],
//                                        switchPortMapper[virtport2Index][phyport2Index]);
//                            }
//                        }
//                    }
//                }
//                int switchPorts = virtSwitchPorts.size();
//                //System.out.println("Size of switch port = " + switchPorts);
//                int totalIter = switchPorts;
//                System.out.println("No. to total = " + totalIter);
//                model.addQConstr(sameSwitch, GRB.EQUAL, 0, st);
//            }

            /* Constraint 7 : Make sure physical switch TCAM capacity is not violated. */

            for (int i = 0; i < physicalTopo.getSwitches().size(); i++) {
                String st = "PhysicalSwitchTcam-" + i;
                GRBLinExpr switchTcam = new GRBLinExpr();
                //System.out.println("phys switch "+ physicalTopo.getSwitches().get(i).toString());
                ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitches().get(i).getSwitchPorts();
                for (int j = 0; j < phySwitchPorts.size(); j++) {
                    int phyPortIndex = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(j));
                    for (int virtPortIndex = 0; virtPortIndex < virtualTopo.getSwitchPorts().size(); virtPortIndex++) {
                        switchTcam.addTerm(virtualTopo.getSwitchPorts().get(virtPortIndex).getTCAM(), switchPortMapper[virtPortIndex][phyPortIndex]);
                    }
                }
                model.addConstr(switchTcam, GRB.LESS_EQUAL, physicalTopo.getSwitches().get(i).getTCAMCapacity(), st);
            }



            /* Set Objective */
            /* Objective : Use many physical switches as possible, to maximize the availability of TCAM */
//            GRBQuadExpr obj = new GRBQuadExpr();
//            for (int i=0;i< virtualTopo.getCoreLinks().size();i++) {
//                String st = "Objective";
//                VirtSwitchPort []virtSwitchPorts = virtualTopo.getCoreLinks().get(i).getEndPoints();
//                int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[0]);
//                int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[1]);
//                ArrayList<PhySwitchPort>  phySwitchPorts = physicalTopo.getCoreSwitchPorts();
//                System.out.println(phySwitchPorts.size());
//                for (int pport1 = 0; pport1< phySwitchPorts.size(); pport1++) {
//                    for (int pport2 = 0; pport2 < phySwitchPorts.size(); pport2++) {
//                        if (pport2 >= phySwitchPorts.size())
//                            break;
//
//                        if (pport1 == pport2) continue;
//                        int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport1));
//                        int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport2));
//                        //System.out.println("X ")
//                        if (isSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)) == 1.0) {
//                            continue;
//                        }
//                        obj.addTerm(1.0,
//                                switchPortMapper[virtport1Index][phyport1Index],
//                                switchPortMapper[virtport2Index][phyport2Index]);
//                        writer.println(virtport1Index+","+virtport2Index+","+phyport1Index+","+phyport2Index);
//
//                    }
//                }
//            }
//
//            model.setObjective(obj, GRB.MINIMIZE);

             /*
              * Objective : Use as less backbone links as possible :
              * 1) Inter-Switch
              * 2) Intra-Switch
              */
            GRBQuadExpr obj = new GRBQuadExpr();

            //  First Inter-Switch
            for (int i=0;i< physicalTopo.getBackboneLinks().size();i++) {
                PhyCoreLink backboneLink = physicalTopo.getBackboneLinks().get(i);
                PhySwitchPort []phySwitchPorts = backboneLink.getEndPoints();
                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[0]);
                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[1]);
                for (int j=0;j< virtualTopo.getCoreLinks().size();j++) {
                    VirtSwitchPort []virtSwitchPorts = virtualTopo.getCoreLinks().get(j).getEndPoints();
                    int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[0]);
                    int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[1]);
                    obj.addTerm(1.0,
                                switchPortMapper[virtport1Index][phyport1Index],
                                switchPortMapper[virtport2Index][phyport2Index]);
                    obj.addTerm(1.0,
                            switchPortMapper[virtport1Index][phyport2Index],
                            switchPortMapper[virtport2Index][phyport1Index]);
                }
            }

            // Second Intra-Switch
            for (int i=0;i< physicalTopo.getBackboneLinks().size();i++) {
                PhyCoreLink backboneLink = physicalTopo.getBackboneLinks().get(i);
                PhySwitchPort[] phySwitchPorts = backboneLink.getEndPoints();
                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[0]);
                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[1]);
                for (int j=0; j < virtualTopo.getSwitches().size();j++) {
                    ArrayList<VirtSwitchPort> virtSwitchPorts = virtualTopo.getSwitches().get(j).getSwitchPorts();
                    for (int port1 = 0; port1 < virtSwitchPorts.size(); port1++) {
                        for (int port2 = 0; port2 < virtSwitchPorts.size(); port2++) {
                            if (port1 == port2) continue;
                            int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
                            int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
                            obj.addTerm(1.0,
                                    switchPortMapper[virtport1Index][phyport1Index],
                                    switchPortMapper[virtport2Index][phyport2Index]);
                            obj.addTerm(1.0,
                                    switchPortMapper[virtport1Index][phyport2Index],
                                    switchPortMapper[virtport2Index][phyport1Index]);
                        }
                    }
                }
            }

            model.update();
            model.setObjective(obj, GRB.MINIMIZE);
            model.update();
            model.write("BNVMapper.lp");

            model.optimize();

            int status = model.get(GRB.IntAttr.Status);
            if (status == GRB.OPTIMAL) {
                System.out.println("LP RESULT : OPTIMAL");
                printAndStoreSolution(switchPortMapper, hostMapper);
                //model.dispose();
                env.dispose();


            } else if (status == GRB.INFEASIBLE) {
                System.out.println("LP RESULT : INFEASIBLE");
                model.dispose();
                env.dispose();
            } else if (status == GRB.UNBOUNDED) {
                System.out.println("LP RESULT : UN_BOUNDED");
                model.dispose();
                env.dispose();
            } else {
                model.dispose();
                env.dispose();

                System.out.println("LP Stopped with status = " + status);
            }
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return GRB.INFEASIBLE;
    }



    public int allocateSafe() {
        GRBEnv env;
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("ModelVirt.txt", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {

            env = new GRBEnv();
            //env.set(GRB.IntParam.PreQLinearize, 1);
            env.set(GRB.DoubleParam.TimeLimit, 200);
            GRBModel model = new GRBModel(env);

            switchPortMapper = new GRBVar[virtualTopo.getSwitchPorts().size()][physicalTopo.getSwitchPorts().size()];
            hostMapper = new GRBVar[virtualTopo.getHosts().size()][physicalTopo.getHosts().size()];

            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    String st = "X[" + i + "," + j + "]";
                    switchPortMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    String st = "Y[" + i + "," + j + "]";
                    hostMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }
            }
            model.update();

            /* Constraint 1: Each virtual switch-port should be placed on only one Physical switch-port. */
            GRBLinExpr[] switchPortPlacement = new GRBLinExpr[virtualTopo.getSwitchPorts().size()];
            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                String st = "SwitchPortPlacement-" + i;
                switchPortPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    switchPortPlacement[i].addTerm(1.0, switchPortMapper[i][j]);
                }
                model.addConstr(switchPortPlacement[i], GRB.EQUAL, 1, st);
            }


            /* Constraint 2: Each virtual switch-port should be placed on only one Physical switch-port. */
            GRBLinExpr[] hostPlacement = new GRBLinExpr[virtualTopo.getHosts().size()];
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                String st = "HostPlacement-" + i;
                hostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    hostPlacement[i].addTerm(1.0, hostMapper[i][j]);
                }
                model.addConstr(hostPlacement[i], GRB.EQUAL, 1, st);
            }

            /* Constraint 3: Each Physical switch-port should be used only once. */
//            GRBLinExpr[] physswitchPortPlacement = new GRBLinExpr[physicalTopo.getSwitchPorts().size()];
//            for (int i=0;i< physicalTopo.getSwitchPorts().size();i++) {
//                String st = "phySwitchPortPlacement-" + i;
//                physswitchPortPlacement[i] = new GRBLinExpr();
//                for (int j = 0; j < virtualTopo.getSwitchPorts().size(); j++) {
//                    physswitchPortPlacement[i].addTerm(1.0, switchPortMapper[j][i]);
//                }
//                model.addConstr(physswitchPortPlacement[i], GRB.LESS_EQUAL, 1, st);
//            }


            /* Constraint 3: Each Physical switch-host should be used as per its VM limits. */
            GRBLinExpr[] physhostPlacement = new GRBLinExpr[physicalTopo.getHosts().size()];
            for (int i = 0; i < physicalTopo.getHosts().size(); i++) {
                String st = "phyHostPlacement-" + i;
                PhyHost phyHost = physicalTopo.getHosts().get(i);
                physhostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < virtualTopo.getHosts().size(); j++) {
                    physhostPlacement[i].addTerm(1.0, hostMapper[j][i]);
                }
                model.addConstr(physhostPlacement[i], GRB.LESS_EQUAL, phyHost.getVMCap(), st);
            }



            /* Constraint 4: Each Virtual switch-port which would be connected to a host should have a direct-link */
            for (int i = 0; i < virtualTopo.getHostLinks().size(); i++) {
                String st = "PortHost-" + i;
                System.out.println("hllll "+virtualTopo.getHostLinks().get(i).toString());
                GRBQuadExpr hostLinkPlacement = new GRBQuadExpr();
                VirtHost myHost = virtualTopo.getHostLinks().get(i).getHostPort();
                VirtSwitchPort mySwitchPort = virtualTopo.getHostLinks().get(i).getSwitchPort();
                int switchPortIndex = virtualTopo.getSwitchPorts().indexOf(mySwitchPort);
                int hostIndex = virtualTopo.getHosts().indexOf(myHost);
                System.out.println(" Index of " + mySwitchPort.toString() + " is " + switchPortIndex);
                System.out.println(" Index of " + myHost.toString() + " is " + hostIndex);
                for (int physPortIndex = 0; physPortIndex < physicalTopo.getSwitchPorts().size(); physPortIndex++) {
                    for (int physhostIndex = 0; physhostIndex < physicalTopo.getHosts().size(); physhostIndex++) {
                        hostLinkPlacement.addTerm(isDirectlyConnected(physicalTopo.getSwitchPorts().get(physPortIndex),
                                physicalTopo.getHosts().get(physhostIndex)),
                                switchPortMapper[switchPortIndex][physPortIndex],
                                hostMapper[hostIndex][physhostIndex]);
                    }
                }
                System.out.println("hostlinkplacement");
                model.addQConstr(hostLinkPlacement, GRB.EQUAL, 1.0, st);
            }

            /* Constraint 5 : Satisfy virtual link mapping to physical link constraint */
            for (int i = 0; i < virtualTopo.getCoreLinks().size(); i++) {
                String st = "PortPort-" + i;
                GRBQuadExpr coreLinkPlacement = new GRBQuadExpr();
                VirtSwitchPort[] virtendPoints = virtualTopo.getCoreLinks().get(i).getEndPoints();
                int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                for (int j = 0; j < physicalTopo.getCoreLinks().size(); j++) {
                    if (!physicalTopo.getCoreLinks().get(j).isEnabled()) {
                        continue;
                    }
                    PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(j).getEndPoints();
                    int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                    int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);
                    coreLinkPlacement.addTerm(1.0,
                            switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                    coreLinkPlacement.addTerm(1.0,
                            switchPortMapper[virtendPoint1Index][endPoint2Index],
                            switchPortMapper[virtendPoint2Index][endPoint1Index]);
                }
                model.addQConstr(coreLinkPlacement, GRB.EQUAL, 1.0, st);
            }


            /* Constraint 6 : Link Bandwidth constraints */

            for (int i = 0; i < physicalTopo.getCoreLinks().size(); i++) {
                if (!physicalTopo.getCoreLinks().get(i).isEnabled()) {
                    continue;
                }
                String st = "CoreLink-" + i;
                GRBQuadExpr coreLinkBandwidth = new GRBQuadExpr();
                PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(i).getEndPoints();
                int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);

                /* Link B/W for core-links */
                for (int j = 0; j < virtualTopo.getCoreLinks().size(); j++) {
                    VirtSwitchPort[] virtendPoints = virtualTopo.getCoreLinks().get(j).getEndPoints();
                    int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                    int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                    coreLinkBandwidth.addTerm(virtualTopo.getCoreLinks().get(j).getBandwidth(),
                            switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                }
                /* Link B/w for intra-switch links */

                for (int j=0;j < virtualTopo.getSwitches().size();j++) {
                    ArrayList<VirtSwitchPort> virtSwitchPorts = virtualTopo.getSwitches().get(j).getSwitchPorts();
                    for (int port1 = 0; port1 < virtSwitchPorts.size(); port1++) {
                        for (int port2 = 0; port2 < virtSwitchPorts.size(); port2++) {
                            if (port1 == port2) continue;
                            int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
                            int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
                            coreLinkBandwidth.addTerm(1,
                                    switchPortMapper[virtport1Index][endPoint1Index],
                                    switchPortMapper[virtport2Index][endPoint2Index]);
                        }
                    }
                }


                model.addQConstr(coreLinkBandwidth, GRB.LESS_EQUAL, physicalTopo.getCoreLinks().get(i).getCapacity(), st);
            }

            /* Constraint 7 : All vSwitch Ports, must be from same physical switch-ports */

            for (int i = 0; i < virtualTopo.getSwitches().size(); i++) {
                String st = "SameSwitch-" + i;
                GRBQuadExpr sameSwitch = new GRBQuadExpr();
                ArrayList<VirtSwitchPort> virtSwitchPorts = virtualTopo.getSwitches().get(i).getSwitchPorts();
                for (int port1 = 0; port1 < virtSwitchPorts.size(); port1++) {
                    for (int port2 = 0; port2 < virtSwitchPorts.size(); port2++) {
                        if (port1 == port2) continue;
                        int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
                        int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
                        ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitchPorts();
                        for (int pport1 = 0; pport1 < phySwitchPorts.size(); pport1++) {
                            for (int pport2 = 0; pport2 < phySwitchPorts.size(); pport2++) {
                                //int pport2 = pport1 + 1;
                                if (pport2 >= phySwitchPorts.size())
                                    break;
                                if (pport2 == pport1) continue;
                                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport1));
                                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport2));
                                //System.out.println("X ")
                                if (isNotSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)) == 1.0) {
                                    //System.out.println("Diff Switch : " + phySwitchPorts.get(pport1) + " and " + phySwitchPorts.get(pport2));
                                }
                                sameSwitch.addTerm(isNotSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)),
                                        switchPortMapper[virtport1Index][phyport1Index],
                                        switchPortMapper[virtport2Index][phyport2Index]);
                            }
                        }
                    }
                }
                int switchPorts = virtSwitchPorts.size();
                //System.out.println("Size of switch port = " + switchPorts);
                int totalIter = switchPorts;
                System.out.println("No. to total = " + totalIter);
                model.addQConstr(sameSwitch, GRB.EQUAL, 0, st);
            }

            /* Constraint 7 : Make sure physical switch TCAM capacity is not violated. */

            for (int i = 0; i < physicalTopo.getSwitches().size(); i++) {
                String st = "PhysicalSwitchTcam-" + i;
                GRBLinExpr switchTcam = new GRBLinExpr();
                //System.out.println("phys switch "+ physicalTopo.getSwitches().get(i).toString());
                ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitches().get(i).getSwitchPorts();
                for (int j = 0; j < phySwitchPorts.size(); j++) {
                    int phyPortIndex = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(j));
                    for (int virtPortIndex = 0; virtPortIndex < virtualTopo.getSwitchPorts().size(); virtPortIndex++) {
                        switchTcam.addTerm(virtualTopo.getSwitchPorts().get(virtPortIndex).getTCAM(), switchPortMapper[virtPortIndex][phyPortIndex]);
                    }
                }
                model.addConstr(switchTcam, GRB.LESS_EQUAL, physicalTopo.getSwitches().get(i).getTCAMCapacity(), st);
            }



            /* Set Objective */
            /* Objective : Use many physical switches as possible, to maximize the availability of TCAM */
//            GRBQuadExpr obj = new GRBQuadExpr();
//            for (int i=0;i< virtualTopo.getCoreLinks().size();i++) {
//                String st = "Objective";
//                VirtSwitchPort []virtSwitchPorts = virtualTopo.getCoreLinks().get(i).getEndPoints();
//                int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[0]);
//                int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[1]);
//                ArrayList<PhySwitchPort>  phySwitchPorts = physicalTopo.getCoreSwitchPorts();
//                System.out.println(phySwitchPorts.size());
//                for (int pport1 = 0; pport1< phySwitchPorts.size(); pport1++) {
//                    for (int pport2 = 0; pport2 < phySwitchPorts.size(); pport2++) {
//                        if (pport2 >= phySwitchPorts.size())
//                            break;
//
//                        if (pport1 == pport2) continue;
//                        int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport1));
//                        int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport2));
//                        //System.out.println("X ")
//                        if (isSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)) == 1.0) {
//                            continue;
//                        }
//                        obj.addTerm(1.0,
//                                switchPortMapper[virtport1Index][phyport1Index],
//                                switchPortMapper[virtport2Index][phyport2Index]);
//                        writer.println(virtport1Index+","+virtport2Index+","+phyport1Index+","+phyport2Index);
//
//                    }
//                }
//            }
//
//            model.setObjective(obj, GRB.MINIMIZE);

             /*
              * Objective : Use as less backbone links as possible :
              * 1) Inter-Switch
              * 2) Intra-Switch
              */
            GRBQuadExpr obj = new GRBQuadExpr();

            //  First Inter-Switch
            for (int i=0;i< physicalTopo.getBackboneLinks().size();i++) {
                PhyCoreLink backboneLink = physicalTopo.getBackboneLinks().get(i);
                PhySwitchPort []phySwitchPorts = backboneLink.getEndPoints();
                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[0]);
                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts[1]);
                for (int j=0;j< virtualTopo.getCoreLinks().size();j++) {
                    VirtSwitchPort []virtSwitchPorts = virtualTopo.getCoreLinks().get(j).getEndPoints();
                    int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[0]);
                    int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts[1]);
                    obj.addTerm(1.0,
                            switchPortMapper[virtport1Index][phyport1Index],
                            switchPortMapper[virtport2Index][phyport2Index]);
                    obj.addTerm(1.0,
                            switchPortMapper[virtport1Index][phyport2Index],
                            switchPortMapper[virtport2Index][phyport1Index]);
                }
            }


            model.update();
            model.setObjective(obj, GRB.MINIMIZE);
            model.update();
            model.write("BNVMapper.lp");

            model.optimize();

            int status = model.get(GRB.IntAttr.Status);
            if (status == GRB.OPTIMAL) {
                System.out.println("LP RESULT : OPTIMAL");
                printAndStoreSolution(switchPortMapper, hostMapper);
                //model.dispose();
                env.dispose();


            } else if (status == GRB.INFEASIBLE) {
                System.out.println("LP RESULT : INFEASIBLE");
                model.dispose();
                env.dispose();
            } else if (status == GRB.UNBOUNDED) {
                System.out.println("LP RESULT : UN_BOUNDED");
                model.dispose();
                env.dispose();
            } else {
                model.dispose();
                env.dispose();

                System.out.println("LP Stopped with status = " + status);
            }
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return GRB.INFEASIBLE;
    }

    private double isSameSwitch(PhySwitchPort phyport1, PhySwitchPort phyport2) {
        if (phyport1.getParentSwitch().equals(phyport2.getParentSwitch())) {
            return 1.0;
        }
        return 0.0;
    }

    private double isNotSameSwitch(PhySwitchPort phyport1, PhySwitchPort phyport2) {
        if (phyport1.getParentSwitch().equals(phyport2.getParentSwitch())) {
            return 0.0;
        }
        return 1.0;
    }

    private Double isDirectlyConnected(PhySwitchPort phySwitchPort, PhyHost phyHost) {
        PhyHostLink phySLink = new PhyHostLink("someLink", phySwitchPort, phyHost);

        if (physicalTopo.getHostLinks().contains(phySLink)) {
            System.out.println(phySwitchPort.toString() + " and "+ phyHost.toString() +" are connected directly");
            return 1.0;
        }
        return 0.0;
    }


    private void printAndStoreSolution(GRBVar[][] switchPortMapper, GRBVar[][] hostMapper) throws Exception {
        if (Global.environment.equals("ncl")) {
            ArrayList<PhyHost> reservedNodes = new ArrayList<>();
            PhyHost ofctrl = null;
            System.out.println("Nodes:");
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    if (hostMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                        hostMapping.put(virtualTopo.getHosts().get(i), physicalTopo.getHosts().get(j));
                        System.out.println(virtualTopo.getHosts().get(i).getID()+ " " + physicalTopo.getHosts().get(j).getID());
                        reservedNodes.add(physicalTopo.getHosts().get(j));
                    }
                }
            }
            /* Now a bruteforce allocation of ofctrl and ovx */
            for (int i=0;i<physicalTopo.getHosts().size();i++) {
                PhyHost myHost = physicalTopo.getHosts().get(i);
                if (!reservedNodes.contains(myHost)) {
                    System.out.println("ofctrl " + physicalTopo.getHosts().get(i).getID());
                    ofctrl = physicalTopo.getHosts().get(i);
                    break;
                }
            }
            System.out.println("ovx ovx");
            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    if (switchPortMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                        switchPortMapping.put(virtualTopo.getSwitchPorts().get(i), physicalTopo.getSwitchPorts().get(j));
                        System.out.println(virtualTopo.getSwitchPorts().get(i).getID() + " " + physicalTopo.getSwitchPorts().get(j).getID());
                    }
                }
            }

            System.out.println("End Nodes");
            System.out.println("Edges:");
            for (int i=0;i<virtualTopo.getHostLinks().size();i++) {
                VirtHostLink vhl = virtualTopo.getHostLinks().get(i);
                PhyHost ph = null;
                ph = hostMapping.get(vhl.getHostPort());
                PhySwitchPort psp  = null;
                psp = switchPortMapping.get(vhl.getSwitchPort());
                System.out.println("linksimple/"+vhl.getID()+"/"+vhl.getHostPort().getID()+":0,"+vhl.getSwitchPort().getID()+":1 direct link-"+ph.getID()+":eth3-"+psp.getID()+":eth0 ("+ph.getID()+"/eth3,eth0)" + " link-"+ph.getID()+":eth3-"+psp.getID()+":eth0 ("+ph.getID()+"/eth3,eth0)");
            }
            for (int i=0;i<virtualTopo.getCoreLinks().size();i++) {
                VirtCoreLink vcl = virtualTopo.getCoreLinks().get(i);
                PhySwitchPort psp1 = null;
                psp1 = switchPortMapping.get(vcl.getEndPoints()[0]);
                PhySwitchPort psp2 = null;
                psp2 = switchPortMapping.get(vcl.getEndPoints()[1]);
                System.out.println("linksimple/"+vcl.getID()+"/"+vcl.getEndPoints()[0].getID()+":1,"+vcl.getEndPoints()[1].getID()+":1 direct link-"+psp1.getID()+":eth0-"+psp2.getID()+":eth0 ("+psp1.getID()+"/eth0,eth0)" + " link-"+psp1.getID()+":eth0-"+psp2.getID()+":eth0 ("+psp1.getID()+"/eth0,eth0)");

            }
            String exptswitch = switchCon.get(ofctrl.getID().substring(ofctrl.getID().length()-1));

            System.out.println("linksimple/ofc-ovxlink/ofctrl:0,ovx:0 intraswitch link-"+ofctrl.getID()+":eth1-"+exptswitch+":(null) ("+ofctrl.getID()+"/eth0,(null)) link-HPCore1:"+exptswitch+" (HPCore1/(null),(null)) link-ovx:eth0-HPCore1:Bridge-Aggregation1 (ovx/eth0,Bridge-Aggregation1))");


            for (int i=0;i<virtualTopo.getSwitches().size();i++) {
                VirtSwitch vs = virtualTopo.getSwitches().get(i);
                ArrayList<VirtSwitchPort> vswitchPorts = vs.getSwitchPorts();

                String linkType = "linksimple";
                if (vswitchPorts.size() > 2) {
                    linkType = "linklan";
                    for (int j=0;j<vswitchPorts.size();j++) {
                        String sdnswitch = switchCon.get(ofctrl.getID().substring(vswitchPorts.get(j).getID().length()-1));

                        System.out.print(linkType+"/"+vs.getID()+"/");
                        System.out.print(vswitchPorts.get(j).getID()+":0");
                        System.out.print(" direct ");
                        System.out.print("link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+sdnswitch+"(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null)) link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+ sdnswitch+ ":(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null))" );
                        System.out.println();
                    }
                    continue;
                }
                System.out.print(linkType+"/"+vs.getID()+"/");
                for (int j=0;j<vswitchPorts.size();j++) {
                    String sdnswitch = switchCon.get(ofctrl.getID().substring(vswitchPorts.get(j).getID().length()-1));
                    if (j!=0) { System.out.print(","); }
                    System.out.print(vswitchPorts.get(j).getID()+":0");
                }
                System.out.print(" intraswitch ");

                for (int j=0;j<vswitchPorts.size();j++) {
                    String sdnswitch = switchCon.get(ofctrl.getID().substring(vswitchPorts.get(j).getID().length()-1));
                    System.out.print("link-"+switchPortMapping.get(vswitchPorts.get(j)).getID()+":eth1-"+ sdnswitch + ":(null) ("+switchPortMapping.get(vswitchPorts.get(j)).getID()+"/eth1,null)) ");
                }
                System.out.println();
            }
            System.out.println("End Edges");
            System.out.println("End solution");
        } else {
            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    if (switchPortMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                        System.out.println(virtualTopo.getSwitchPorts().get(i).toString() + " Mapped to " + physicalTopo.getSwitchPorts().get(j).toString());
                    }
                }
            }
            for (int i = 0; i < virtualTopo.getHosts().size(); i++) {
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    if (hostMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                        System.out.println(virtualTopo.getHosts().get(i).toString() + " Mapped to " + physicalTopo.getHosts().get(j).toString());
                    }
                }
            }
        }
    }

    public int updateMapping(VirtTopo updVirtualTopo) throws Exception {


        /* Store the Old Mapping values */
        switchPortMapperOld = new Double[virtualTopo.getSwitchPorts().size()][physicalTopo.getSwitchPorts().size()];
        hostMapperOld = new Double[virtualTopo.getHosts().size()][physicalTopo.getHosts().size()];
        printAndStoreSolution(switchPortMapper, hostMapper);
        for (int i=0; i< switchPortMapper.length;i++) {
            for (int j=0;j<switchPortMapper[i].length;j++) {
                if (switchPortMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                    switchPortMapperOld[i][j] = 1.0;
                } else {
                    switchPortMapperOld[i][j] = 0.0;
                }
                //switchPortMapperOld[i][j] = switchPortMapper[i][j].get(GRB.DoubleAttr.X);
            }
        }

        for (int i=0; i< hostMapper.length;i++) {
            for (int j=0;j<hostMapper[i].length;j++) {
                hostMapperOld[i][j] = hostMapper[i][j].get(GRB.DoubleAttr.X);
            }
        }

        GRBEnv env = new GRBEnv();
        //env.set(GRB.IntParam.PreQLinearize, 1);
        env.set(GRB.DoubleParam.TimeLimit, 200);
        GRBModel model = new GRBModel(env);
        virtualTopo = updVirtualTopo;
        switchPortMapper = new GRBVar[updVirtualTopo.getSwitchPorts().size()][physicalTopo.getSwitchPorts().size()];
        hostMapper = new GRBVar[updVirtualTopo.getHosts().size()][physicalTopo.getHosts().size()];
        for (int i = 0; i < updVirtualTopo.getSwitchPorts().size(); i++) {
            for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                String st = "X[" + i + "," + j + "]";
                switchPortMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
            }
        }
        for (int i = 0; i < updVirtualTopo.getHosts().size(); i++) {
            for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                String st = "Y[" + i + "," + j + "]";
                hostMapper[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
            }
        }
        model.update();

        /* Constraint 1: Each virtual switch-port should be placed on only one Physical switch-port. */
        GRBLinExpr[] switchPortPlacement = new GRBLinExpr[updVirtualTopo.getSwitchPorts().size()];
        for (int i = 0; i < updVirtualTopo.getSwitchPorts().size(); i++) {
            String st = "SwitchPortPlacement-" + i;
            switchPortPlacement[i] = new GRBLinExpr();
            for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                switchPortPlacement[i].addTerm(1.0, switchPortMapper[i][j]);
            }
            model.addConstr(switchPortPlacement[i], GRB.EQUAL, 1, st);
        }


            /* Constraint 2: Each virtual switch-port should be placed on only one Physical switch-port. */
        GRBLinExpr[] hostPlacement = new GRBLinExpr[updVirtualTopo.getHosts().size()];
        for (int i = 0; i < updVirtualTopo.getHosts().size(); i++) {
            String st = "HostPlacement-" + i;
            hostPlacement[i] = new GRBLinExpr();
            for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                hostPlacement[i].addTerm(1.0, hostMapper[i][j]);
            }
            model.addConstr(hostPlacement[i], GRB.EQUAL, 1, st);
        }


            /* Constraint 3: Each Physical switch-host should be used as per its VM limits. */
        GRBLinExpr[] physhostPlacement = new GRBLinExpr[physicalTopo.getHosts().size()];
        for (int i = 0; i < physicalTopo.getHosts().size(); i++) {
            String st = "phyHostPlacement-" + i;
            PhyHost phyHost = physicalTopo.getHosts().get(i);
            physhostPlacement[i] = new GRBLinExpr();
            for (int j = 0; j < updVirtualTopo.getHosts().size(); j++) {
                physhostPlacement[i].addTerm(1.0, hostMapper[j][i]);
            }
            model.addConstr(physhostPlacement[i], GRB.LESS_EQUAL, phyHost.getVMCap(), st);
        }



            /* Constraint 4: Each Virtual switch-port which would be connected to a host should have a direct-link */
        for (int i = 0; i < updVirtualTopo.getHostLinks().size(); i++) {
            String st = "PortHost-" + i;
            //System.out.println(virtualTopo.getHostLinks().get(i).toString());
            GRBQuadExpr hostLinkPlacement = new GRBQuadExpr();
            VirtHost myHost = updVirtualTopo.getHostLinks().get(i).getHostPort();
            VirtSwitchPort mySwitchPort = updVirtualTopo.getHostLinks().get(i).getSwitchPort();
            int switchPortIndex = updVirtualTopo.getSwitchPorts().indexOf(mySwitchPort);
            int hostIndex = updVirtualTopo.getHosts().indexOf(myHost);
            //.out.println(" Index of " + mySwitchPort.toString() + " is " + switchPortIndex);
            //System.out.println(" Index of " + myHost.toString() + " is " + hostIndex);
            for (int physPortIndex = 0; physPortIndex < physicalTopo.getSwitchPorts().size(); physPortIndex++) {
                for (int physhostIndex = 0; physhostIndex < physicalTopo.getHosts().size(); physhostIndex++) {
                    hostLinkPlacement.addTerm(isDirectlyConnected(physicalTopo.getSwitchPorts().get(physPortIndex),
                            physicalTopo.getHosts().get(physhostIndex)),
                            switchPortMapper[switchPortIndex][physhostIndex],
                            hostMapper[hostIndex][physhostIndex]);
                }
            }
            model.addQConstr(hostLinkPlacement, GRB.EQUAL, 1.0, st);
        }

            /* Constraint 5 : Satisfy virtual link mapping to physical link constraint */
        for (int i = 0; i < updVirtualTopo.getCoreLinks().size(); i++) {
            String st = "PortPort-" + i;
            GRBQuadExpr coreLinkPlacement = new GRBQuadExpr();
            VirtSwitchPort[] virtendPoints = updVirtualTopo.getCoreLinks().get(i).getEndPoints();
            int virtendPoint1Index = updVirtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
            int virtendPoint2Index = updVirtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
            for (int j = 0; j < physicalTopo.getCoreLinks().size(); j++) {
                if (!physicalTopo.getCoreLinks().get(j).isEnabled()) {
                    continue;
                }
                PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(j).getEndPoints();
                int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);
                coreLinkPlacement.addTerm(1.0,
                        switchPortMapper[virtendPoint1Index][endPoint1Index],
                        switchPortMapper[virtendPoint2Index][endPoint2Index]);
            }
            model.addQConstr(coreLinkPlacement, GRB.EQUAL, 1.0, st);
        }


            /* Constraint 6 : Link Bandwidth constraints */

        for (int i = 0; i < physicalTopo.getCoreLinks().size(); i++) {
            if (!physicalTopo.getCoreLinks().get(i).isEnabled()) {
                continue;
            }
            String st = "CoreLink-" + i;
            GRBQuadExpr coreLinkBandwidth = new GRBQuadExpr();
            PhySwitchPort[] endPoints = physicalTopo.getCoreLinks().get(i).getEndPoints();
            int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
            int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);

            for (int j = 0; j < updVirtualTopo.getCoreLinks().size(); j++) {
                VirtSwitchPort[] virtendPoints = updVirtualTopo.getCoreLinks().get(j).getEndPoints();
                int virtendPoint1Index = updVirtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                int virtendPoint2Index = updVirtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                coreLinkBandwidth.addTerm(updVirtualTopo.getCoreLinks().get(j).getBandwidth(),
                        switchPortMapper[virtendPoint1Index][endPoint1Index],
                        switchPortMapper[virtendPoint2Index][endPoint2Index]);
            }
            model.addQConstr(coreLinkBandwidth, GRB.LESS_EQUAL, physicalTopo.getCoreLinks().get(i).getCapacity(), st);
        }

            /* Constraint 7 : Make sure physical switch TCAM capacity is not violated. */

        for (int i = 0; i < physicalTopo.getSwitches().size(); i++) {
            String st = "PhysicalSwitchTcam-" + i;
            GRBLinExpr switchTcam = new GRBLinExpr();
            //System.out.println("phys switch "+ physicalTopo.getSwitches().get(i).toString());
            ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitches().get(i).getSwitchPorts();
            for (int j = 0; j < phySwitchPorts.size(); j++) {
                int phyPortIndex = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(j));
                for (int virtPortIndex = 0; virtPortIndex < updVirtualTopo.getSwitchPorts().size(); virtPortIndex++) {
                    switchTcam.addTerm(updVirtualTopo.getSwitchPorts().get(virtPortIndex).getTCAM(), switchPortMapper[virtPortIndex][phyPortIndex]);
                }
            }
            model.addConstr(switchTcam, GRB.LESS_EQUAL, physicalTopo.getSwitches().get(i).getTCAMCapacity(), st);
        }



        /* Set Objective */
        /* Objective : Try to maximize the similarity in mapping b/w old one and updated virt topo */
        GRBLinExpr obj = new GRBLinExpr();

        for (int i=0;i< updVirtualTopo.getSwitchPorts().size();i++ ) {
            int oldIndex = updVirtualTopo.indexOf(updVirtualTopo.getSwitchPorts().get(i));
            if (oldIndex == -1) {
                /* The switch-port is newly defined in the updated topology.
                 * Hence, no objective for this switch-port
                 */
                continue;
            }
            System.out.println("OldIndex = of " + updVirtualTopo.getSwitchPorts().get(i)+"= " + oldIndex);
            for (int j=0;j< physicalTopo.getSwitchPorts().size();j++) {
                String st  = "Objective";
                obj.addTerm(switchPortMapperOld[oldIndex][j], switchPortMapper[i][j]);
            }
        }
        model.setObjective(obj, GRB.MAXIMIZE);



        model.update();


        model.write("Core.Mapper.lp");

        model.optimize();

        int status = model.get(GRB.IntAttr.Status);
        if (status == GRB.OPTIMAL) {
            System.out.println("LP RESULT : OPTIMAL");
            printAndStoreSolution(switchPortMapper, hostMapper);
            model.dispose();
            env.dispose();


        } else if (status == GRB.INFEASIBLE) {
            System.out.println("LP RESULT : INFEASIBLE");
            model.dispose();
            env.dispose();
        } else if (status == GRB.UNBOUNDED) {
            System.out.println("LP RESULT : UN_BOUNDED");
            model.dispose();
            env.dispose();
        } else {
            model.dispose();
            env.dispose();

            System.out.println("LP Stopped with status = " + status);
        }
        return status;
    }


}