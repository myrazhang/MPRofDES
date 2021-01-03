package systems;

import ilog.concert.IloException;
import simulation.*;
import util.ConstantRV;
import util.ExpRV;
import util.StocVariate;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class GGm extends SimulatorWithoutCancellation {
    int nbPart = 5;
    double pt =8;
    double at =5;
    StocVariate stream1 = new ExpRV(1,at,"at");
    StocVariate stream2 = new ExpRV(2,pt,"pt");
    int maxK = 20;


    public GGm(int m) throws IloException{
        // 状态变量 state variables  ====================================================================================
        ArrayList<StateVariable> stateVariables = new ArrayList<>();
        HashMap<String, StateVariable> states = new HashMap<>();

        StateVariable g = new StateVariable("g", 0,0,m);
        String gString = g.toString();
        states.put(gString,g);
        stateVariables.add(g);

        StateVariable q = new StateVariable("q", 0,0,100);
        String qString = q.toString();
        states.put(qString,q);
        stateVariables.add(q);

        StateVariable uA = new StateVariable("ua", 0,0,1);
        String uaString = uA.toString();
        states.put(uaString,uA);
        stateVariables.add(uA);

        /*StateVariable nbCa = new StateVariable("nbCa", 0,0,nbPart);
        String nbCaString = nbCa.toString();
        states.put(nbCaString,nbCa);
        stateVariables.add(nbCa);*/


        /*StateVariable nbS = new StateVariable("nbS", 0,0,nbPart);
        String nbsString = nbS.toString();
        states.put(nbsString,nbS);
        stateVariables.add(nbS);*/


        // 事件 events ==================================================================================================
        ArrayList<Event> events = new ArrayList<>();
        //ArrayList<EventZeroDelay> zeroEvents = new ArrayList<>();
        //ArrayList<EventPositiveDelay> positiveEvents = new ArrayList<>();
        HashMap<String, Integer> nbExecutions = new HashMap<>();

        // Count Arrival
        ArrayList<ConditionToSchedule> cca = new ArrayList<>();
        HashMap<String, Integer> sca = new HashMap<>();
        cca.add(new ConditionToSchedule(uA,0,0));
        //cca.add(new ConditionToSchedule(nbCaString,0,nbPart-1));
        sca.put(uaString,1);
        //sca.put(nbCaString,1);
        //sca.put(nbaString,1);
        EventZeroDelay countArrival = new EventZeroDelay("Count arrival", cca, sca);
        events.add(countArrival);
        nbExecutions.put(countArrival.toString(),nbPart);

        // Arrival
        HashMap<String, Integer> sa = new HashMap<>();
        //ca.add(new ConditionToSchedule(nbCaString,0,nbPart-1));
        sa.put(qString,1);
        sa.put(uaString,-1);
        EventPositiveDelay arrival = new EventPositiveDelay("Arrival", stream1, 1, sa, countArrival, uA);
        events.add(arrival);
        nbExecutions.put(arrival.toString(),nbPart);

        // Start
        ArrayList<ConditionToSchedule> cs = new ArrayList<>();
        HashMap<String, Integer> ss = new HashMap<>();
        cs.add(new ConditionToSchedule(q,1,100));
        cs.add(new ConditionToSchedule(g,0,m-1));
        //cs.add(new ConditionToSchedule(nbsString,0,nbPart-1));
        ss.put(gString,1);
        ss.put(qString,-1);
        //ss.put(nbsString,1);
        //ss.put(nbfString,1);
        EventZeroDelay start = new EventZeroDelay("Start",  cs, ss);
        events.add(start);
        nbExecutions.put(start.toString(),nbPart);

        //finish
        HashMap<String, Integer> sf = new HashMap<>();
        //cf.add(new ConditionToSchedule(nbsString,0,nbPart-1));
        sf.put(gString,-1);
        EventPositiveDelay finish = new EventPositiveDelay("Finish", stream2, m, sf,start,g);
        events.add(finish);
        nbExecutions.put(finish.toString(),nbPart);

        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"simlog_GGm.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter log = new PrintWriter(logStream, true);

        // 初始化系统 ===================================================================================================
        initializeSimulator(states, nbExecutions, events, stateVariables, maxK,log);
    }

    public static void main(String argv[]) throws Exception{
        // *************************************************** Validation ************************************************ //
        // output 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GGm_validation.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter valFile = new PrintWriter(logStream, true);

        for(int i = 0;i<100;i++){
            String inputDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GGm_"+(i+1)+"_delays.txt";
            String desDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GGm_"+(i+1)+"_DesResults.txt";
            String mprDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GGm_"+(i+1)+"_MprResults.txt";
            OutputStream inputStream = null, desStream=null, mprStream = null;
            try {
                inputStream  = new FileOutputStream(inputDirectory);
                desStream = new FileOutputStream(desDirectory);
                mprStream = new FileOutputStream(mprDirectory);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            PrintWriter inputFile = new PrintWriter(inputStream, true);
            PrintWriter desFile = new PrintWriter(desStream, true);
            PrintWriter mprFile = new PrintWriter(mprStream, true);

            System.out.println("=======================  i = "+i+"  =======================================================");
            GGm system = new GGm(2);
            system.resetSeeds(4*i,4*i+1);
            while(!system.validateMpr(inputFile,desFile,mprFile)){
                system.MY_INFTY += 100;
            }
            if(system.MY_INFTY>=500){
                valFile.println("Validation does not succeed with instance "+i+"!");
                return;
            }
        }
        valFile.println("All instances are validated!");


        /*GGm system = new GGm(2);

        int i=0;
        system.resetSeeds(4*i,4*i+1);

        String rvDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GGm_random_variates.txt";
        OutputStream rvStream = null;
        try {
            rvStream  = new FileOutputStream(rvDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter rvResult = new PrintWriter(rvStream, true);

        system.simulate(true,rvResult);
        system.printLog();

        system.resetRandomStream();
        String milpDirectory = programPath + File.separator+"OUTPUT"+File.separator+"MilpSol_GGm.txt";
        OutputStream milpStream = null;
        try {
            milpStream  = new FileOutputStream(milpDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter milpResult = new PrintWriter(milpStream, true);
        system.MY_INFTY = 100;
        system.buildMilpModel();

        boolean b = system.solveMilpModel();
        if(b)
            system.printMilpResults(milpResult);
        else
            system.printMilpModel(milpResult);*/
    }

    public void resetSeeds(long s1,long s2){
        stream1.setRandomNumberStream(s1);
        stream2.setRandomNumberStream(s2);
    }

}
