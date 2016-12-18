import PhysicalTopo.PhyHost;
import PhysicalTopo.PhyHostLink;
import PhysicalTopo.PhySwitchPort;
import PhysicalTopo.PhyTopo;
import VirtualTopo.VirtHost;
import VirtualTopo.VirtHostLink;
import VirtualTopo.VirtSwitchPort;
import VirtualTopo.VirtTopo;
import gurobi.*;
import sun.plugin.javascript.navig.Array;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by pravein on 16/12/16.
 */
public class Mapper {
    VirtTopo virtualTopo;
    PhyTopo physicalTopo;

    private GRBVar[][] switchPortMapper;
    private GRBVar[][] hostMapper;
    public Mapper(VirtTopo virtualTopo, PhyTopo physicalTopo) {
        this.virtualTopo = virtualTopo;
        this.physicalTopo = physicalTopo;
    }

    public void allocate () {
        GRBEnv env;
        try {

            env = new GRBEnv();
            //env.set(GRB.IntParam.PreQLinearize, 1);
            //env.set(GRB.DoubleParam.TimeLimit, 200);
            GRBModel model = new GRBModel(env);

            switchPortMapper = new GRBVar[virtualTopo.getSwitchPorts().size()][physicalTopo.getSwitchPorts().size()];
            hostMapper = new GRBVar[virtualTopo.getHosts().size()][physicalTopo.getHosts().size()];

            for (int i = 0; i < virtualTopo.getSwitchPorts().size(); i++) {
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    String st = "X[" + i + "," + j +"]";
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
            for (int i=0;i< virtualTopo.getSwitchPorts().size();i++) {
                String st = "SwitchPortPlacement-" + i;
                switchPortPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getSwitchPorts().size(); j++) {
                    switchPortPlacement[i].addTerm(1.0, switchPortMapper[i][j]);
                }
                model.addConstr(switchPortPlacement[i], GRB.EQUAL, 1, st);
            }


            /* Constraint 2: Each virtual switch-port should be placed on only one Physical switch-port. */
            GRBLinExpr[] hostPlacement = new GRBLinExpr[virtualTopo.getHosts().size()];
            for (int i=0;i< virtualTopo.getHosts().size();i++) {
                String st = "HostPlacement-" + i;
                hostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < physicalTopo.getHosts().size(); j++) {
                    hostPlacement[i].addTerm(1.0, hostMapper[i][j]);
                }
                model.addConstr(hostPlacement[i], GRB.EQUAL, 1, st);
            }

            /* Constraint 3: Each Physical switch-port should be used only once. */
            GRBLinExpr[] physswitchPortPlacement = new GRBLinExpr[physicalTopo.getSwitchPorts().size()];
            for (int i=0;i< physicalTopo.getSwitchPorts().size();i++) {
                String st = "phySwitchPortPlacement-" + i;
                physswitchPortPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < virtualTopo.getSwitchPorts().size(); j++) {
                    physswitchPortPlacement[i].addTerm(1.0, switchPortMapper[j][i]);
                }
                model.addConstr(physswitchPortPlacement[i], GRB.LESS_EQUAL, 1, st);
            }


            /* Constraint 3: Each Physical switch-port should be used only once. */
            GRBLinExpr[] physhostPlacement = new GRBLinExpr[physicalTopo.getHosts().size()];
            for (int i=0;i< physicalTopo.getHosts().size();i++) {
                String st = "phyHostPlacement-" + i;
                physhostPlacement[i] = new GRBLinExpr();
                for (int j = 0; j < virtualTopo.getHosts().size(); j++) {
                    physhostPlacement[i].addTerm(1.0, hostMapper[j][i]);
                }
                model.addConstr(physhostPlacement[i], GRB.LESS_EQUAL, 1, st);
            }



            /* Constraint 4: Each Virtual switch-port which would be connected to a host should have a direct-link */
            for (int i=0;i< virtualTopo.getHostLinks().size();i++) {
                String st = "PortHost-"+i;
                GRBQuadExpr hostLinkPlacement = new GRBQuadExpr();
                VirtHost myHost = virtualTopo.getHostLinks().get(i).getHostPort();
                VirtSwitchPort mySwitchPort = virtualTopo.getHostLinks().get(i).getSwitchPort();
                int switchPortIndex = virtualTopo.getSwitchPorts().indexOf(mySwitchPort);
                int hostIndex = virtualTopo.getHosts().indexOf(myHost);
                System.out.println(" Index of " + mySwitchPort.toString() + " is "+switchPortIndex);
                System.out.println(" Index of " + myHost.toString() + " is "+hostIndex);
                for (int physPortIndex = 0; physPortIndex < physicalTopo.getSwitchPorts().size() ; physPortIndex++) {
                    for (int physhostIndex = 0; physhostIndex < physicalTopo.getHosts().size(); physhostIndex++) {
                        hostLinkPlacement.addTerm(isDirectlyConnected(physicalTopo.getSwitchPorts().get(physPortIndex),
                                physicalTopo.getHosts().get(physhostIndex)),
                                switchPortMapper[switchPortIndex][physhostIndex],
                                hostMapper[hostIndex][physhostIndex]);
                    }
                }
                model.addQConstr(hostLinkPlacement, GRB.EQUAL, 1.0, st);
            }

            /* Constraint 6 : Satisfy virtual link mapping to physical link constraint */
            for (int i=0;i< virtualTopo.getCoreLinks().size();i++) {
                String st = "PortPort-"+i;
                GRBQuadExpr coreLinkPlacement = new GRBQuadExpr();
                VirtSwitchPort [] virtendPoints = virtualTopo.getCoreLinks().get(i).getEndPoints();
                int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                for (int j=0; j < virtualTopo.getCoreLinks().size();j++) {
                    PhySwitchPort []endPoints = physicalTopo.getCoreLinks().get(j).getEndPoints();
                    int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                    int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);
                    coreLinkPlacement.addTerm(1.0,
                                switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                }
                model.addQConstr(coreLinkPlacement, GRB.EQUAL, 1.0, st);
            }


            /* Constraint 5 : Link Bandwidth constraints */

            for (int i=0;i < physicalTopo.getCoreLinks().size();i++) {
                String st = "CoreLink-"+i;
                GRBQuadExpr coreLinkBandwidth = new GRBQuadExpr();
                PhySwitchPort []endPoints = physicalTopo.getCoreLinks().get(i).getEndPoints();
                int endPoint1Index = physicalTopo.getSwitchPorts().indexOf(endPoints[0]);
                int endPoint2Index = physicalTopo.getSwitchPorts().indexOf(endPoints[1]);

                for (int j=0; j < virtualTopo.getCoreLinks().size();j++) {
                    VirtSwitchPort [] virtendPoints = virtualTopo.getCoreLinks().get(j).getEndPoints();
                    int virtendPoint1Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[0]);
                    int virtendPoint2Index = virtualTopo.getSwitchPorts().indexOf(virtendPoints[1]);
                    coreLinkBandwidth.addTerm(virtualTopo.getCoreLinks().get(j).getBandwidth(),
                            switchPortMapper[virtendPoint1Index][endPoint1Index],
                            switchPortMapper[virtendPoint2Index][endPoint2Index]);
                }
                model.addQConstr(coreLinkBandwidth, GRB.LESS_EQUAL, physicalTopo.getCoreLinks().get(i).getCapacity(), st);
            }

            /* Constraint 6 : All vSwitch Ports, must be from same physical switch-ports */

            for (int i=0;i<virtualTopo.getSwitches().size();i++) {
                String st = "SameSwitch-"+i;
                GRBQuadExpr sameSwitch = new GRBQuadExpr();
                ArrayList<VirtSwitchPort>  virtSwitchPorts = virtualTopo.getSwitches().get(i).getSwitchPorts();
                for (int port1 = 0; port1< virtSwitchPorts.size(); port1++) {
                    for (int port2=0; port2< virtSwitchPorts.size();port2++) {
                        if (port1 == port2) continue;
                        int virtport1Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port1));
                        int virtport2Index = virtualTopo.getSwitchPorts().indexOf(virtSwitchPorts.get(port2));
                        ArrayList<PhySwitchPort>  phySwitchPorts = physicalTopo.getSwitchPorts();
                        for (int pport1 = 0; pport1< phySwitchPorts.size(); pport1++) {
                            for (int pport2 = 0; pport2 < phySwitchPorts.size(); pport2++) {
                                if (pport1 == pport2) continue;
                                int phyport1Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport1));
                                int phyport2Index = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(pport2));
                                //System.out.println("X ")
                                sameSwitch.addTerm(isSameSwitch(phySwitchPorts.get(pport1), phySwitchPorts.get(pport2)),
                                        switchPortMapper[virtport1Index][phyport1Index],
                                        switchPortMapper[virtport2Index][phyport2Index]);
                            }
                        }
                    }
                }
                int switchPorts = virtSwitchPorts.size();
                System.out.println("Size of switch port = "+ switchPorts);
                int totalIter = (switchPorts * (switchPorts -1));
                System.out.println("No. to total = "+ totalIter);
                model.addQConstr(sameSwitch, GRB.EQUAL, totalIter, st);
            }

            /* Constraint 7 : Make sure physical switch TCAM capacity is not violated. */

            for (int i=0;i < physicalTopo.getSwitches().size();i++) {
                String st = "PhysicalSwitchTcam-"+i;
                GRBLinExpr switchTcam = new GRBLinExpr();
                ArrayList<PhySwitchPort> phySwitchPorts = physicalTopo.getSwitches().get(i).getSwitchPorts();
                for (int j=0;j< phySwitchPorts.size();j++) {
                    int phyPortIndex = physicalTopo.getSwitchPorts().indexOf(phySwitchPorts.get(j));

                    for (int virtPortIndex =0; virtPortIndex < virtualTopo.getSwitchPorts().size(); virtPortIndex++) {
                        switchTcam.addTerm(virtualTopo.getSwitchPorts().get(virtPortIndex).getTCAM(), switchPortMapper[virtPortIndex][phyPortIndex]);
                    }
                }
                model.addConstr(switchTcam, GRB.LESS_EQUAL, physicalTopo.getSwitches().get(i).getTCAMCapacity());
            }

            model.update();
            /* Set Objective */

            model.write("Mapper.lp");

            model.optimize();

            int status = model.get(GRB.IntAttr.Status);
            if (status == GRB.OPTIMAL) {
                System.out.println("LP RESULT : OPTIMAL");
                printAndStoreSolution(switchPortMapper,hostMapper);
                model.dispose();
                env.dispose();

            }
            else if (status == GRB.INFEASIBLE)  {
                System.out.println("LP RESULT : INFEASIBLE");
                model.dispose();
                env.dispose();
            }
            else if (status == GRB.UNBOUNDED) {
                System.out.println("LP RESULT : UN_BOUNDED");
                model.dispose();
                env.dispose();
            }
            else {
                model.dispose();
                env.dispose();

                System.out.println("LP Stopped with status = " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double isSameSwitch(PhySwitchPort phyport1, PhySwitchPort phyport2) {
        if (phyport1.getParentSwitch().equals(phyport2.getParentSwitch())) {
            return 1.0;
        }
        return 0.0;
    }

    private Double isDirectlyConnected(PhySwitchPort phySwitchPort, PhyHost phyHost) {
        PhyHostLink phySLink = new PhyHostLink("someLink", phySwitchPort, phyHost);

        if (physicalTopo.getHostLinks().contains(phySLink)) {
            return 1.0;
        }
        return 0.0;
    }

    private void printAndStoreSolution(GRBVar[][] switchPortMapper, GRBVar[][] hostMapper) throws Exception {
        for (int i=0;i<virtualTopo.getSwitchPorts().size() ;i++) {
            for (int j=0;j< physicalTopo.getSwitchPorts().size();j++) {
                if (switchPortMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                    System.out.println(virtualTopo.getSwitchPorts().get(i).toString() +" Mapped to "+ physicalTopo.getSwitchPorts().get(j).toString());
                }
            }
        }

        for (int i=0;i<virtualTopo.getHosts().size() ;i++) {
            for (int j=0;j< physicalTopo.getHosts().size();j++) {
                if (hostMapper[i][j].get(GRB.DoubleAttr.X) == 1.0) {
                    System.out.println(virtualTopo.getHosts().get(i).toString() +" Mapped to "+ physicalTopo.getHosts().get(j).toString());
                }
            }
        }
    }
}