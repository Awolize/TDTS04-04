import javax.swing.*;
import java.util.Arrays;

public class RouterNode {
    private int myID;
    private GuiTextArea myGUI;
    private RouterSimulator sim;

    private boolean PoisonReverse = false;
    private int INFINITY = RouterSimulator.INFINITY;
    private int numOfNodes = RouterSimulator.NUM_NODES;

    private int[] costs = new int[RouterSimulator.NUM_NODES];
    private int[] route = new int[RouterSimulator.NUM_NODES];
    private int[][] distanceTable = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];

    //--------------------------------------------------
    public RouterNode(int ID, RouterSimulator sim, int[] costs) {
      	myID = ID;
      	this.sim = sim;
      	myGUI = new GuiTextArea("  Output window for Router #" + ID + "  ");

      	System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

      	// init [distanceTable]
      	for (int i = 0; i < numOfNodes; i++)
      	    for (int j = 0; j < numOfNodes; j++)
      		      if (i == j) // set distance to itself to 0
      		          distanceTable[i][j] = 0;
      		      else if (i == myID) // transfer known distances
      			        distanceTable[myID][j] = costs[j];
      		      else // unknown(remaining) with infinity
      			        distanceTable[i][j] = INFINITY;

      	// init [route] (-1 = no direct path)
      	for (int i = 0; i < numOfNodes; i++)
      	    if (costs[i] != INFINITY)
                route[i] = i;
      	    else
      		      route[i] = -1;

      	// notify connected nodes
      	broadcast();
      	printDistanceTable();
    }

    //--------------------------------------------------
    public void recvUpdate(RouterPacket pkt) {
      	// Check if
      	distanceTable[pkt.sourceid] = pkt.mincost;
      	if (Bellman())
      	    broadcast();
    }

    //--------------------------------------------------
    private void sendUpdate(RouterPacket pkt) {
      	// PoisonReverse
      	if (PoisonReverse)
      	    for (int i = 0; i < numOfNodes; i++)
            		if (distanceTable[myID][i] == pkt.destid)
            		    pkt.mincost[i] = INFINITY;
      	sim.toLayer2(pkt);
    }

    //--------------------------------------------------
    public void printDistanceTable() {
      	myGUI.println("\n\n\nCurrent table for " + myID + " at time " + sim.getClocktime());
      	myGUI.println("Distancetable: ");
      	myGUI.print("     dst  |");
      	for(int i = 0; i < numOfNodes; i++)
      	    myGUI.print("\t" + i);

      	myGUI.print("\n-------------");

      	for(int i = 0; i < numOfNodes; i++)
      	    myGUI.print("--------------------");
      	for(int i = 0; i < numOfNodes; i++)
      	    if (i != myID && costs[i] != INFINITY)
      	    {
      		      myGUI.print("\n" + " nbr " + i + "   | " + "\t");
      		      for(int j = 0; j < numOfNodes; j++)
      		          myGUI.print(distanceTable[i][j] + "\t");
      	    }
      	myGUI.print("\n\n");

      	myGUI.println("Our distance vector and routes: ");
      	myGUI.print("     dst  |");
      	for(int i = 0; i < numOfNodes; i++)
      	    myGUI.print("\t" + i);

      	myGUI.print("\n-------------");
      	for(int i = 0; i < numOfNodes; i++)
      	    myGUI.print("--------------------");

      	myGUI.print("\n   cost  |\t");
      	for(int i = 0; i < numOfNodes; i++)
            myGUI.print(String.valueOf(distanceTable[myID][i]) + "\t");

      	myGUI.print("\n  route |\t");
      	for(int i = 0; i < RouterSimulator.NUM_NODES; i++)
            if(route[i] == -1)
                myGUI.print("-\t");
            else
                myGUI.print(String.valueOf(route[i]) + "\t");
    }
    //--------------------------------------------------
    public void updateLinkCost(int dest, int newcost) {
      	costs[dest] = newcost;
      	distanceTable[myID][dest] = newcost;
      	if (Bellman())
      	    broadcast();
    }

    private void broadcast()
    {
      	for (int i = 0; i < numOfNodes; i++)
      	    if (i != myID && costs[i] != INFINITY && route[i] != -1)
      		      sendUpdate(new RouterPacket(myID, i, distanceTable[myID]));
    }

    // Bellman-Ford algorithm
    private boolean Bellman()
    {
      	Boolean changed = false;
      	for (int i = 0; i < numOfNodes; i++) // myID = a, i = c (dest)
      	{
      	    if (i == myID)
      		      continue;

      	    int distanceToNextRouter = distanceTable[myID][route[i]];
      	    int distanceFromNextRouterToGoalRouter = distanceTable[route[i]][i];
      	    int estimatedRouteCost = distanceToNextRouter + distanceFromNextRouterToGoalRouter;

      	    // compare old cost to new cost
      	    if (distanceTable[myID][i] != estimatedRouteCost)
      	    {
            		distanceTable[myID][i] = estimatedRouteCost;
            		if (distanceTable[myID][i] > costs[i])
            		{
            		    distanceTable[myID][i] = costs[i];
            		    route[i] = i;
            		}
            		changed = true;
      	    }

      	    for (int j = 0; j < numOfNodes; j++)
      	    {
            		int currentcost = distanceTable[myID][j];
            		int newcost = distanceTable[myID][i] + distanceTable[i][j];
            		if (newcost < currentcost)
            		{
            		    distanceTable[myID][j] = newcost;
            		    route[j] = route[i];
            		    changed = true;
            		}
      	    }
      	}
      	return changed;
    }
}
