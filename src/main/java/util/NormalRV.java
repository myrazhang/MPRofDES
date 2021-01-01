package util;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class NormalRV extends StocVariate {
    double mean;
    double std;
    long seed;

    public NormalRV(long seed, double mean, double std,String name){
        this.name = name;
        this.mean = mean;
        this.std = std;
        this.seed = seed;
        RandomGenerator g = RandomGeneratorFactory.createRandomGenerator(new Random(seed));
        this.dist = new NormalDistribution(g, mean, std);
    }

    @Override
    public double getMean(){
        return mean;
    }

    @Override
    public void resetRandomNumberStream(){
        RandomGenerator g = RandomGeneratorFactory.createRandomGenerator(new Random(seed));
        this.dist = new NormalDistribution(g, mean, std);
    }

    @Override
    public void setRandomNumberStream(long newSeed) {
        seed = newSeed;
        resetRandomNumberStream();
    }
}
