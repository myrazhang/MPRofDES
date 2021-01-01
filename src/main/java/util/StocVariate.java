package util;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.*;

public abstract class StocVariate {


    AbstractRealDistribution dist;
    RandomGenerator g;
    String name;

    public StocVariate(){}

    public double getOneSample(PrintWriter rvResult){
        double a = dist.sample();

        rvResult.println(name+" "+a);

        return a;
    }

    public double getMean(){
        return dist.getNumericalMean();
    }

    public abstract void resetRandomNumberStream();
    public abstract void setRandomNumberStream(long newSeed);
}

