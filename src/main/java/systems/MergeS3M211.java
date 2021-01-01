package systems;

import ilog.concert.IloException;
import simulation.*;
import util.ConstantRV;
import util.ExpRV;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MergeS3M211 extends SimulatorWithoutCancellation {

    int nbPartm1 = 20;
    int nbPartm2 = 20;
    //long seed1 =1;
    //long seed2 =2;
    //long seed3 = 3;
    double pt1 =20;
    double pt2 = 8;
    double pt3 = 4;
    int buffer = 8;
    int nbM1 =3;
    int nbM2 =1;
    int nbM3 =1;

    public MergeS3M211(int nbPartm1,
                        int nbPartm2,
                       int buffer,
                        double pt1,
                        double pt2,
                        long seed,
                        PrintWriter log) throws IloException {

        /*
        todo: 新的定义系统的方法
        this.nbPartm1 = nbPartm1;
        this.nbPartm2 = nbPartm2;
        this.buffer = buffer;
        this.pt1 = pt1;
        this.pt2 = pt2;
        // 状态变量 state variables  ====================================================================================
        ArrayList<StateVariable> stateVariables = new ArrayList<>();
        HashMap<String, StateVariable> states = new HashMap<>();

        StateVariable e1 = new StateVariable("e1", nbM1,0,nbM1);
        String e1String = e1.toString();
        states.put(e1String,e1);
        stateVariables.add(e1);

        StateVariable f1 = new StateVariable("f1", 0,0,nbM1);
        String f1String = f1.toString();
        states.put(f1String,f1);
        stateVariables.add(f1);

        StateVariable e2 = new StateVariable("e2", nbM2,0,nbM2);
        String e2String = e2.toString();
        states.put(e2String,e2);
        stateVariables.add(e2);

        StateVariable f2 = new StateVariable("f2", 0,0,nbM2);
        String f2String = f2.toString();
        states.put(f2String,f2);
        stateVariables.add(f2);

        StateVariable e3 = new StateVariable("e3", nbM3,0,nbM3);
        String e3String = e3.toString();
        states.put(e3String,e3);
        stateVariables.add(e3);

        StateVariable q = new StateVariable("q", buffer,0, buffer);
        String qString = q.toString();
        states.put(qString,q);
        stateVariables.add(q);

        // 事件 events ==================================================================================================
        ArrayList< Event > events = new ArrayList<>();
        HashMap<String, Integer> nbExecutions = new HashMap<>();

        // Start m1
        ArrayList<ConditionToSchedule> cs1 = new ArrayList<>();
        HashMap<String, Integer> ss1 = new HashMap<>();
        cs1.add(new ConditionToSchedule(e1String,1,nbM1));
        ss1.put(e1String,-1);
        Event startm1 = new Event("Start  m1", new ConstantRV(0), 1, cs1, ss1);
        events.add(startm1);
        nbExecutions.put(startm1.toString(),nbPartm1);

        // Finish m1
        ArrayList<ConditionToSchedule> cf1 = new ArrayList<>();
        HashMap<String, Integer> sf1 = new HashMap<>();
        cf1.add(new ConditionToSchedule(e1String,1,nbM1));
        sf1.put(f1String,1);
        Event finishm1 = new Event("Finish m1", new ExpRV(seed+1,pt1), nbM1, cf1, sf1);
        events.add(finishm1);
        nbExecutions.put(finishm1.toString(),nbPartm1);

        // depart m1
        ArrayList<ConditionToSchedule> cd1 = new ArrayList<>();
        HashMap<String, Integer> sd1 = new HashMap<>();
        cd1.add(new ConditionToSchedule(f1String,1,nbM1));
        cd1.add(new ConditionToSchedule(qString,1,buffer));
        sd1.put(e1String,1);
        sd1.put(f1String,-1);
        sd1.put(qString,-1);
        Event departm1 = new Event("Depart m1", new ConstantRV(0), 1, cd1, sd1);
        events.add(departm1);
        nbExecutions.put(departm1.toString(),nbPartm1);

        // start m2
        ArrayList<ConditionToSchedule> cs2 = new ArrayList<>();
        HashMap<String, Integer> ss2 = new HashMap<>();
        cs2.add(new ConditionToSchedule(e2String,1,nbM2));
        ss2.put(e2String,-1);
        Event startm2 = new Event("Start  m2", new ConstantRV(0), 1, cs2, ss2);
        events.add(startm2);
        nbExecutions.put(startm2.toString(),nbPartm2);

        // finish m2
        ArrayList<ConditionToSchedule> cf2 = new ArrayList<>();
        HashMap<String, Integer> sf2 = new HashMap<>();
        cf2.add(new ConditionToSchedule(e2String,1,nbM2));
        sf2.put(f2String,1);
        Event finishm2 = new Event("Finish m2", new ExpRV(seed+2,pt2), nbM2, cf2, sf2);
        events.add(finishm2);
        nbExecutions.put(finishm2.toString(),nbPartm2);

        // depart m2
        ArrayList<ConditionToSchedule> cd2 = new ArrayList<>();
        HashMap<String, Integer> sd2 = new HashMap<>();
        cd2.add(new ConditionToSchedule(f2String,1,nbM2));
        cd2.add(new ConditionToSchedule(qString,1,buffer));
        cd2.add(new ConditionToSchedule(f1String,0,0));
        sd2.put(f2String,-1);
        sd2.put(e2String,1);
        sd2.put(qString,-1);
        Event departm2 = new Event("Depart m2", new ConstantRV(0), 1, cd2, sd2);
        events.add(departm2);
        nbExecutions.put(departm2.toString(),nbPartm2);

        // start m3
        ArrayList<ConditionToSchedule> cs3 = new ArrayList<>();
        HashMap<String, Integer> ss3 = new HashMap<>();
        cs3.add(new ConditionToSchedule(e3String,1,nbM3));
        cs3.add(new ConditionToSchedule(qString,0,buffer-1));
        ss3.put(e3String,-1);
        ss3.put(qString,1);
        Event startm3 = new Event("Start  m3",new ConstantRV(0),1,cs3,ss3);
        events.add(startm3);
        nbExecutions.put(startm3.toString(),nbPartm1+nbPartm2);

        // depart m3
        ArrayList<ConditionToSchedule> cd3 = new ArrayList<>();
        HashMap<String, Integer> sd3 = new HashMap<>();
        cd3.add(new ConditionToSchedule(e3String,1,nbM3));
        cd3.add(new ConditionToSchedule(qString,0,buffer-1));
        sd3.put(e3String,1);
        Event departm3 = new Event("Depart m3", new ExpRV(seed+3,pt3),nbM3,cd3,sd3);
        events.add(departm3);
        nbExecutions.put(departm3.toString(),nbPartm1+nbPartm2);

        // 初始化系统 ===================================================================================================
        initializeSimulator(states, nbExecutions, events, stateVariables, log);*/
    }


    public static void main(String argv[]) throws Exception{

        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"log_mergeS3M211.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter result = new PrintWriter(logStream, true);
        PrintWriter log = new PrintWriter(OutputStream.nullOutputStream());


        {int nbPart = 20000;
                    MergeS3M211 system = new MergeS3M211(nbPart,nbPart,4,10,8,1,log);
                    system.simulate(false, new PrintWriter(OutputStream.nullOutputStream()));
        }

        /*String programPath = System.getProperty("user.dir");
        String milpDirectory = programPath + File.separator+"OUTPUT"+File.separator+"MilpSol_mergeS3M211.txt";
        OutputStream milpStream = null;
        try {
            milpStream  = new FileOutputStream(milpDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter milpResult = new PrintWriter(milpStream, true);
        system.buildMilpModel();

        boolean b = system.solveMilpModel();
        if(b)
            system.printMilpResults(milpResult);*/
    }


}
