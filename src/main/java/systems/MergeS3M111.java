package systems;

import ilog.concert.IloException;
import simulation.*;
import util.ConstantRV;
import util.ExpRV;
import util.StocVariate;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MergeS3M111 extends SimulatorWithoutCancellation {

    int nbPartm1 = 2;
    int nbPartm2 = 2;
    double pt1 =8;
    double pt2 =10;
    double pt3 = 5;
    int buffer = 1;
    int maxK = 18;

    StocVariate stream1 =  new ExpRV(1,pt1,"pt1");
    StocVariate stream2 = new ExpRV(2,pt2,"pt2");
    StocVariate stream3 = new ExpRV(3,pt3,"pt3");

    public MergeS3M111() throws IloException {
        // 状态变量 state variables  ====================================================================================
        ArrayList<StateVariable> stateVariables = new ArrayList<>();
        HashMap<String, StateVariable> states = new HashMap<>();

        StateVariable g1 = new StateVariable("g1", 0,0,1);
        String g1String = g1.toString();
        states.put(g1String,g1);
        stateVariables.add(g1);

        StateVariable g2 = new StateVariable("g2", 0,0,1);
        String g2String = g2.toString();
        states.put(g2String,g2);
        stateVariables.add(g2);

        StateVariable g3 = new StateVariable("g3", 0,0,1);
        String g3String = g3.toString();
        states.put(g3String,g3);
        stateVariables.add(g3);

        StateVariable b1 = new StateVariable("b1", 0,0,1);
        String b1String = b1.toString();
        states.put(b1String,b1);
        stateVariables.add(b1);

        StateVariable b2 = new StateVariable("b2", 0,0,1);
        String b2String = b2.toString();
        states.put(b2String,b2);
        stateVariables.add(b2);

        StateVariable q = new StateVariable("q", 0,0, buffer);
        String qString = q.toString();
        states.put(qString,q);
        stateVariables.add(q);

        // 事件 events ==================================================================================================
        ArrayList<Event> events = new ArrayList<>();
        HashMap<String, Integer> nbExecutions = new HashMap<>();

        // Start m1
        ArrayList<ConditionToSchedule> cs1 = new ArrayList<>();
        HashMap<String, Integer> ss1 = new HashMap<>();
        cs1.add(new ConditionToSchedule(g1,0,0));
        cs1.add(new ConditionToSchedule(b1,0,0));
        ss1.put(g1String,1);
        EventZeroDelay startm1 = new EventZeroDelay("Start  m1",  cs1, ss1);
        events.add(startm1);
        nbExecutions.put(startm1.toString(),nbPartm1);

        // Finish m1
        HashMap<String, Integer> sf1 = new HashMap<>();
        sf1.put(g1String,-1);
        sf1.put(b1String,1);
        EventPositiveDelay finishm1 = new EventPositiveDelay("Finish m1",stream1, 1, sf1,startm1,g1);
        events.add(finishm1);
        nbExecutions.put(finishm1.toString(),nbPartm1);

        // depart m1
        ArrayList<ConditionToSchedule> cd1 = new ArrayList<>();
        HashMap<String, Integer> sd1 = new HashMap<>();
        cd1.add(new ConditionToSchedule(b1,1,1));
        cd1.add(new ConditionToSchedule(q,0,buffer-1));
        sd1.put(b1String,-1);
        sd1.put(qString,1);
        EventZeroDelay departm1 = new EventZeroDelay("Depart m1",  cd1, sd1);
        events.add(departm1);
        nbExecutions.put(departm1.toString(),nbPartm1);

        // start m2
        ArrayList<ConditionToSchedule> cs2 = new ArrayList<>();
        HashMap<String, Integer> ss2 = new HashMap<>();
        cs2.add(new ConditionToSchedule(g2,0,0));
        cs2.add(new ConditionToSchedule(b2,0,0));
        ss2.put(g2String,1);
        EventZeroDelay startm2 = new EventZeroDelay("Start  m2",  cs2, ss2);
        events.add(startm2);
        nbExecutions.put(startm2.toString(),nbPartm2);

        // finish m2
        HashMap<String, Integer> sf2 = new HashMap<>();
        sf2.put(g2String,-1);
        sf2.put(b2String,1);
        EventPositiveDelay finishm2 = new EventPositiveDelay("Finish m2", stream2, 1,sf2,startm2,g2);
        events.add(finishm2);
        nbExecutions.put(finishm2.toString(),nbPartm2);

        // depart m2
        ArrayList<ConditionToSchedule> cd2 = new ArrayList<>();
        HashMap<String, Integer> sd2 = new HashMap<>();
        cd2.add(new ConditionToSchedule(b2,1,1));
        cd2.add(new ConditionToSchedule(q,0,buffer-1));
        cd2.add(new ConditionToSchedule(b1,0,0));
        sd2.put(b2String,-1);
        sd2.put(qString,1);
        EventZeroDelay departm2 = new EventZeroDelay("Depart m2", cd2, sd2);
        events.add(departm2);
        nbExecutions.put(departm2.toString(),nbPartm2);

        // start m3
        ArrayList<ConditionToSchedule> cs3 = new ArrayList<>();
        HashMap<String, Integer> ss3 = new HashMap<>();
        cs3.add(new ConditionToSchedule(g3,0,0));
        cs3.add(new ConditionToSchedule(q,1,buffer));
        ss3.put(g3String,1);
        ss3.put(qString,-1);
        EventZeroDelay startm3 = new EventZeroDelay("Start  m3",cs3,ss3);
        events.add(startm3);
        nbExecutions.put(startm3.toString(),nbPartm1+nbPartm2);

        // depart m3
        HashMap<String, Integer> sd3 = new HashMap<>();
        sd3.put(g3String,-1);
        EventPositiveDelay departm3 = new EventPositiveDelay("depart m3", stream3,1,sd3,startm3,g3);
        events.add(departm3);
        nbExecutions.put(departm3.toString(),nbPartm1+nbPartm2);

        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"log_mergem3.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter log = new PrintWriter(logStream, true);

        // 初始化系统 ===================================================================================================
        initializeSimulator(states, nbExecutions, events, stateVariables, maxK, log);
    }

    public static void main(String argv[]) throws Exception{

        // *************************************************** Validation ************************************************ //
        // output 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"merge_validation.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter valFile = new PrintWriter(logStream, true);


        for(int i =0;i<100;i++){
            System.out.println("===================================== i = "+i+" =======================================");
            MergeS3M111 system = new MergeS3M111();
            system.resetSeeds(4*i,4*i+1,4*i+2);
            while(!system.validateMpr()){
                system.MY_INFTY += 100;
            }
            if(system.MY_INFTY>=500){
                valFile.println("Validation does not succeed with instance "+i+"!");
                return;
            }
        }
        valFile.println("All instances are validated!");

        /*MergeS3M111 system = new MergeS3M111();
        programPath = System.getProperty("user.dir");
        String rvDirectory = programPath + File.separator+"OUTPUT"+File.separator+"merge_random_variates.txt";
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
        String milpDirectory = programPath + File.separator+"OUTPUT"+File.separator+"MilpSol_mergeS3M111.txt";
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
            system.printMilpResults(milpResult);*/
    }
    public void resetSeeds(long s1,long s2,long s3){
        stream1.setRandomNumberStream(s1);
        stream2.setRandomNumberStream(s2);
        stream3.setRandomNumberStream(s3);
    }

}
