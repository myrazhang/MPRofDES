package simulation;

import util.StocVariate;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class EventZeroDelay extends Event{
    EventPositiveDelay ep=null;

    public EventZeroDelay(){}

    public EventZeroDelay(String n, // name 名字
                          ArrayList<ConditionToSchedule> c, //conditionsToSchedule
                          HashMap<String, Integer> s // stateChanges
    ){
        type = "zero";
        name = n;
        conditionsToSchedule = c;
        stateChanges = s;
    }

    @Override
    boolean possibleToAddNewExecute(HashMap<String, StateVariable> systemState, int nbExistExecution){
        if(nbExistExecution == 1)
            return false;
        else{
            for(ConditionToSchedule c: conditionsToSchedule){
                StateVariable sv = systemState.get(c.stateVariable.toString());
                if(sv.value < c.lowerBound || sv.value > c.upperBound){
                    return false;
                }
            }
            return true;
        }
    }

    void setCountedEvent(EventPositiveDelay ep){
        this.ep = ep;
    }

    @Override
    EventPositiveDelay getCountedEvent(){return ep;}

    @Override
    double getOneSampleOfDelay( PrintWriter rvResult){
        return 0;
    }

    @Override
    void executeTheEvent(HashMap<String, StateVariable> systemState){
        for(String k: stateChanges.keySet()){
            systemState.get(k).value += stateChanges.get(k);
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                 name +
                '}';
    }

    @Override
    boolean possibleToCancelExecute(HashMap<String, StateVariable> systemState){return false;}
}
