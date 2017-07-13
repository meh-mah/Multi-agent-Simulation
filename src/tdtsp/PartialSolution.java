package tdtsp;

import java.util.ArrayList; 
 import jbarge.Terminal;

 /**
 * Partial solution of TDTSP.
 */
 public class PartialSolution{

 public String currentSet, previousSet;

 public static final int ACCEPT = 1;
 public static final int ABANDON = 2;
 public static final int CONTINUE = 3;

 TDTSP tdtsp;

 /**
 * Constructor of PartialSolution
 * @param currentSet example 000000110001011
 * @param previousSet example 000000110001101
 * @param tdtsp the TDTSP object
 */
 public PartialSolution(String currentSet, String previousSet, TDTSP tdtsp){
 this.currentSet = currentSet;
 this.previousSet = previousSet;
 this.tdtsp=tdtsp;
 }

 /**
 * add node
 * @return
 */
 public ArrayList<PartialSolution> extend(){
 ArrayList<PartialSolution> set = new ArrayList<PartialSolution>();
 for(int i = tdtsp.numNodes-1; i>=0; i--){
 String node = ""+currentSet.charAt(i);
 String newNode = "";
 if(node.equals("0")){

 //set unvisited node from 0 to 1
 String newSet = currentSet.substring(0,i) + '1' + currentSet.substring(i+1, tdtsp.numNodes) ;

 //set the last visited to the new node visited and get the corresponding 5 bit notation
 newNode = Integer.toBinaryString(tdtsp.numNodes-i);
 String zeros="";
 for(int u=0; u< 5-newNode.length(); u++){
 zeros+="0";
 }
 newNode=zeros+newNode;

 //add the last node visited to the new set
 newSet+=newNode;

 //create the partial solution
 PartialSolution newSol = new PartialSolution(newSet, currentSet, tdtsp);

 //add the partial solution to set
 set.add(newSol);
 }
 }
 return set;
 }

 /**
 * examine whether to accept, or continue
 * @return
 */
 public int examine(){
 if(this.getNumVisited() == tdtsp.numNodes){
 return ACCEPT;
 }
 else{
 return CONTINUE;
 }
 }


 /**
 * get the time dependent travel time between i (origin) and j (destination)
 * as a function of the departure time from the origin node i of the link.
 * @param departureTime
 * @return
 */
 public int timeDependentTravelTime(int departureTime){

 int originIndex = getLastNodeVisited(previousSet);
 int destinIndex = getLastNodeVisited(currentSet);

 Terminal destinTerminal = tdtsp.barge.terminals.get(destinIndex);
int sailingTime = tdtsp.sailingTimes[originIndex][destinIndex];
 int waitingTime = tdtsp.barge.waitingProfiles.get(destinTerminal).getMaxWaitingTime(departureTime+sailingTime);
 int handlingTime = tdtsp.barge.handlingTimes.get(destinIndex);

 int tdtt = sailingTime + waitingTime + handlingTime;

 return tdtt;
 }

 /**
 * get last node from the current set
 * @return
 */
 public int getLastNodeVisited(){
 return getLastNodeVisited(currentSet);
 }

 /**
 * get last node from binary string s
 * @param s example 000000110001011 (the last five digits contains info about the last visited node)
 * @return the index of the the terminal in barge.terminals
 */
 public int getLastNodeVisited(String s){
 String last="";
 for(int i = tdtsp.numNodes; i<tdtsp.numNodes+5; i++){
 last=last+s.charAt(i);
 }
 int b = Integer.parseInt(last,2);
 return b;
 }

 /**
 * get number of visited nodes
 * @return the number of visited nodes
 */
 public int getNumVisited(){
 int numVisited = 0;
 for(int i = tdtsp.numNodes-1; i>=0; i--){
 String node = ""+currentSet.charAt(i);
 if(node.equals("1")){
 numVisited++;
 }
 }
 return numVisited;
 }

 /**
 * get the cost of adding a node. si is the origin set, sj is the destination set.
 * note: only used to return to exit point
 * @return
 */
 public int computeCost(){
 int origin = getLastNodeVisited(previousSet);
 int destination = getLastNodeVisited(currentSet);;
 return tdtsp.sailingTimes[origin][destination];
 }

 /**
 * get the cost of adding a node.
 * still used for getting the cost of the return to the depot, which is not time dependent
 * @param origin example 000000110001011
 * @param destination example 000000110001011
 * @return
 */
 public int computeCost(int origin, int destination){
 return tdtsp.sailingTimes[origin][destination];
 }

 }
