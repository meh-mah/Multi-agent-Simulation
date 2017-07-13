package jbarge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.util.ContextUtils;
import tdtsp.TDTSP;

import com.google.common.collect.Table;

 /**
 * The Barge object represents the barge agent and everything that is associated with a barge.
 */
 public class Barge{

 /**
 * The barge number is used to identify the barge. The first barge gets the number 0 so
 * that it is the same as the index number of the barge list in the simulation class.
 */
 public int bargeNumber;

 /**
 * The arrival time at the port. This is derived from the tick count of the simulation
 * schedule and is used as input for the TDTSP.
 */
 public int arrivalTime;

 /**
 * The terminals this barge has to visit. This list is parallel to the handlingTimes List.
 * At index 0 is the port entrance.
 */
 public ArrayList<Terminal> terminals;
/**
 * unvisited terminals
 */
 public ArrayList<Terminal> visitedTerminals;
 /**
 * The handling time at each terminal. This list is parallel to the terminals List.
 */
 public ArrayList<Integer> handlingTimes;

 /**
 * This is a smaller version of the sailing times table in the Simulation class.
 * It contains only the terminals this barge has to visit and is used as input for the TDTSP.
 */
 public int[][] sailingTimes;

 /**
 * Stores the waiting profile for each terminal. It is used as input for the TDTSP.
 */
 public Map<Terminal, WaitingProfile> waitingProfiles;

 /**
 * This TDTSP object handles the computation of the route and also stores all
 * information associated with it.
 */
 public TDTSP tdtsp;

 /**
 * This map stores the appointments. The key of the map is the Terminal. The value is
 * an integer array with the following indexes: 0=LAT, 1=LST. LAT=Latest arrival time,
 * LST=Latest starting time.
 */
 Map<Terminal,int[]> appointments;

 /**
 * The remaining route is derived from the bestRoute in the TDTSP object.
 * It is used as input for scheduleArrivalTerminal method.
 * Every time a barge is finished at a terminal a node is removed from the remaining route.
 */
 String remainingRoute;

 /**
 * To store the appointments of the terminals at the time of constructing the waiting profile.
 * This is only used to print the appointments together with the barge info. This is needed when
 * the correctnes of the waiting profiles is checked.
 */
 public String terminalAppointments;

 /**
 * Barge state.
 */
 int state;
 public static final int SAILING = 1;
 public static final int WAITING = 2;
 public static final int HANDLING = 3;
 public int LEFT = 0;

 /**
 * Barge statistic.
 */
 public int actualSojourntime, totalSailingTime, totalWaitingTime, totalHandlingTime, differenceExpectedActual, expectedLeavetime, bestSTime;
/**
 * Stores the satisfaction on a scale from 1 to 7, where 1 is completely dissatisfied and 7 completely satisfied.
 * The informationSatisfaction relates to the difference between the expected and actual sojourn time.
 * The waitingtimeSatisfaction relates to the waiting time with respect to the service time.
 */
 public int informationSatisfaction, waitingtimeSatisfaction;

 /**
 * Fraction waiting time with respect to handling time. Used to determine the waitingtimeSatisfaction.
 */
 public double fraction;

 /**
 * The constructor of class Barge. This constructor assigns all attributes of a barge.
 * It also runs the commands that requests the waiting profiles from the terminals,
 * computes the best route using TDTSP and it schedules the arrival at the
 * first terminal.
 * @param bargeNumber The barge number is used to identify the barge
 * @param arrivalTime The arrival time at the port
 * @param terminals The terminals this barge has to visit
 * @param handlingtimes The handling time at each terminal
 */
 public Barge(int bargeNumber, int arrivalTime, ArrayList<Terminal> terminals,
 ArrayList<Integer> handlingtimes) {

 this.bargeNumber = bargeNumber;
 this.arrivalTime = arrivalTime;
 this.terminals = terminals;
 this.handlingTimes = handlingtimes;
 this.visitedTerminals=new ArrayList<Terminal>();

 this.totalSailingTime=0;
 this.totalWaitingTime=0;
 this.totalHandlingTime=0;

 this.createSailingTimes(Port.sailingtimesTable);
 this.requestWaitingProfiles();


 // start time for TDTSP = arrival time in port
 this.tdtsp = new TDTSP(arrivalTime, this);
 expectedLeavetime=this.tdtsp.bestLeaveTime;
 bestSTime=this.tdtsp.bestSojournTime;
 // make appointments
 this.addAppointments();

 // save the appointments of terminals so that they can be written to the spreadsheet
 if(Port.eventsToExcel.equals("Yes")){
 this.saveAppointmentsterminals();
 }

 this.state = SAILING;

 // to remove the first space of bestRoute we take this substring of bestRoute.
 this.remainingRoute = this.tdtsp.bestRoute.substring(1);

 // schedule the arrival at the first terminal.
 this.scheduleArrivalTerminal(this.remainingRoute, this.arrivalTime);
 }

 /**
 * Creates the two dimensional sailing times array that only contains the terminals this
 * barge has to visit. It is stored in sailingTimes.
 * @param sailingTimeTable The sailing times table containing all terminals in the port
 * @param Terminals 
 */
 public void createSailingTimes(Table<Terminal,Terminal,Integer> sailingTimeTable){
 int n = terminals.size();
 this.sailingTimes = new int[n][n];

 for(int i=0; i<n; i++){
 for(int j=0; j<n; j++){
 Terminal ti = terminals.get(i);
 Terminal tj = terminals.get(j);
 this.sailingTimes[i][j] = sailingTimeTable.get(ti,tj);
 }
 }
 }

 /**
 * Requests waiting profiles from terminals and stores them in waitingProfiles.
 */
 public void requestWaitingProfiles(){
 this.waitingProfiles = new HashMap<Terminal, WaitingProfile>();

 for(Terminal terminal : terminals){
 WaitingProfile waitingProfile = terminal.constructWaitingProfile(this,
 this.arrivalTime);
 waitingProfiles.put(terminal, waitingProfile);
 }
 }
 
private void requestWaitingProfiles(int newArrivalTime) {
	this.waitingProfiles = new HashMap<Terminal, WaitingProfile>();

	 for(Terminal terminal : terminals){
	 WaitingProfile waitingProfile = terminal.constructWaitingProfile(this,
			 newArrivalTime);
	 waitingProfiles.put(terminal, waitingProfile);
	 }
}

 /**
 * Makes appointments with terminals using the best rotation.
*/
public void addAppointments(){
	
	 this.appointments = new LinkedHashMap<Terminal, int[]>();
	
	 String bestRoute = tdtsp.bestRoute;
	 int departureTime = this.arrivalTime;
	 int departureNode = 0;
	 for(int i=0; i<bestRoute.length(); i++){
	 String c = ""+bestRoute.charAt(i);
	 // check if the character is a " ". example of bestRoute: " 1 2 3 4 0"
	 if(!c.equals(" ")){
	 // return terminal object at index c from the terminals list
	 int destination = Integer.parseInt(c);
	 Terminal ter = this.terminals.get(destination);
	
	 // compute latest arrival time (LAT) and latest starting time (LST)
	 int LAT = departureTime + this.sailingTimes[departureNode][destination];
	 int LST = LAT + this.waitingProfiles.get(ter).getMaxWaitingTime(LAT);
	
	 // add the appointment to the schedule of this barge
	 appointments.put(ter, new int[]{LAT,LST});
	
	 // send the LAT, LST and handling time to the terminal
	 int handlingTime = this.handlingTimes.get(destination).intValue();
	 ter.addAppointment(this, LAT, LST, handlingTime);
	
	 //update departureTime and departureNode for next iteration
	 departureTime = LST + handlingTime;
	 departureNode = destination;
	 }
	 }
	 }
private void addAppointments(int newArrivalTime) {
	
	 this.appointments = new LinkedHashMap<Terminal, int[]>();
	
	 String bestRoute = tdtsp.bestRoute;
	 int departureTime = newArrivalTime;
	 int departureNode = 0;
	 for(int i=0; i<bestRoute.length(); i++){
	 String c = ""+bestRoute.charAt(i);
	 // check if the character is a " ". example of bestRoute: " 1 2 3 4 0"
	 if(!c.equals(" ")){
	 // return terminal object at index c from the terminals list
	 int destination = Integer.parseInt(c);
	 Terminal ter = this.terminals.get(destination);
	
	 // compute latest arrival time (LAT) and latest starting time (LST)
	 int LAT = departureTime + this.sailingTimes[departureNode][destination];
	 int LST = LAT + this.waitingProfiles.get(ter).getMaxWaitingTime(LAT);
	
	 // add the appointment to the schedule of this barge

	 appointments.put(ter, new int[]{LAT,LST});
	
	 // send the LAT, LST and handling time to the terminal
	 int handlingTime = this.handlingTimes.get(destination).intValue();
	 ter.addAppointment(this, LAT, LST, handlingTime);
	 //System.out.println("appointments at "+ter+ter.appointments+"after readding "+this);
	
	 //update departureTime and departureNode for next iteration
	 departureTime = LST + handlingTime;
	 departureNode = destination;
	 }
	 }
	
}
	
	 /**
	 * Schedules the arrival at the next terminal.
	 * @param route the (remaining) route. Example of the String: "0 1 2 3 4 5 0"
	 * @param time departure time from the previous point
	 */
	 public void scheduleArrivalTerminal(String route, int time){
	
	 // determine sailing time
	 int sailingTime=0;
	 Terminal terminalDestin = null;
	 for(int i=0; i<route.length()-2; i++){
	 String c = ""+route.charAt(i);
	 if(!c.equals(" ")){
	 int origin = Integer.parseInt(c);
	 // the destination node is 2 indexes further in String route
	 String d = ""+route.charAt(i+2);
	 int destin = Integer.parseInt(d);
	
	 // get sailing time between the depot node and the first terminal
	 sailingTime=this.sailingTimes[origin][destin];
	
	 // set terminal to this terminal, this is input for the scheduled action below
	 terminalDestin = this.terminals.get(destin);
	
	 // end loop
	 break;
	 }
	 }
	
	 if(Port.model.equals("Stochastic")){
	 sailingTime = (int) Math.round(Port.timeRNG.nextGaussian(sailingTime, Port.timeSigma));
	 if(sailingTime<1){
	 sailingTime=1;
	 }
	 }
	
	 // add to statistic
	 this.totalSailingTime+=sailingTime;
	
	 // arrival time at the terminal
	 int arrivalTimeTerminal = time + sailingTime;
	
	 // schedule the arrival in the simulation schedule
	 Port.schedule.schedule(
	 ScheduleParameters.createOneTime(arrivalTimeTerminal), this, "arriveAtTerminal",
	 terminalDestin, arrivalTimeTerminal);
	
	 }
	
	 /**
	 * [ISchedulableAction] Arrive at Terminal. The terminal decides when the handling starts.
	 * @param terminal The terminal at which the barge arrives
	 * @param time The arrival time at the terminal
	 */
	 public void arriveAtTerminal(Terminal terminal, int time){
	
	 // add to queue, even if the terminal is idle. the terminal agent will take care of the rest 
		 //this moved to terminal class atarival method
		//terminal.queue.add(this);
	// set state of barge to waiting
	 this.state = Barge.WAITING;
	
	 if(Port.eventsToExcel.equals("Yes")){
	 Port.stats.addEvent(time, this.bargeNumber, ("Arrived at Terminal " + terminal.toString()+
	 ". Expected - actual = " + this.appointments.get(terminal)[0]+" - " + time + " = "
	 + (this.appointments.get(terminal)[0]-time)));
	 }
	
	 // schedule the start handling of this barge at the terminal in the simulation schedule
	 Port.schedule.schedule(
	 ScheduleParameters.createOneTime(time), terminal, "bargeArrives", this, time);
	 }
	
	 /**
	 * Actions for the barge after it finished handling at a terminal.
	 * @param time the time it finished handling at the last visited terminal
	 */
	 public void afterFinish(int time, Terminal ter){
	
	 this.state=Barge.SAILING;
	//remove from list so if barge has to calculate rotation again due to delay it should not include this terminal
	this.visitedTerminals.add(ter);
	
	 // if the barge visits more than 1 terminal
	 // remove a node from remainingRoute (example: "1 2 3 4 0" ---> "2 3 4 0")
	 if(this.remainingRoute.length()>3){
	 this.remainingRoute = this.remainingRoute.substring(2);
	 }
	
	 // check if there is another terminal to visit
	 if(this.remainingRoute.length()>3){
	 this.scheduleArrivalTerminal(this.remainingRoute, time);
	 }
	 else{
	 // sail to port exit point, i.e., schedule leaving the port in the simulation schedule
	 int lastTerminal = Integer.parseInt(""+this.remainingRoute.charAt(0));
	 int exitPoint = Integer.parseInt(""+this.remainingRoute.charAt(2));
	 int sailingTime = this.sailingTimes[lastTerminal][exitPoint];
	 if(Port.model.equals("Stochastic")){
	 sailingTime = (int) Math.round(Port.timeRNG.nextGaussian(sailingTime, Port.timeSigma));
	 if(sailingTime<1){
	 sailingTime=1;
	 }
	 }
	 // add to statistic
	 this.totalSailingTime+=sailingTime;
	 // schedule arrival at exit point
	 int arrivalTimeAtExit = time + sailingTime;
	 Port.schedule.schedule(ScheduleParameters.createOneTime(arrivalTimeAtExit,ScheduleParameters.LAST_PRIORITY)
	 , this, "leavePort", arrivalTimeAtExit);
	 }
	 }
	
	 /**
	 * [ISchedulableAction] Leaves the port.
	 * @param time the time the barges leaves the port
	 */
	 public void leavePort(int time){
	
	 //only save statistics of barge 301 to 800.
		 LEFT++; 
	 if(this.bargeNumber>-1 && this.bargeNumber<=800000){
	
	 Port.stats.bargesLeftPort++;
	 Port.stats.descriptiveStatistics[0].addValue(bestSTime);
	
	 this.actualSojourntime = time-this.arrivalTime;
	 Port.stats.descriptiveStatistics[1].addValue(this.actualSojourntime);
	
	 Port.stats.descriptiveStatistics[3].addValue(this.totalHandlingTime);
	 Port.stats.descriptiveStatistics[4].addValue(this.totalSailingTime);
	
	 this.totalWaitingTime = time-this.arrivalTime - this.totalHandlingTime - this.totalSailingTime;
	 Port.stats.descriptiveStatistics[2].addValue(this.totalWaitingTime);
	
	 differenceExpectedActual = expectedLeavetime-time;
	
	 this.determineSatisfaction();
	
	 if(Port.eventsToExcel.equals("Yes")){
	 Port.stats.addEvent(time, this.bargeNumber, ("Left port. Expected - actual = " +
	 expectedLeavetime + " - "+ time + " = " + differenceExpectedActual));
	 }
	
	 //add to barge stats
	 if(Port.bargesDetailsToExcel.equals("Yes")){
	 Port.stats.addSinglebargeinfo(this);
	 }
	 }
	
	 // remove from context: first get the context by using one of the terminals, then use it to remove the barge
	 @SuppressWarnings("unchecked")
	 Context<Object> context = ContextUtils.getContext(Port.terminals.get(0));
	context.remove(this);
	 }
	
	 public String toString(){
	 return "Barge "+ this.bargeNumber;
	 }
	
	 /**
	 *
	 * @return The appointments in a String
	 */
	 public String appointmentsToString(){
	 String str = "Schedule of barge "+ this.bargeNumber + "\n";
	 for(Terminal t : this.appointments.keySet()){
	 str += "Terminal " + t.name + " ";
	 for(int i : this.appointments.get(t)){
	 str += i +" ";
	 }
	 str += "\n";
	 }
	 return str+"\n";
	 }
	
	 /**
	 * Saves the appointment of the terminals at the time of constructing the waiting profile.
	 * This is required to save the appointments to barge_info in the spreadsheet.
	 *
	 */
	 public void saveAppointmentsterminals(){
	 //the appointments of the terminals this barge has to visit
	 terminalAppointments="";
	 for(int i=1; i<this.terminals.size(); i++){
	 terminalAppointments+=this.terminals.get(i).appointmentsToString();
	 }
	 }
	
	 /**
	 * Checks the satisfaction/happiness with (1) the information providing regarding the expected sojourn time
	 * and (2) the waiting time with respect to the service time.
	 */
	 public void determineSatisfaction(){
	
	 //checks if the difference between the expected and actual sojourn time is acceptable.
	 if(this.differenceExpectedActual > 179){
	 this.informationSatisfaction = 4;
	 }
	 else if(this.differenceExpectedActual > 37){
	 this.informationSatisfaction = 5;
	 }
	 else if(this.differenceExpectedActual > 7){
	 this.informationSatisfaction = 6;
	 }
	 else if(this.differenceExpectedActual > -3){
	 this.informationSatisfaction = 7;
	 }
	 else if(this.differenceExpectedActual > -7){
	 this.informationSatisfaction = 6;
	 }
	 else if(this.differenceExpectedActual > -15){
	 this.informationSatisfaction = 5;
	 }
	 else if(this.differenceExpectedActual > -34){
	 this.informationSatisfaction = 4;
	 }
	 else if(this.differenceExpectedActual > -79){
	 this.informationSatisfaction = 3;
	 }
	 else if(this.differenceExpectedActual > -180){
	 this.informationSatisfaction = 2;
	 }
	 else{
	 this.informationSatisfaction = 1;
	 }
	
	 //checks if the waiting time is proportional to the service time
	 fraction = (double)this.totalWaitingTime / (double)this.totalHandlingTime;
	
	 if(fraction < 0.61){
	 this.waitingtimeSatisfaction = 7;
	 }
	 else if(fraction < 0.76){
	 this.waitingtimeSatisfaction = 6;
	 }
	 else if(fraction < 0.95){
	 this.waitingtimeSatisfaction = 5;
	 }
	 else if(fraction < 1.2){
	 this.waitingtimeSatisfaction = 4;
	 }
	 else if(fraction < 1.54){
	 this.waitingtimeSatisfaction = 3;
	 }
	else if(fraction < 2){
		 this.waitingtimeSatisfaction = 2;
		 }
		 else{
		 this.waitingtimeSatisfaction = 1;
		 }
		 }

	public void recalculateRotation(int time) {
		//reset states
		for(Terminal ter: this.terminals){
			ter.appointments.remove(this);			
		}

		ListIterator<Terminal> it = visitedTerminals.listIterator();    
        while(it.hasNext()) {
        	Terminal t=it.next();
        	//System.out.println("wants to delete: "+t);
        	this.handlingTimes.remove(this.terminals.indexOf(t));
            this.terminals.remove(t);
        }
        this.visitedTerminals.clear();
        this.waitingProfiles.clear();
        this.appointments.clear();
        this.state=WAITING;
        this.terminalAppointments="";		
		 this.createSailingTimes(Port.sailingtimesTable);
		 this.requestWaitingProfiles(time);

		 // start time for TDTSP = arrival time in port
		 this.tdtsp = new TDTSP(time, this);

		 // make appointments
		 this.addAppointments(time);

		 // save the appointments of terminals so that they can be written to the spreadsheet
		 if(Port.eventsToExcel.equals("Yes")){
		 this.saveAppointmentsterminals();
		 }

		 this.state = SAILING;

		 // to remove the first space of bestRoute we take this substring of bestRoute.
		 this.remainingRoute = this.tdtsp.bestRoute.substring(1);

		 // schedule the arrival at the first terminal.
		 this.scheduleArrivalTerminal(this.remainingRoute, time);
		
	}

	


		 }
	