package simulation;

import util.ConstantRV;
import util.StocVariate;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class Event {
    String type="zero";
    int nbParallelExecutions=1;
    ArrayList<ConditionToSchedule> conditionsToSchedule;
    HashMap<String, Integer> stateChanges;
    String name;
    EventZeroDelay countEvent=null;
    StateVariable countVariable=null;
    StocVariate delay=new ConstantRV(0,"-");
    ArrayList<ConditionToSchedule> conditionsToCancel = null;

    abstract void executeTheEvent(HashMap<String, StateVariable> systemState);
    abstract EventPositiveDelay getCountedEvent();
    abstract double getOneSampleOfDelay(PrintWriter pw);
    abstract boolean possibleToCancelExecute(HashMap<String, StateVariable> systemState);
    abstract boolean possibleToAddNewExecute(HashMap<String, StateVariable> systemState, int nbExistExecution);
}
