package simulation;

public class StateVariable {
    String name;
    int value;
    int ub;
    int lb;

    public StateVariable(String name, int initialValue, int lb,int ub){
        this.name  = name;
        this.value = initialValue;
        this.lb = lb;
        this.ub=ub;
    }

    @Override
    public String toString() {
        return "StateVariable{" +
                 name +
                '}';
    }

    public String printState(){
        return "{" +
                name + ": " + value +
                '}';
    }

    public StateVariable deepCopy(){
        return new StateVariable(name,value,lb,ub);
    }
}
