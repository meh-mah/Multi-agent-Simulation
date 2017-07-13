package jbarge;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import repast.simphony.engine.schedule.ScheduleParameters;

/**
 * Represents the terminal agent and everything that is associated with a
 * terminal.
 */
public class Terminal {

	/**
	 * The name of the terminal. This is used to identify the terminal.
	 */
	String name;

	/**
	 * The appointments this terminal made with barges are stored in this Map.
	 * The key is the barge. The value is an integer array. The meaning of the
	 * indexes in the integer array are as follows: 0 = LAT, 1 = LST, 2 = PST, 3
	 * = PT, 4 = EDT
	 */
	Map<Barge, int[]> appointments;

	/**
	 * The number of barges handling at the terminal.
	 */
	int numHandling;

	/**
	 * The queue at this terminal.
	 */
	Queue<Barge> queue;

	/**
	 * States
	 */
	public int state;
	public static final int HANDLING = 1;
	public static final int IDLE = 2;
	
	/**
	 * actual arrival time of the barge
	 */
	private Map<Barge, Integer> actualArrival;

	/**
	 * @param name
	 *            the name of the terminal. This is used to identify the
	 *            terminal.
	 */
	public Terminal(String name) {
		this.name = name;
		this.appointments = new ConcurrentHashMap<Barge, int[]>();
		this.actualArrival = new LinkedHashMap<Barge, Integer>();
		this.queue = new LinkedList<Barge>();
		this.state = IDLE;
		this.numHandling = 0;
	}

	/**
	 * Add a barge to the schedule. The schedule should be sorted on the latest
	 * starting time in order to construct waiting profiles. Because
	 * LinkedHashMap (in which schedule is stored) does not provide this
	 * functionality out of the box, we implemented that in this method as well.
	 * 
	 * @param barge
	 *            barge
	 * @param LAT
	 *            latest arrival time
	 * @param LST
	 *            latest starting time
	 * @param PST
	 *            planned starting time
	 * @param PT
	 *            processing time (handling time)
	 * @param EDT
	 *            expected departure time
	 */
	public void addAppointment(Barge barge, int LAT, int LST, int PT) {
		// determine position where the appointment should be inserted.
		// the schedule should be sorted on the LST in order to create
		// a right waiting profile
		// to keep track of the position we use integer i.
		int i = 0;
		for (Barge b : this.appointments.keySet()) {
			// compare LAT of Barge b with the LAT of the new barge
			if (LST > this.appointments.get(b)[1]) {
				i++;
			} else {
				break;
			}
		}

		// create a new LinkedHashMap that replaces appointments at the end of
		// this method
		LinkedHashMap<Barge, int[]> newAppointments = new LinkedHashMap<Barge, int[]>();

		for (int j = 0; j < appointments.size() + 1; j++) {

			if (j < i) {
				// add the appointment from appointments to newAppointments
				Barge key = (Barge) appointments.keySet().toArray()[j];
				int[] value = appointments.get(key);
				newAppointments.put(key, value);
			} else if (j == i) {
				// add the new appointment at this position
				int PST = LST;
				int EDT = PST + PT;
				newAppointments
						.put(barge, new int[] { LAT, LST, PST, PT, EDT });
			} else {
				// add the remaining appointment after the new appointment
				Barge key = (Barge) appointments.keySet().toArray()[j - 1];
				int[] value = appointments.get(key);
				newAppointments.put(key, value);
			}
		}
		// replace appointments with newAppointments
		this.appointments = newAppointments;
	}

	/**
	 * Construct and send the waiting profile
	 */
	public WaitingProfile constructWaitingProfile(Barge barge, int currentTime) {
		 /*if(Port.eventsToExcel.equals("Yes")){
			 Port.stats.addEvent(currentTime, barge.bargeNumber, (barge.toString()+ " asked Terminal " + this.toString()+
					 " for appoints "+
					 " ||appointements= " +this.appointmentsToString()));
			 }*/
		  
		 
		return new WaitingProfile(this, barge, currentTime);
	}

	/**
	 * [ISchedulableAction] Starts the handling of a barge
	 * 
	 * @param barge
	 *            the barge that arrived at the terminal
	 * @param time
	 *            the time at which the barge tries to start handing. this is
	 *            the arrival time at the terminal at the first try.
	 */
	public void bargeArrives(Barge barge, int time) {
		this.actualArrival.put(barge, time);
		this.queue.add(barge);
		//System.out.println(barge+" arrived to "+this.name+"// arraival list= "+this.actualArrival);

		if (Port.terminalLogic.equals("Unreserved")
				&& this.state == Terminal.IDLE) {
			// //////////////////////////////////the last two argument defined
			// by me////////////////////////////////////////////////////
			Port.schedule.schedule(ScheduleParameters.createOneTime(time,
					ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
					barge, time);
			
		} else if (Port.terminalLogic.equals("Reserved") && this.state == Terminal.IDLE) {
			boolean []c=checkIfHandleBarge(barge, time);
			//if could not handle the barge and it was late check && c[1] for next one
			if(!c[0] ){
				checkNextPossibleBargeToHandle(time);
			}
		}
	}
	
	/*
	 * returns a boolean array the cell 0 is if barge handled and cell 1 if rejected
	 */
	private boolean[] checkIfHandleBarge(Barge barge, int time) {
		boolean [] result={false, false};
		// if there is no other barge in the appointment list start handling
		// directly

		if (this.appointments.size() == 1) {
			Port.schedule.schedule(ScheduleParameters.createOneTime(time,
					ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
					barge, time);
			result[0]=true;
			return result;
		} else {
			// we need the following information to check whether or not the
			// barge can start handling
			
			
			/*Barge nextBargeInSchedule = (Barge) this.appointments.keySet().toArray()[0];
			Barge secondBargeInSchedule = (Barge) this.appointments.keySet().toArray()[1];*/
			int expectedEndTimeThisBarge = time+ barge.handlingTimes.get(barge.terminals.indexOf(this));
			int latThisBarge= this.appointments.get(barge)[0];
			//System.out.println(this.name+" wants to check "+barge+" in this arrival list "+this.actualArrival+" and que= "+this.queue);
			int actualArrive = this.actualArrival.get(barge);
			/*int lstNextAppointment = this.appointments.get(nextBargeInSchedule)[1];
			int lstSecondAppointment = this.appointments.get(secondBargeInSchedule)[1];*/
			
			
			//if next in schedule and it was not late
			if ((barge.equals(this.appointments.keySet().toArray()[0]) == true && latThisBarge >= actualArrive)) {
				this.state=Terminal.HANDLING;
				Port.schedule.schedule(ScheduleParameters.createOneTime(
						time, ScheduleParameters.LAST_PRIORITY), this,
						"handleBarge", barge, time);
				result[0]=true;
				return result;
			}
			//if next barge in schedule but it was late check to see if it is possible to handle it without interrupting other appointments
			else if ((barge.equals(this.appointments.keySet().toArray()[0]) == true && latThisBarge <= actualArrive)) {
				if (expectedEndTimeThisBarge <= this.appointments.get(this.appointments.keySet().toArray()[1])[1]){
					this.state=Terminal.HANDLING;
					Port.schedule.schedule(ScheduleParameters.createOneTime(
							time, ScheduleParameters.LAST_PRIORITY), this,
							"handleBarge", barge, time);
					
					result[0]=true;
					return result;
				} else {
					this.queue.remove(barge);
					this.appointments.remove(barge);
					//System.out.println(barge+" removed from "+this.name);
					//System.out.println(barge+" removed from the "+ this.name);
					if (Port.eventsToExcel.equals("Yes")) {
						Port.stats
								.addEvent(
										time,
										barge.bargeNumber,
										("rejected at Terminal "
												+ this.toString() + "due to delay"));
					}
					// notify barge to calculate rotation again
					barge.recalculateRotation(time);
					result[0]=false;
					result[1]=true;
					return result;				

				}
			}
			// if it is not the next one see if it is possible to service it without interrupting others
			//also if the next barge is late start this one
			else if ((barge.equals(this.appointments.keySet().toArray()[0]) == false && expectedEndTimeThisBarge <= this.appointments.get(this.appointments.keySet().toArray()[0])[1])
					||(barge.equals(this.appointments.keySet().toArray()[0]) == false && time > this.appointments.get(this.appointments.keySet().toArray()[0])[1])){
				this.state=Terminal.HANDLING;
				Port.schedule.schedule(ScheduleParameters.createOneTime(
						time, ScheduleParameters.LAST_PRIORITY), this,
						"handleBarge", barge, time);
				result[0]=true;
				return result;
			}			
		}
		return result;
	}

	private Boolean checkNextPossibleBargeToHandle(int time) {
		boolean found=false;
		if (this.queue.size() == 0) {
			this.state = Terminal.IDLE;
		}  else  {
			//if(this.state == Terminal.IDLE){
				//Iterator<Barge> it = this.appointments.keySet().iterator();
				Barge[] barges=this.appointments.keySet().toArray(new Barge[0]);

				for(Barge nextBargeInSchedule: barges){
					boolean kir=this.queue.contains(nextBargeInSchedule);
					if (kir) {
						boolean [] kos= checkIfHandleBarge(nextBargeInSchedule, time);
						if (kos[0]){
							this.state = Terminal.HANDLING;
							return found=true;						
						}
//						Port.schedule.schedule(ScheduleParameters.createOneTime(time,
//								ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
//								nextBargeInSchedule, time);
					}
				}
			//}
			
/*			//Barge nextBargeInSchedule = (Barge) this.appointments.keySet().toArray()[0];
			if (this.queue.contains(nextBargeInSchedule) == true) {
				if (checkIfHandleBarge(nextBargeInSchedule, time)){
					return found=true;
				}
//				Port.schedule.schedule(ScheduleParameters.createOneTime(time,
//						ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
//						nextBargeInSchedule, time);
			} else {
				int lstNextAppointment = this.appointments.get(nextBargeInSchedule)[1];
				boolean startNextBarge = false;
				for (Barge nextBarge : this.queue) {
					int expectedEndTimeThisBarge = time + nextBarge.handlingTimes.get(nextBarge.terminals.indexOf(this));
					if (expectedEndTimeThisBarge <= lstNextAppointment) {
						Port.schedule.schedule(ScheduleParameters
								.createOneTime(time,
										ScheduleParameters.LAST_PRIORITY),
								this, "handleBarge", nextBarge, time);
						startNextBarge = true;
						break;
					}
				}
				if (startNextBarge == false) {
					this.state = Terminal.IDLE;
				} else{
					return found=true;
				}
			}*/
		}
		return found;
	}

	/**
	 * [ISchedulableAction] Finish handling of a barge
	 * 
	 * @param barge
	 *            barge that finished
	 * @param time
	 *            finish time
	 */
	public void finishHandling(Barge barge, int time) {

		// remove the barge from appointments of terminal
		this.appointments.remove(barge);
		this.numHandling--;
		
		//this.state = Terminal.IDLE;
		
		// let the barge decide what to do after it finished handling
		barge.afterFinish(time, this);
		if (Port.eventsToExcel.equals("Yes")) {
			Port.stats.addEvent(time, barge.bargeNumber,
					("Finished handling at Terminal " + this.toString()));
		}
		
		
		
		if (this.queue.size() == 0) {
			this.state = Terminal.IDLE;
		} else if (Port.terminalLogic.equals("Unreserved") ) {
			Barge nextBarge = this.queue.peek();
			Port.schedule.schedule(ScheduleParameters.createOneTime(time,
					ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
					nextBarge, time);

		} else if (Port.terminalLogic.equals("Reserved")) {
//			System.out.print("queue "+this.queue);
//			System.out.print(" appointments "+this.appointments);
//			System.out.println(this.name);

			if (!checkNextPossibleBargeToHandle(time)){
				this.state = Terminal.IDLE;
			}

			/*Barge nextBargeInSchedule = (Barge) this.appointments.keySet().toArray()[0];
			if (this.queue.contains(nextBargeInSchedule) == true) {
				Port.schedule.schedule(ScheduleParameters.createOneTime(time,
						ScheduleParameters.LAST_PRIORITY), this, "handleBarge",
						nextBargeInSchedule, time);
			} else {
				int lstNextAppointment = this.appointments
						.get(nextBargeInSchedule)[1];
				boolean startNextBarge = false;
				for (Barge nextBarge : this.queue) {
					//System.out.print(nextBarge);
					//System.out.print(nextBarge.terminals);
					//System.out.print(" handeling times"+ nextBarge.handlingTimes);
					//System.out.println(" "+this.name);
					int expectedEndTimeThisBarge = time + nextBarge.handlingTimes.get(nextBarge.terminals.indexOf(this));
					if (expectedEndTimeThisBarge <= lstNextAppointment) {
						Port.schedule.schedule(ScheduleParameters
								.createOneTime(time,
										ScheduleParameters.LAST_PRIORITY),
								this, "handleBarge", nextBarge, time);
						startNextBarge = true;
						break;
					}
				}
				if (startNextBarge == false) {
					this.state = Terminal.IDLE;
				}
			}*/
		}



	}

	/**
	 * used to display the queue size in the chart in the GUI
	 */
	public int getQueueSize() {
		return this.queue.size();
	}

	/**
	 * used to display the number of busy terminals in the GUI
	 */
	public int getNumhandling() {
		return this.numHandling;
	}

	/**
	 * Start handling
	 * 
	 * @param barge
	 * @param currentTime
	 */
	public void handleBarge(Barge barge, int currentTime) {
		// start handling
		// set the states of the barge and terminal to handling
		// remove the barge from the queue
		this.queue.remove(barge);
		//this.appointments.remove(barge);
		barge.state = Barge.HANDLING;
		this.state = Terminal.HANDLING;
		this.numHandling++;

		if (Port.eventsToExcel.equals("Yes")) {
			Port.stats.addEvent(
					currentTime,
					barge.bargeNumber,
					("Started handling at Terminal " + this.toString()
							+ ". Expected - actual = "
							+ barge.appointments.get(this)[1] + " - "
							+ currentTime + " = " + (barge.appointments
							.get(this)[1] - currentTime)));
		}

		

		// get handling time
		int handlingTime = barge.handlingTimes.get(barge.terminals
				.indexOf(this));
		if (Port.model.equals("Stochastic")) {
			handlingTime = (int) Math.round(Port.timeRNG.nextGaussian(
					handlingTime, Port.timeSigma));
			if (handlingTime < 10) {
				handlingTime = 10;
			}
		}

		// add handling time to total (actual) handling time statistic of the
		// barge
		barge.totalHandlingTime += handlingTime;

		// schedule finishHandling. at start time handling + handling time
		int finishTime = currentTime + handlingTime;
		Port.schedule.schedule(ScheduleParameters.createOneTime(finishTime,
				ScheduleParameters.FIRST_PRIORITY), this, "finishHandling",
				barge, finishTime);
	}

	/**
	 * @return A String representation of the appointments this terminal has
	 *         with barges
	 */
	public String appointmentsToString() {
		String str = "Schedule of terminal " + this.name + "\n";
		for (Barge b : this.appointments.keySet()) {
			str += "Barge " + b.bargeNumber + " ";
			for (int i : this.appointments.get(b)) {
				str += i + " ";
			}
			str += "\n";
		}
		return str + "\n";
	}

	/**
	 * @param time
	 * @return A String representation of the queue at this terminal
	 */
	public String queueToString(int time) {
		String str = "Queue of terminal " + this.name + "at time " + time
				+ "\n";
		for (Barge b : this.queue) {
			str += "Barge " + b.bargeNumber + "\n";
		}
		return str;
	}

	public String toString() {
		return name;
	}
}
