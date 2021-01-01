package simulation;

public class ConditionToSchedule {
    StateVariable stateVariable;
    int upperBound;
    int lowerBound;

    public ConditionToSchedule(StateVariable s, int lb, int ub){
        stateVariable =s;
        upperBound = ub;
        lowerBound = lb;
    }
}
