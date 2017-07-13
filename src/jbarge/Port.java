package jbarge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Precision;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.ContextUtils;

public class Port implements ContextBuilder<Object> {

	/**
	* The Repast ISchedule manages the execution of events according to the simulation clock.
	*/
	public static ISchedule schedule;

	/**
	* The sailing times in a (Guava) Table object where the keys are the Terminals and the value is the sailing time.
	*/
	public static Table<Terminal,Terminal,Integer> sailingtimesTable;

	/**
	* The Terminal objects are stored in this ArrayList.
	*/
	 public static ArrayList<Terminal> terminals;

	/**
	* The String parameters.Model stands for stochastic or deterministic sailing and handling times.
	*/
	public static String sheetName, model, terminalLogic, slackMethod, eventsToExcel, bargesDetailsToExcel;

	/**
	* The RandomDataGenerators.
	*/
	public static RandomDataGenerator arrivalRNG, numTerminalRNG, handlingTimeRNG, timeRNG;

	/**
	* The random data seed parameter.
	*/
	public static long seed;

	 /**
	 * The Integer parameters.
	 */
	 public static int timeSigma, slack, slackDenominator, numNodes;

	 /**
	 * The Double parameters.
	 */
	 public static double endTime, warmup, arrivalRate, numTerminalMean, numTerminalStd, handlingTimeMean, handlingTimeStd;

	 /**
	 * The Statistics object that manages the statistics.
	 */
	 public static Statistics stats;

	 /**
	 * Keeps track of the scenarios. Especially useful when running simulations in batches. We set this to 0,
	 * and plus 1 in the build method. When a new context is build, the scenario number will be updated.
	 */
	 public static int scenarioCount=0;

	 /**
	 * Used to randomly select terminals to visit. This is used in the arriveAtPort() method.
	 */
	 ArrayList<Integer> terminalList;
	/**
	 * This method build the Context.
	 * The Context is the core concept and object in Repast Simphony.
	 * It provides a data structure to organize agents.
	 */
	 @Override
	 public Context<Object> build(Context<Object> context) {

	 schedule = RunEnvironment.getInstance().getCurrentSchedule();

	 //read parameters, setup random data generators, construct terminal agents, and read sailing times table.
	 this.readParameters();
	 this.setupRandomgenerators();
	 this.createTerminals();
	 this.sailingtimesTable();

	 //add terminal agents
	 context.addAll(terminals);

	 //construct and add statistics object
	 stats = new Statistics();
	 context.add(stats);

	 //plus 1 the scenario number
	 scenarioCount++;

	 //schedule the first event in the ISchedule schedule
	 schedule.schedule(ScheduleParameters.createOneTime(0), this, "initialize"); // schedule first event

	 return context;
	 }
	 /**
	 * The first event of the simulation. Schedules the first arrival.
	 */
	 public void initialize() {
	 int arrivalTime = (int)schedule.getTickCount()+(int) Precision.round(arrivalRNG.nextExponential(Port.arrivalRate),0);
	 schedule.schedule(ScheduleParameters.createOneTime(arrivalTime, 1), this, "arriveAtPort");
	 //schedule.schedule(ScheduleParameters.createOneTime(warmup, ScheduleParameters.LAST_PRIORITY), stats, "warmupReset");
	 schedule.schedule(ScheduleParameters.createOneTime(endTime, ScheduleParameters.LAST_PRIORITY), this, "end");
	 }

	 /**
	 * Reads in all the parameters and initializes the corresponding objects.
	 * Note that the parameters that require configuration from that GUI was changed,
	 * therefore there are some fixed parameters below.
	 */
	 public void readParameters(){
	 Parameters params = RunEnvironment.getInstance().getParameters();
	  sheetName = "14 terminals";
	  seed = params.getInteger("randomSeed");
	  warmup = 1440;
	  //endTime = 10000;
	  //endTime = 17280;
	  //endTime = 65118;
	  endTime=65120;
	  arrivalRate = params.getDouble("arrivalRate");
	  numTerminalMean = 5;
	  numTerminalStd = 1;
	  handlingTimeMean = 30;
	  handlingTimeStd = 10;
	  eventsToExcel = params.getString("eventsToExcel");
	  bargesDetailsToExcel = params.getString("bargesDetailsToExcel");
	  model = params.getString("model");
	  timeSigma = 3;
	  terminalLogic = params.getString("terminalLogic");
	  slack = params.getInteger("slack");
	  slackMethod = params.getString("slackMethod");
	  slackDenominator = params.getInteger("slackDenominator");
	 }

	 /**
	 * Setup the random data generators.
	 */
	 public void setupRandomgenerators(){
	 arrivalRNG = new RandomDataGenerator();
	 numTerminalRNG = new RandomDataGenerator();
	 handlingTimeRNG = new RandomDataGenerator();
	 timeRNG = new RandomDataGenerator();
	 arrivalRNG.reSeed(1206271786);
	 numTerminalRNG.reSeed(1209361928);
	 handlingTimeRNG.reSeed(1209361923);
	 timeRNG.reSeed(1209351537);
	 }



	 /**
	 * Create terminal object/agents.
	 * The number of rows in sailing times is equal to the number of terminals.
	 * (Note: the port entrance is seen terminal object)
	 */
	public void createTerminals(){
		 numNodes=15;
		 terminals = new ArrayList<Terminal>();
		 for(int i=0; i<numNodes; i++){
		 terminals.add(new Terminal("t"+i));
		 }
		
		 terminalList = new ArrayList<Integer>();
		 for(int i = 1; i<terminals.size(); i++){ //exclude i = 0 (= port entrance)
		 terminalList.add(i);
		 }
		 }
		
		
		 /**
		 * This method creates all parameters to construct a barge. After that the barge is constructed.
		 * It also schedules the arrival of the next barge.
		 */
		 public void arriveAtPort(){
		
		 // update statistic
		 Port.stats.bargesEnteredPort++;
		
		 // declare the input for the barge constructor
		 ArrayList<Terminal> terminalsToVisit = new ArrayList<Terminal>();
		 ArrayList<Integer> handlingTimes = new ArrayList<Integer>();
		
		 // add depot node with handling time 0
		 terminalsToVisit.add(terminals.get(0));
		 handlingTimes.add(0);
		
		
		 // number of terminals to visit, normal distribution
		 int numberToVisit = (int) Precision.round(numTerminalRNG.nextGaussian(Port.numTerminalMean, Port.numTerminalStd),0);
		
		 // max 8 terminals to visit
		 if(numberToVisit>8){
		 numberToVisit=8;
		 }
		
		 //this could happen in ports with less than 8 terminals.
		 if(numberToVisit>Port.terminals.size()-1){
		 numberToVisit=Port.terminals.size()-1;
		 }
		
		 // select terminals randomly, add handling times and add to lists
		 Collections.shuffle(terminalList, new Random(1));
		 for(int i = 0; i<numberToVisit;i++){
		 int terminalNumber = terminalList.get(i);
		 Terminal terminal = terminals.get(terminalNumber);
		 Integer handlingTime = (int) Precision.round(numTerminalRNG.nextGaussian(Port.handlingTimeMean, Port.handlingTimeStd),0);
		 // the minimum handling time is 10
		 if(handlingTime<10){
		 handlingTime=10;
		 }
		 terminalsToVisit.add(terminal);
		 handlingTimes.add(handlingTime);
		 }
		
		 // arrival time of the barge in the port as an integer
		 int arrivalTime = (int) Math.round(schedule.getTickCount());
		
		 // create the new barge agent
		 Barge barge = new Barge(stats.bargeCount, arrivalTime, terminalsToVisit, handlingTimes);

		 // add to context: first get the context by using one of the terminals, then use it to add the barge
		 @SuppressWarnings("unchecked")
		 Context<Object> context = ContextUtils.getContext(terminals.get(0));
		 context.add(barge);
		
		 if(Port.eventsToExcel.equals("Yes")){
		 Port.stats.addEvent(barge.arrivalTime, barge.bargeNumber, "Arrived at Port");
		 }
		
		 //update barge count so the next barge will get a new bargeNumber.
		 stats.bargeCount++;
		
		 // if the next barge arrives before the end time, then schedule the next arrival
		 int nextArrivalTime = (int)schedule.getTickCount()+(int) Precision.round(arrivalRNG.nextExponential(Port.arrivalRate),0);
		 if (nextArrivalTime < endTime){
		 schedule.schedule(ScheduleParameters.createOneTime(nextArrivalTime, 1), this, "arriveAtPort");
		 }
		 }
		
		 /**
		 * The statistics are written to excel after which they are cleared. The simulation ends.
		 */
		 public void end(){
		 stats.toExcel();
		 Port.stats.resetStats();
		 RunEnvironment.getInstance().endRun(); // end the simulation
		 System.out.println("Scerario "+Port.scenarioCount+" completed.");
		 }
		/**
		 * Put the sailing times from the spreadsheet into a HashBasedTable. The table is used by a
		 * barge to construct a smaller sailing times array with only the relevant terminals. The barge
		 * will use the smaller version of the table as input for the TDTSP.
		 */
			 
			 public void sailingtimesTable() {
					
				 String filePath; //the file path depends on whether the simulation is part of a batch run
				 if(RunEnvironment.getInstance().isBatch()==true){
				 filePath = "C:/jbarges/data.xlsx";
				 }
				 else{
				 filePath = "src/data.xlsx";
				 }
				
				 int[][] table=null;
				 try {
				 FileInputStream file = new FileInputStream(new File(filePath));
				
				 // get the file
				 Workbook workbook = new XSSFWorkbook(file);
				
				 // get the sheet
				 Sheet sheet = workbook.getSheet(sheetName);
				
				 // create iterator
				 Iterator<Row> rowIterator = sheet.iterator();
				
				 // create an array to store the content. the array will later in this method be converted to a HashBasedTable
				 int noOfColumns = sheet.getRow(0).getPhysicalNumberOfCells();
				 table = new int[noOfColumns][noOfColumns];
				
				 // store the content in the array
				 for(int i = 0; i<noOfColumns; i++){
				 Row row = rowIterator.next();
				 Iterator<Cell> cellIterator = row.cellIterator();
				 for (int j = 0; j<noOfColumns; j++){
				 Double cell = cellIterator.next().getNumericCellValue();
				 table[i][j]= cell.intValue();
				 }
				 }
				 file.close();
				
				 } catch (FileNotFoundException e1) {
				 e1.printStackTrace();
				 } catch (IOException e) {
				 e.printStackTrace();
				 }
				
				 // convert to a HashBasedTable
				 sailingtimesTable = HashBasedTable.create();
				 for(int i=0; i<terminals.size(); i++){
				 for(int j=0; j<terminals.size();j++){
				 sailingtimesTable.put(terminals.get(i), terminals.get(j), table[i][j]);
				 }
				 }
				 }
	

}
