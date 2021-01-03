package systems;

import ilog.concert.IloException;
import simulation.*;
import util.ConstantRV;
import util.ExpRV;
import util.StocVariate;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class GG1Failure  extends SimulatorWithEventCancellation{

    double at=10;
    double pt=8;
    double mttf=40;
    double mttr=8;
    StocVariate stream1= new ExpRV(1,at,"at");
    StocVariate stream2 = new ExpRV(2,pt,"pt");
    StocVariate stream3 = new ExpRV(3,mttf,"ttf");
    StocVariate stream4 = new ExpRV(4,mttr,"ttr");
    int nbPart = 5;
    int nbFail = 1;
    int maxK = 20;

    public GG1Failure () throws IloException {
        // 状态变量 state variables  ====================================================================================
        ArrayList<StateVariable> stateVariables = new ArrayList<>();
        HashMap<String, StateVariable> states = new HashMap<>();

        StateVariable ua = new StateVariable("ua", 0,0,1);
        String uaString = ua.toString();
        states.put(uaString,ua);
        stateVariables.add(ua);

        StateVariable q = new StateVariable("q", 0,0,100);
        String qString = q.toString();
        states.put(qString,q);
        stateVariables.add(q);

        StateVariable g = new StateVariable("g", 0,0,1);
        String gString = g.toString();
        states.put(gString,g);
        stateVariables.add(g);

        StateVariable h = new StateVariable("h", 0,0,1);
        String hString = h.toString();
        states.put(hString,h);
        stateVariables.add(h);

        StateVariable ufail = new StateVariable("ufail", 0,0,1);
        String ufailString = ufail.toString();
        states.put(ufailString,ufail);
        stateVariables.add(ufail);

        StateVariable urepair = new StateVariable("urepair", 0,0,1);
        String urepairString = urepair.toString();
        states.put(urepairString,urepair);
        stateVariables.add(urepair);

        // 事件 events ==================================================================================================
        ArrayList<Event> events = new ArrayList<>();
        HashMap<String, Integer> nbExecutions = new HashMap<>();

        // Count Arrival
        ArrayList<ConditionToSchedule> cca = new ArrayList<>();
        HashMap<String, Integer> sca = new HashMap<>();
        cca.add(new ConditionToSchedule(ua,0,0));
        sca.put(uaString,1);
        EventZeroDelay countArrival = new EventZeroDelay("Count arrival", cca, sca);
        events.add(countArrival);
        nbExecutions.put(countArrival.toString(),nbPart);

        // Arrival
        HashMap<String, Integer> sa = new HashMap<>();
        sa.put(qString,1);
        sa.put(uaString,-1);
        EventPositiveDelay arrival = new EventPositiveDelay("Arrival", stream1, 1, sa,countArrival,ua);
        events.add(arrival);
        nbExecutions.put(arrival.toString(),nbPart);

        // Start
        ArrayList<ConditionToSchedule> cs = new ArrayList<>();
        HashMap<String, Integer> ss = new HashMap<>();
        cs.add(new ConditionToSchedule(q,1,100));
        cs.add(new ConditionToSchedule(g,0,0));
        cs.add(new ConditionToSchedule(h,0,0));
        ss.put(gString,1);
        ss.put(qString,-1);
        EventZeroDelay start = new EventZeroDelay("Start", cs, ss);
        events.add(start);
        nbExecutions.put(start.toString(),nbPart);

        // Finish
        ArrayList<ConditionToSchedule> cancelF = new ArrayList<>();
        HashMap<String, Integer> sf = new HashMap<>();
        cancelF.add(new ConditionToSchedule(h,1,1));
        cancelF.add(new ConditionToSchedule(g,1,1));
        sf.put(gString,-1);
        EventPositiveDelay finish = new EventPositiveDelay("Finish", stream2, 1, sf,start,g,cancelF);
        events.add(finish);
        nbExecutions.put(finish.toString(),nbPart);

        // Count failure
        ArrayList<ConditionToSchedule> cCountFail = new ArrayList<>();
        HashMap<String, Integer> sCountFail = new HashMap<>();
        cCountFail.add(new ConditionToSchedule(h,0,0));
        cCountFail.add(new ConditionToSchedule(ufail,0,0));
        sCountFail.put(ufailString,1);
        EventZeroDelay countFail = new EventZeroDelay("Count fail", cCountFail, sCountFail);
        events.add(countFail);
        nbExecutions.put(countFail.toString(),nbFail);

        // Failure
        HashMap<String, Integer> sFail = new HashMap<>();
        sFail.put(hString,1);
        sFail.put(ufailString,-1);
        EventPositiveDelay fail = new EventPositiveDelay("Fail",stream3 , 1, sFail,countFail,ufail);
        events.add(fail);
        nbExecutions.put(fail.toString(),nbFail);

        // Failure 2
        /*ArrayList<ConditionToSchedule> cFail2 = new ArrayList<>();
        HashMap<String, Integer> sFail2 = new HashMap<>();
        cFail2.add(new ConditionToSchedule(h,1,1));
        cFail2.add(new ConditionToSchedule(g,1,1));
        sFail2.put(gString,-1);
        Event fail2 = new Event("Fail2", new ConstantRV(0), 1, cFail2, sFail2,null,null);
        events.add(fail2);
        nbExecutions.put(fail2.toString(),0);*/

        // Count repair
        ArrayList<ConditionToSchedule> cCountRepair = new ArrayList<>();
        HashMap<String, Integer> sCountRepair = new HashMap<>();
        cCountRepair.add(new ConditionToSchedule(h,1,1));
        cCountRepair.add(new ConditionToSchedule(urepair,0,0));
        sCountRepair.put(urepairString,1);
        EventZeroDelay countRepair = new EventZeroDelay("Count repair", cCountRepair, sCountRepair);
        events.add(countRepair);
        nbExecutions.put(countRepair.toString(),nbFail);

        // Repair
        HashMap<String, Integer> sRepair = new HashMap<>();
        sRepair.put(hString,-1);
        sRepair.put(urepairString,-1);
        EventPositiveDelay repair = new EventPositiveDelay("Repair", stream4, 1, sRepair,countRepair,urepair);
        events.add(repair);
        nbExecutions.put(repair.toString(),nbFail);

        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"simlog_GG1Fail.txt";
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
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GG1Fail_validation.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter valFile = new PrintWriter(logStream, true);

        for(int i =0;i<100;i++){
            String inputDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GG1Fail_"+(i+1)+"_delays.txt";
            String desDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GG1Fail_"+(i+1)+"_DesResults.txt";
            String mprDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GG1Fail_"+(i+1)+"_MprResults.txt";
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


            System.out.println("=======================  i = "+i+" =======================================================");
            GG1Failure system = new GG1Failure();
            system.resetSeeds(4*i,4*i+1,4*i+2,4*i+3);
            while(!system.validateMpr(inputFile, desFile, mprFile)){
                system.MY_INFTY += 100;
            }
            if(system.MY_INFTY>=500){
                valFile.println("Validation does not succeed with instance "+i+"!");
                return;
            }
        }
        valFile.println("All instances are validated!");


        // *************************************************** Debug ************************************************ //

        /*GG1Failure system = new GG1Failure();
        int i=60;
        system.resetSeeds(4*i,4*i+1,4*i+2,4*i+3);
        String programPath = System.getProperty("user.dir");
        String rvDirectory = programPath + File.separator+"OUTPUT"+File.separator+"GG1fail_random_variates.txt";
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
        String milpDirectory = programPath + File.separator+"OUTPUT"+File.separator+"MilpSol_GG1Fail.txt";
        OutputStream milpStream = null;
        try {
            milpStream  = new FileOutputStream(milpDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter milpResult = new PrintWriter(milpStream, true);
        system.MY_INFTY = 200;
        system.buildMilpModel();
        //system.validateMPRFeasibility();


        boolean b = system.solveMilpModel();
        if(b){
            system.printMilpResults(milpResult);
        }
        else
            system.printMilpModel(milpResult);*/
        // *************************************************** End Debug ************************************************ //

    }

    public void resetSeeds(long s1,long s2,long s3,long s4){
        stream1.setRandomNumberStream(s1);
        stream2.setRandomNumberStream(s2);
        stream3.setRandomNumberStream(s3);
        stream4.setRandomNumberStream(s4);
    }

}
