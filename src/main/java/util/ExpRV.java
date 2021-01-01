package util;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class ExpRV extends StocVariate{
    double mean;
    long seed;

    public ExpRV(long seed, double mean,String name){
        this.name = name;
        this.mean = mean;
        this.seed = seed;
        RandomGenerator g = RandomGeneratorFactory.createRandomGenerator(new Random(seed));
        this.dist = new ExponentialDistribution(g, mean);
    }

    @Override
    public double getMean(){
        return mean;
    }

    @Override
    public void resetRandomNumberStream(){
        RandomGenerator g = RandomGeneratorFactory.createRandomGenerator(new Random(seed));
        this.dist = new ExponentialDistribution(g, mean);
    }

    @Override
    public void setRandomNumberStream(long newSeed) {
        seed = newSeed;
        resetRandomNumberStream();
    }
}
