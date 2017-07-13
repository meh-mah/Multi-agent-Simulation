package tdtsp;

import java.util.ArrayList; 
 /**
 * Stage of the TDTSP.
 */
 public class Stage {

 // arrival times at last node
 ArrayList<Integer> cost;

 // S and k are stored in parallel in an array
 ArrayList<String> set;

 //predecessor nodes
 ArrayList<String> pred;

 public Stage() {
 this.cost = new ArrayList<Integer>();
 this.set = new ArrayList<String>();
 this.pred = new ArrayList<String>();
 }

 public Stage(Stage stage){
 this.cost = new ArrayList<Integer>();
 this.set = new ArrayList<String>();
 this.pred = new ArrayList<String>();


 for(int c : stage.cost){
 this.cost.add(c);
 }

 for(String s : stage.set){
 this.set.add(s);
 }

 for(String p : stage.pred){
 this.pred.add(p);
 }
 }

 public void addCost(int cost){
 this.cost.add(cost);
 }

 public void addSet(String set){
 this.set.add(set);
 }

 public void addPred(String pred){
 this.pred.add(pred);
 }
 }

