package util;

import java.io.PrintWriter;

public class ConstantRV extends StocVariate {
    double value;

    public ConstantRV(double v,String name){
        this.name = name;
        value = v;}

    @Override
    public double getOneSample(PrintWriter rvResult){
        rvResult.println(name + " "+value);

        return value;
    }

    @Override
    public double getMean(){
        return value;
    }

    @Override
    public void resetRandomNumberStream() {}

    @Override
    public void setRandomNumberStream(long newSeed) {}
}
