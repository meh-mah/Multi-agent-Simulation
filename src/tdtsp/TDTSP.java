package tdtsp;

import java.util.ArrayList;
import java.util.Collections; 
import jbarge.Barge; 
 /**
 * An implementation of the TDTSP. This object is used by a barge to compute
 * the best rotation and it also stores information about the rotation.
 * It is based on the algorithm developed by Malandraki and Dial (1996).
 * The implementation uses a recursive algorithm / backtracking technique
 * and is partly based on an implementation of the eight queens problem as shown
 * in "Java for Everyone, 2e by Cay Horstmann.
 */
 public class TDTSP{

 /**
 * The sailing times. This is a smaller version of the sailing times table in the simulation class. It
 * only contains the terminals that this barge has to visit.
 */
 public int[][] sailingTimes;

 /**
 * The barge for which the TDTSP is solved.
 */
 Barge barge;

 /**
 * Number of nodes in the graph, excluding the depot (support up to 27 nodes excluding depot, can easily be extended if necess
 */
 public int numNodes;

 /**
 * The best route is the route with the lowest sojourn time. If there is more than one route, then it is the route with the lo
 */
 public String bestRoute;

 /**
 * The best leave time is the time in of leaving the port. The best sojourn time is
 * the time of leaving the port minus the time of arrival at the port.
 */
 public int bestLeaveTime, bestSojournTime;

 /**
 * The number of 0's is the number of nodes excluding the depot node 0, plus 5 zeros to store the last node visited.
 * This is created by the method initial()
 */
 String sInitial;

 int startTime;

 //the current stage and the previous stage is retained.
 Stage currentStage;

 //Accepted tours with corresponding cost
 ArrayList<String> acceptedTour;
 ArrayList<Integer> acceptedCost;

 public TDTSP(int startTime, Barge barge){

 this.barge = barge;
 this.startTime = startTime;
 this.sailingTimes = barge.sailingTimes;
 this.numNodes = sailingTimes.length-1;
 this.sInitial= this.initial(numNodes);

 this.acceptedTour = new ArrayList<String>();
 this.acceptedCost = new ArrayList<Integer>();
 this.currentStage = new Stage();

 PartialSolution start = new PartialSolution(sInitial,sInitial, this);
 this.solve(start,this.startTime, "");

 this.bestRoute();

 this.currentStage.cost.clear();
 this.currentStage.pred.clear();
 this.currentStage.set.clear();
 }

 public void solve(PartialSolution sol, int time, String pred){



 //compute cost to cost (cost = arrival time)
 int tdtt = sol.timeDependentTravelTime(time);
 time += tdtt;

 this.currentStage.addCost(time);
 //add to set
 this.currentStage.addSet(sol.currentSet);
//add to route
 pred = pred +" "+ sol.getLastNodeVisited();
 this.currentStage.addPred(pred);

 int exam = sol.examine();
 if (exam == PartialSolution.ACCEPT){

 //return to the depot
 int lastNode = sol.getLastNodeVisited();
 time+=sol.computeCost(lastNode, 0);
 pred = pred +" "+ 0;

 //add solution to list
 this.acceptedTour.add(pred);
 this.acceptedCost.add(time);

 }
 else if (exam == PartialSolution.CONTINUE){
 for(PartialSolution p : sol.extend()){
 solve(p, time, pred);
 }
 }
 }

 /**
 * Initialize the first set binary string.
 * Number of 0's is the number of nodes excluding the depot node 0, plus 5 zeros to store the last node visited.
 * Nodes visited and last node node visited (does not include depot), also known as set S and node k.
 * @param numNodes
 * @return
 */
 public String initial(int numNodes){
 String s= "00000";
 for(int i = 0; i<numNodes; i++){
 s+="0";
 }
 return s;
 }

 /**
 * Set the best route to the route with the least cost from the accepted tours list.
 */
 public void bestRoute(){
 int index = this.minimizeSailingtime();
 this.bestRoute = this.acceptedTour.get(index);
 this.bestLeaveTime = this.acceptedCost.get(index);
 this.bestSojournTime = this.bestLeaveTime - this.startTime;
 }

 /**
 * This method converts the best route to a printable String for the console
 * @return a string with the best route and associated cost
 */
 public String bestRouteToString(){
 String best = this.bestRoute;
 String converted = "";
 for(int i=0; i<best.length(); i++){
 String c = ""+best.charAt(i);
 if(c.equals(" ")){
 converted += " ";
 }
 else{
 int ter = Integer.parseInt(c);
 converted += barge.terminals.get(ter).toString();
 }
 }
 return converted;
 }

 /**
 * Puts all the accepted tours with associated cost in a String that is made for the console
 * @return
 */
 public String getAccepted(){

 String acceptedTours = "";

 for(int i=0; i<this.acceptedTour.size(); i++){
 //get tour at index i
 String tour = acceptedTour.get(i);

 //convert tour
 String convTour = "";
 for(int j=0; j<tour.length(); j++){
 String c = ""+tour.charAt(j);
 if(c.equals(" ")){
 convTour += " ";
 }
 else{
 int ter = Integer.parseInt(c);
 convTour += barge.terminals.get(ter).toString();
 }
}

 //get associated costs
 int cost = this.acceptedCost.get(i);

 //add to the string
 acceptedTours += "Tour: " + convTour + "\t Cost: " + cost + "\n";
 }

 return acceptedTours;

 }

 /**
 * If there are more than 1 best tour this method will select the tour
 * with the least sailing time. A solution with less sailing time is
 * preferred because it will cost less fuel cost.
 * @return the index in acceptedTours of the tour with the least sailingTime
 *
 */
 public int minimizeSailingtime(){
 //a list to store all indexes with the lowest costs
 ArrayList<Integer> indexes = new ArrayList<Integer>();

 // find the lowest cost
 int minCost = Collections.min(this.acceptedCost);
 int indexBestTour = this.acceptedCost.indexOf(minCost);

 // find all tours with that cost
 for(int i=0; i<this.acceptedCost.size(); i++){
 //check if tours has the same cost as minCost
 if(this.acceptedCost.get(i) == minCost){
 //add the index
 indexes.add(i);
 }
 }

 //check if there is more than 1 best tours
 if(indexes.size()>1){
 //a list to save the sailing times
 ArrayList<Integer> sTimes = new ArrayList<Integer>(indexes.size());

 //compute the sailing times for each of these tours
 for(int i : indexes){
 //get the tour and compute its sailing time
 String tour = this.acceptedTour.get(i);
 int sailingTime = this.computeSailingtime(tour);
 //add the sailing to the list
 sTimes.add(sailingTime);
 }

 // index of the minimum sailing time. if there are more than 1 than the
 // first occurrence in the list is sufficient.
 int minSailingTime = Collections.min(sTimes);
 int indexSTimes = sTimes.indexOf(minSailingTime);
 indexBestTour = indexes.get(indexSTimes);
 }
 return indexBestTour;
 }

 /**
 * Compute the total sailing time of a specific tour. This method is used
 * in the minSailingTime() method.
 * @param route a String representation of the tour. e.g., "0 1 5 9 0"
 * @return the sailing time of a tour
 *
 */
 public int computeSailingtime(String route){
 int sailingTime=0;

 // iterate over the String
 // -2 because the last destination node is already reached at tour.length()-2
 for(int i=0; i<route.length()-2; i++){
 String c = ""+route.charAt(i);
 if(!c.equals(" ")){
 int origin = Integer.parseInt(c);
 //the destination node is 2 indexes further in String tour
 String d = ""+route.charAt(i+2);
 int destin = Integer.parseInt(d);

 //add sailing time between this node and the next node
 sailingTime+=this.sailingTimes[origin][destin];
 }
 }
 return sailingTime;
 }
 }
