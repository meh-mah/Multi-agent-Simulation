package jbarge;

 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Date;

 import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
 import org.apache.commons.math3.util.Precision;
 import org.apache.poi.ss.usermodel.Row;
 import org.apache.poi.ss.usermodel.Sheet;
 import org.apache.poi.ss.usermodel.Workbook;
 import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import repast.simphony.engine.environment.RunEnvironment;

 /**
 * Manages the statistics.
 */

 public class Statistics {

 public int bargesEnteredPort, bargesLeftPort, bargesInPort, bargesInPortafterWarmup, bargeCount;


 /**
 * Construct DescriptiveStatistics from org.apache.commons.math3.
 * Array indexes: 0=expectedSojournTime, 1=actualSojournTime, 2=totalWaitingTime,
 * 3=totalHandlingTime, 4=totalSailingTime
 */
 public DescriptiveStatistics[] descriptiveStatistics;

 public ArrayList<Object[]> singleBargeinfo, events;


 public Statistics(){


 bargesEnteredPort=0;
 bargesLeftPort=0;
 bargesInPort=bargesEnteredPort-bargesLeftPort;

 bargeCount = 0;

 descriptiveStatistics = new DescriptiveStatistics[5];
 for(int i = 0; i<5; i++){
 descriptiveStatistics[i]= new DescriptiveStatistics();
 }

 singleBargeinfo = new ArrayList<Object[]>();
 events = new ArrayList<Object[]>();
 }


 public void addEvent(int time, int bargeNumber, String description){
 Object[] event = new Object[]{
 time,
 bargeNumber,
 description
 };

 this.events.add(event);
 }

 public void addSinglebargeinfo(Barge barge){
 //terminals to visit
 String terminalsToVisit="";
 for(int i=1; i<barge.terminals.size();i++){
 Terminal t = barge.terminals.get(i);
 terminalsToVisit+= t+" ";
 }

 //handling time (parallel to the terminal to visit)
 String handlingtimes="";
 for(int i=1; i<barge.handlingTimes.size();i++){
 Integer htime = barge.handlingTimes.get(i);
 handlingtimes+=htime+" ";
 }

 //waiting profiles
 String wprofiles="";
 for(int i=1; i<barge.terminals.size(); i++){
 Terminal t = barge.terminals.get(i);
 WaitingProfile wp = barge.waitingProfiles.get(t);
 wprofiles += "Waiting Profile of " + t.toString()+ "\n"+ wp.toString() + "\n";
 wprofiles += "Start invervals of " + t.toString()+ "\n" + wp.startIntervalsToString() + "\n";
 }
//sailing times
 String sailingTimes="";
 for(int[] i : barge.sailingTimes){
 for(int j : i){
 sailingTimes+=j+" ";
 }
 sailingTimes+="\n";
 }


 Object[] bargeInfo = new Object[]{
 Port.scenarioCount,
 barge.bargeNumber,
 barge.arrivalTime,
 barge.terminals.size()-1,
 terminalsToVisit,
 handlingtimes,
 barge.tdtsp.bestRouteToString(),
 //wprofiles,
 //sailingTimes,
 //barge.appointmentsToString(),
 //barge.terminalAppointments,
 barge.bestSTime,
 barge.actualSojourntime,
 barge.totalWaitingTime,
 barge.totalHandlingTime,
 barge.totalSailingTime,
 barge.differenceExpectedActual,
 barge.fraction,
 barge.informationSatisfaction,
 barge.waitingtimeSatisfaction
 };

 this.singleBargeinfo.add(bargeInfo);
 }


 public void toExcel(){
 String filePath;
 if(RunEnvironment.getInstance().isBatch()==true){
 filePath = "C:/jbarges/output.xlsx";
 }
 else{
 filePath = "output/output.xlsx";
 }


 try {

 //check if file exists, else create the file
 File f = new File(filePath);
 FileInputStream file;
 if(f.exists() && !f.isDirectory()){
 file = new FileInputStream(new File(filePath));
 }
 else{
 FileOutputStream out =
 new FileOutputStream(new File(filePath));
 Workbook workbook = new XSSFWorkbook();

 //create sheet with average statistics
 Sheet sheet = workbook.createSheet("Simulations");
 //add headings in an array
 Object[] heading = new Object[]{
 "Run date",
 "Random seed",
 "Terminal logic",
 "Actual sailing and handling times",
 //"Run time",
 //"Warm-up time",
 //"Time deviation (in case of stochastic)",
 "Slack method",
 "Slack constant",
 "Slack denominator",
 //"Mean terminals to visit",
 //"SD terminals to visit",
 //"Mean handling time",
 //"SD handling time",
 "Arrival rate",
 "Barges entered the port after warm-up",
 "Barges left the port after warm-up",
 "Barges in port after warm-up",
 "Mean expected sojourn time",
 "SD expected sojourn time",
 "Min expected sojourn time",
 "Max expected sojourn time",
 "Mean actual sojourn time",
 "SD actual sojourn time",
 "Min actual sojourn time",
"Max actual sojourn time",
 "Mean waiting time",
 "SD waiting time",
 "Min waiting time",
 "Max waiting time",
 "Mean handling time",
 "SD handling time",
 "Min handling time",
 "Max handling time",
 "Mean sailing time",
 "SD sailing time",
 "Min sailing time",
 "Max sailing time",

 "# Completely dissatisfied with info",
 "# Mostly dissatisfied with info",
 "# Somewhat dissatisfied with info",
 "# Neither satisfied or dissatisfied with info",
 "# Somewhat satisfied with info",
 "# Mostly satisfied with info",
 "# Completely satisfied with info",

 "# Completely dissatisfied with waiting time",
 "# Mostly dissatisfied with waiting time",
 "# Somewhat dissatisfied with waiting time",
 "# Neither satisfied or dissatisfied with waiting time",
 "# Somewhat satisfied with waiting time",
 "# Mostly satisfied with waiting time",
 "# Completely satisfied with waiting time"
 };
 //add the heading to the sheet
 Row row = sheet.createRow(sheet.getLastRowNum());
 int cellIndex=0;
 for(Object cellContent : heading){
 row.createCell(cellIndex).setCellValue(cellContent.toString());
 cellIndex++;
 }

 //create the sheet with the single barge information
 Sheet sheet2 = workbook.createSheet("Barge info");
 //add headings
 Object[] heading2 = new Object[]{
 "Scenario",
 "Barge number",
 "Arrival time",
 "Number of terminals to visit",
 "Terminals to visit",
 "Handling times",
 "Best route",
 //"Waiting profiles",
 //"Sailing times table",
 //"Appointments",
 //"Appointments of terminals",
 "Expected sojourn time",
 "Actual sojourn time",
 "Total waiting time",
 "Total handling time",
 "Total sailing time",
 "Difference between expected and actual sojourntime",
 "Fraction waiting time with respect to handling time",
 "Information provision satisfaction",
 "Waiting time satisfaction"
 };
 Row row2 = sheet2.createRow(sheet.getLastRowNum());
 int cellIndex2=0;
 for(Object cellContent : heading2){
 row2.createCell(cellIndex2).setCellValue(cellContent.toString());
 cellIndex2++;
 }


 //create the sheet with the events
 Sheet sheet3 = workbook.createSheet("Events");

 //add headings
 Object[] heading3 = new Object[]{
 "Time",
 "Barge",
 "Event",
 };
 Row row3 = sheet3.createRow(sheet.getLastRowNum());
 int cellIndex3=0;
 for(Object cellContent : heading3){
 row3.createCell(cellIndex3).setCellValue(cellContent.toString());
 cellIndex3++;
 }

 //save the file
 workbook.write(out);
 out.close();
 file = new FileInputStream(new File(filePath));
 }
Workbook workbook = new XSSFWorkbook(file);
 Sheet sheet = workbook.getSheetAt(0);


 //count satisfaction levels. satisfaction rating 1 is located at index 0, rating 2 is located at index 1, etc.
 int infoSatisfaction[] = new int[]{0,0,0,0,0,0,0};
 int waitSatisfaction[] = new int[]{0,0,0,0,0,0,0};
 for(Object[] o : this.singleBargeinfo){
 //the information satisfaction rating is located at index 14, waiting time satisfaction rating at index 15
 int info = (Integer) o[14];
 int wait = (Integer) o[15];
 infoSatisfaction[info-1]++;
 waitSatisfaction[wait-1]++;
 }

 // this array is used to fill the row
 Object[] rowContent = new Object[]{
 new Date(System.currentTimeMillis()),
 Port.seed,
 Port.terminalLogic,
 Port.model,
 //Port.endTime,
 //Port.warmup,
 //Port.timeSigma,
 Port.slackMethod,

 Port.slack,

 Port.slackDenominator,

 //Port.numTerminalMean,
 //Port.numTerminalStd,
 //Port.handlingTimeMean,
 //Port.handlingTimeStd,
 Port.arrivalRate,

 this.bargesEnteredPort,
 this.bargesLeftPort,
 this.bargesInPortafterWarmup,

 Precision.round(this.descriptiveStatistics[0].getMean(),0),
 Precision.round(this.descriptiveStatistics[0].getStandardDeviation(),0),
 Precision.round(this.descriptiveStatistics[0].getMin(),0),
 Precision.round(this.descriptiveStatistics[0].getMax(),0),

 Precision.round(this.descriptiveStatistics[1].getMean(),0),
 Precision.round(this.descriptiveStatistics[1].getStandardDeviation(),0),
 Precision.round(this.descriptiveStatistics[1].getMin(),0),
 Precision.round(this.descriptiveStatistics[1].getMax(),0),

 Precision.round(this.descriptiveStatistics[2].getMean(),0),
 Precision.round(this.descriptiveStatistics[2].getStandardDeviation(),0),
 Precision.round(this.descriptiveStatistics[2].getMin(),0),
 Precision.round(this.descriptiveStatistics[2].getMax(),0),

 Precision.round(this.descriptiveStatistics[3].getMean(),0),
 Precision.round(this.descriptiveStatistics[3].getStandardDeviation(),0),
 Precision.round(this.descriptiveStatistics[3].getMin(),0),
 Precision.round(this.descriptiveStatistics[3].getMax(),0),

 Precision.round(this.descriptiveStatistics[4].getMean(),0),
 Precision.round(this.descriptiveStatistics[4].getStandardDeviation(),0),
 Precision.round(this.descriptiveStatistics[4].getMin(),0),
 Precision.round(this.descriptiveStatistics[4].getMax(),0),

 infoSatisfaction[0],
 infoSatisfaction[1],
 infoSatisfaction[2],
 infoSatisfaction[3],
 infoSatisfaction[4],
 infoSatisfaction[5],
 infoSatisfaction[6],

 waitSatisfaction[0],
 waitSatisfaction[1],
 waitSatisfaction[2],
 waitSatisfaction[3],
 waitSatisfaction[4],
 waitSatisfaction[5],
 waitSatisfaction[6]
 };

 Row row = sheet.createRow(sheet.getLastRowNum()+1);

 int cellIndex=0;
 for(Object cellContent : rowContent){
 if(cellContent instanceof Integer){
 row.createCell(cellIndex).setCellValue((Integer) cellContent);
 }
 else if(cellContent instanceof Double){
 row.createCell(cellIndex).setCellValue((Double) cellContent);
}
 else if(cellContent instanceof String){
 row.createCell(cellIndex).setCellValue(cellContent.toString());
 }
 else if(cellContent instanceof Date){
 row.createCell(cellIndex).setCellValue((Date) cellContent);
 }
 else if(cellContent instanceof Long){
 row.createCell(cellIndex).setCellValue((Long) cellContent);
 }
 cellIndex++;
 }



 //add the ArrayList<Object[]> singleBargeinfo to sheet 2 in the excel file.
 Sheet sheet2 = workbook.getSheet("Barge info");

 for(int i=0; i<singleBargeinfo.size(); i++){
 Row row2 = sheet2.createRow(sheet2.getLastRowNum()+1);
 int cellIndex2=0;
 for(Object cellContent : singleBargeinfo.get(i)){
 if(cellContent instanceof Integer){
 row2.createCell(cellIndex2).setCellValue((Integer) cellContent);
 }
 else if(cellContent instanceof Double){
 row2.createCell(cellIndex2).setCellValue((Double) cellContent);
 }
 else if(cellContent instanceof String){
 row2.createCell(cellIndex2).setCellValue(cellContent.toString());
 }
 else if(cellContent instanceof Date){
 row2.createCell(cellIndex2).setCellValue((Date) cellContent);
 }
 cellIndex2++;
 }

 }


 //add the ArrayList<Object[]> events to sheet 3 in the excel file.
 Sheet sheet3 = workbook.getSheet("Events");

 for(int i=0; i<events.size(); i++){
 Row row3 = sheet3.createRow(sheet3.getLastRowNum()+1);
 int cellIndex3=0;
 for(Object cellContent : events.get(i)){
 if(cellContent instanceof Integer){
 row3.createCell(cellIndex3).setCellValue((Integer) cellContent);
 }
 else if(cellContent instanceof Double){
 row3.createCell(cellIndex3).setCellValue((Double) cellContent);
 }
 else if(cellContent instanceof String){
 row3.createCell(cellIndex3).setCellValue(cellContent.toString());
 }
 else if(cellContent instanceof Date){
 row3.createCell(cellIndex3).setCellValue((Date) cellContent);
 }
 cellIndex3++;
 }

 }

 file.close();

 FileOutputStream outFile =new FileOutputStream(new File(filePath));
 workbook.write(outFile);
 outFile.close();

 } catch (FileNotFoundException e) {
 e.printStackTrace();
 } catch (IOException e) {
 e.printStackTrace();
 }

 }


 public void resetStats(){
 //reset stats
 this.bargesInPortafterWarmup=0;
 this.bargesEnteredPort=0;
 this.bargesLeftPort=0;
 for(int i=0; i<5;i++){
 this.descriptiveStatistics[i].clear();
 }
 this.singleBargeinfo.clear();
 this.events.clear();
 }

 public void warmupReset(){
	//register current barges in port
	 this.bargesInPortafterWarmup=this.bargesEnteredPort-this.bargesLeftPort;
	
	 //reset stats
	 this.bargesEnteredPort=0;
	 this.bargesLeftPort=0;
	 for(int i=0; i<5;i++){
	 this.descriptiveStatistics[i].clear();
	 }
	 this.singleBargeinfo.clear();
	 this.events.clear();
	 }
	 }
