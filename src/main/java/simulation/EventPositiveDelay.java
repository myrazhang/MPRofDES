package simulation;

import util.StocVariate;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class EventPositiveDelay extends Event {

    public EventPositiveDelay(){}

    public EventPositiveDelay(String n, // name 名字
                              StocVariate d, // delay time distribution
                              int r, // nbParallelExecutions
                              HashMap<String, Integer> s, // stateChanges
                              EventZeroDelay e, // counting event
                              StateVariable v // counting variable
    ){
        type = "positive";
        name = n;
        delay = d;
        nbParallelExecutions = r;
        stateChanges = s;
        countEvent = e;
        countVariable = v;

        if(delay.getMean()>0 && countEvent == null){
            throw new IllegalStateException("Event "+ n +" needs a counting event!");
        }

        countEvent.setCountedEvent(this);
    }


    // ******************************  Event cancellation  ***************************************************** //
    public EventPositiveDelay(String n, // name 名字
                              StocVariate d, // delay time distribution
                              int r, // nbParallelExecutions
                              HashMap<String, Integer> s, // stateChanges
                              EventZeroDelay e, // counting event
                              StateVariable v, // counting variable
                              ArrayList<ConditionToSchedule> cc //conditionsToCancel
    ){
        type = "positive";
        name = n;
        delay = d;
        nbParallelExecutions = r;
        stateChanges = s;
        countEvent = e;
        countVariable = v;

        if(delay.getMean()>0 && countEvent == null){
            throw new IllegalStateException("Event "+ n +" needs a counting event!");
        }
        conditionsToCancel = cc;
        countEvent.setCountedEvent(this);
    }

    boolean possibleToCancelExecute(HashMap<String, StateVariable> systemState){
        if(conditionsToCancel==null) return false;
        for(ConditionToSchedule c: conditionsToCancel){
            StateVariable sv = systemState.get(c.stateVariable.toString());
            if(sv.value < c.lowerBound || sv.value > c.upperBound){
                return false;
            }
        }
        return true;
    }
    // ******************************  End Event cancellation  ************************************************** //

    /*boolean possibleToAddNewExecute(HashMap<String, StateVariable> systemState, int nbExistExecution){
        if(nbExistExecution == nbParallelExecutions)
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
    }*/

    @ Override
    double getOneSampleOfDelay( PrintWriter rvResult){
        return delay.getOneSample(rvResult);
    }

    @Override
    void executeTheEvent(HashMap<String, StateVariable> systemState){
        for(String k: stateChanges.keySet()){
            systemState.get(k).value += stateChanges.get(k);
        }
    }

    @Override
    EventPositiveDelay getCountedEvent(){return null;}

    @Override
    public String toString() {
        return "Event{" +
                 name +
                '}';
    }

    @Override
    boolean possibleToAddNewExecute(HashMap<String, StateVariable> systemState, int nbExistExecution){return false;};
}
