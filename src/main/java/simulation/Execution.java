package simulation;

public class Execution {
    Event event;
    int scheduleSequence;
    int executeSequence;
    double scheduleTime;
    double executiontime;
    boolean canceled = false;

    Execution(Event e, int scheduleSequence, double clockTime, double delay){
        event = e;
        this.scheduleSequence = scheduleSequence;
        this.scheduleTime = clockTime;
        this.executiontime = this.scheduleTime + delay;
    }

    public void setExecuteSequence(int executeSequence) {
        this.executeSequence = executeSequence;
    }

    @Override
    public String toString(){
        return event.toString() + scheduleSequence;
    }
}
