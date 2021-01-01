package util;

import java.io.PrintWriter;

public class TruncatedNormalRV extends NormalRV {

    public TruncatedNormalRV(long seed, double mean, double std,String name){
        super(seed, mean, std,name);
    }

    @Override
    public double getOneSample(PrintWriter rvResult){
        double s = dist.sample();
        while(s<0 || s>2*mean){s = dist.sample();}
        rvResult.println(name+" "+s);
        return s;
    }

}
