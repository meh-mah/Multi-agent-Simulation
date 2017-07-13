package jbarge;

import java.util.ArrayList;
 /**
 * Waiting profiles. Terminals provide barges information about
 * the maximum amount of time a barge has to wait until its processing
 * is started after it has arrived. This information is provided for
 * every possible arrival moment during a certain time horizon in the
 * form of a waiting profile.
 */
 public class WaitingProfile {

 /**
 * Indexes of integer array: 0=startInterval, 1=startTime, 2=endTime, 3=insertionPoint
 */
 ArrayList<int[]> startIntervals;

 /**
 * Indexes of integer array: 0=Time, 1=Maximum waiting time, 2=Insertion Point
 */
 ArrayList<int[]> waitingProfile;

 int currentTime;

 Terminal terminal;

 /**
 * construct the waiting profile
 * @param terminal
 * @param barge
 * @param currentTime the arrival time in the port
 */
 public WaitingProfile(Terminal terminal, Barge barge, int currentTime){
 this.currentTime=currentTime;
 this.terminal=terminal;
 // there is no waiting at the port entrance, therefore we set all the values to 0
 if(terminal.toString().equals("t0")){
 waitingProfile = new ArrayList<int[]>();
 this.waitingProfile.add(new int[]{0,0,0});
 startIntervals = new ArrayList<int[]>();
 this.startIntervals.add(new int[]{0,0,0,0});
 }
 else{
 this.startIntervals(terminal,barge);
 this.waitingProfile();
 }
 }


 /**
 * Determine start intervals
 * @param terminal
 * @param barge
 * @return the start interval
 */
 public ArrayList<int[]> startIntervals(Terminal terminal, Barge barge){

 // every integer array in this list contains: startInterval, startTime, endTime
 startIntervals = new ArrayList<int[]>();

 for(int i=0; i<=terminal.appointments.size(); i++){
 // declare the values to compute for each interval
 int startInterval, startTime, endTime;

 // start interval and insertion point
 startInterval = i+1;

 // start time
 if(i==0){
 startTime = currentTime;
 }
 else{
 // start time is equal to the EDT of the last planned barge before insertion point i
 Barge key = (Barge) terminal.appointments.keySet().toArray()[i-1];
 int[] appointment = terminal.appointments.get(key);
 startTime = appointment[4]; //index 4 is EDT
 }

 // end time
 if(i==terminal.appointments.size()){
 endTime = Integer.MAX_VALUE; //used as infinity
 }
 else{
 // The end time of the start interval is equal to the
 // PST of the first planned barge after insertion point i,
 // minus the processing time of barge b. (They take LST in the example,
 // because they plan barges after i as late as possible)
 Barge key = (Barge) terminal.appointments.keySet().toArray()[i];
 int PST_i = terminal.appointments.get(key)[1];//index LST is 1
 int indexTerminal = barge.terminals.indexOf(terminal);
 int handlingTime = barge.handlingTimes.get(indexTerminal);
endTime = PST_i - handlingTime;
 }

 //add to startInterval ArrayList
 //if feasible (?). i.e., startTime < endTime
 if(startTime<=endTime && startTime>=this.currentTime){
 startIntervals.add(new int[]{startInterval, startTime, endTime});

 }

 }

 //check if intervals are disjoint
 for(int i=0; i<startIntervals.size()-1; i++){
 int end_i = startIntervals.get(i)[2];
 int start_i1 = startIntervals.get(i+1)[1];
 if(end_i > start_i1){
 int[] startInterval = startIntervals.get(i);
 startInterval[2] = start_i1;
 startIntervals.set(i, startInterval);
 end_i = start_i1;
 }
 }

 return startIntervals;
 }

 /**
 * Slack is added to maximum waiting time (mwt). Slack is a parameter which can be configured in the repast GUI.
 * Furthermore, the slack method and slack denominator can also be configured in the GUI.
 * @param t arrival time
 * @return maximum waiting time
 */
 public int getMaxWaitingTime(int t){

 //search index of waiting profile to use
 for(int i=(waitingProfile.size()-1); i>=0; i--){
	 
	 /*if (t>=startIntervals.get(i)[1]&&t<=startIntervals.get(i)[2]){
	 mwt=0;
	 break;
 } else if (i<startIntervals.size()-1){
	 if(t>startIntervals.get(i)[2]&& t<startIntervals.get(i+1)[1]){
		 mwt=startIntervals.get(i+1)[1]-t;
		 break;
	 }
	 
 }*/

 if(t>=waitingProfile.get(i)[0]){

 int mwt;

 // mwt maximum waiting time + current time - arrival time
 int time = waitingProfile.get(i)[0];
 int maxWaitingTime = waitingProfile.get(i)[1];
 if (i>0 && t==waitingProfile.get(i)[0]){
	 mwt=0;
 }else{
	 mwt = maxWaitingTime + time - t;
 }
 

 if(mwt<0){
 mwt=0;
 }
 if(Port.slackMethod.equals("Constant")){
 return (mwt + Port.slack);
 }
 else if(Port.slackMethod.equals("Factor")){
 //get the number of appointments in the terminal schedule
 int numAppointments = terminal.appointments.size();
 int slack = numAppointments / Port.slackDenominator * mwt;
 return (mwt + slack);
 }
 }
 }
 return 0; // this will never be returned, see above
 }

 public ArrayList<int[]> waitingProfile(){
 // every integer array in this list contains: Time, Maximum waiting time
 waitingProfile = new ArrayList<int[]>();
 int time, maximumWaitingTime;

 for(int i=-1; i<startIntervals.size()-1; i++){
 if(i==-1){
 time=currentTime;
 }
 else{
 time=startIntervals.get(i)[2];
 }
 maximumWaitingTime=startIntervals.get(i+1)[1]-time;

 //if(maximumWaitingTime>=0){
 waitingProfile.add(new int[]{time, maximumWaitingTime});
 //}
 }
 return waitingProfile;
 }

 /**
 *
 * @return String with the start intervals.
 */
 public String startIntervalsToString(){
 String s = "";
 for(int[] i: this.startIntervals){
	for(int j:i){
		 s+=j +" ";
		 }
		 s+="\n";
		 }
		
		 return s;
		 }
		
		 /**
		 * @return String with the waiting profile.
		 */
		 public String toString(){
		
		 String s = "";
		 //add the table with the time, max waiting time
		 for(int[] i: this.waitingProfile){
		 for(int j:i){
		 s += j +" ";
		 }
		 s += "\n";
		 }
		 return s;
		 }
		 }
