package simulation;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public abstract class Simulator {
    public double MY_INFTY = 100;

    // 必须初始化的成员
    HashMap<String, StateVariable> systemState;
    protected HashMap<String, Integer> nbExecutions;
    //ArrayList<EventZeroDelay> zeroEvents;
    //ArrayList<EventPositiveDelay> positiveEvents;
    ArrayList<Event> events = new ArrayList<>();
    ArrayList<StateVariable> stateVariables;
    HashMap<String, Integer> initialState = new HashMap<>();
    int totNbExecutions;
    PrintWriter logWriter;

    // 仿真过程中使用到的成员
    PriorityQueue<Execution> futureExecutionList = new PriorityQueue<>(new Comparator<Execution>() {
        @Override
        public int compare(Execution execution, Execution t1) {
            if (execution.executiontime < t1.executiontime) return -1;
            else if (execution.executiontime > t1.executiontime) return 1;
            return 0;
        }
    });
    HashMap<String, Integer> eventScheduleUpTo = new HashMap<>();
    HashMap<String, Integer> eventExecuteUpTo = new HashMap<>();
    HashMap<String, Integer> eventNbExistExecutions = new HashMap<>();
    boolean isTerminate = false;
    protected double clockTime = 0.0;
    int nbTotalExecutionsSimulated = 0;
    protected ArrayList<SimLog> log = new ArrayList<>();

    public Simulator() throws IloException {
    }

    public void initializeSimulator(HashMap<String, StateVariable> States, // 状态变量
                                    HashMap<String, Integer> nbExecutions, // 每个事件执行的次数 maxI^{\xi}
                                    ArrayList<Event> events, // 事件
                                    ArrayList<StateVariable> stateVariables, // 状态变量
                                    int totalNbExecutions, // 执行事件总次数 maxK
                                    PrintWriter log
    ) {

        this.systemState = States;
        this.nbExecutions = nbExecutions;
        this.events = events;
        this.stateVariables = stateVariables;
        this.totNbExecutions = totalNbExecutions;

        for (StateVariable stateVariable : stateVariables)
            this.initialState.put(stateVariable.toString(), stateVariable.value);
        this.logWriter = log;
        cleanFutureEventList();
    }


    // ************************************************* Simulate **************************************** //
    public void simulate(boolean withLog, PrintWriter rvResult) {

        // log 文件 ====================================================================================================
        /*String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator + "OUTPUT" + File.separator + "log_mergeS3M211.txt";
        OutputStream logStream = null;
        try {
            logStream = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter result = new PrintWriter(logStream, true);*/

        simulateToGetMprSolution();
        resetSimulator();
        StringBuilder s = new StringBuilder();
        for (String k : systemState.keySet())
            s.append(systemState.get(k).printState());
        if (withLog) log.add(new SimLog(0, s.toString()));
        scheduleNewEvents(withLog);
        while (!isTerminate) {
            while ( // 将某一时刻可以执行的事件全部执行
                    nbTotalExecutionsSimulated < totNbExecutions // 仿真过的事件总数量小于预设的数量
                            && futureExecutionList.size() > 0 // Future event list 不空
                            && futureExecutionList.peek().executiontime == clockTime) {
                executeNextEvent(withLog, rvResult);
                scheduleNewEvents(withLog);
            }
            if (nbTotalExecutionsSimulated < totNbExecutions
                    && futureExecutionList.size() > 0)
                clockTime = futureExecutionList.peek().executiontime; // 仿真时钟向前拨
            else
                isTerminate = true;
        }
        MY_INFTY = clockTime;
        if (!futureExecutionList.isEmpty()) {
            for (Execution exe : futureExecutionList) MY_INFTY = Math.max(MY_INFTY, exe.executiontime);
        }

    }

    void resetSimulator() {
        futureExecutionList.clear();
        eventScheduleUpTo.clear();
        eventExecuteUpTo.clear();
        isTerminate = false;
        clockTime = 0.0;
        nbTotalExecutionsSimulated = 0;
        log.clear();
        cleanFutureEventList();
        for (StateVariable s : stateVariables) s.value = initialState.get(s.toString());
    }

    private void cleanFutureEventList() {
        for (Event e : events) {
            eventScheduleUpTo.put(e.toString(), 0);
            eventExecuteUpTo.put(e.toString(), 0);
            eventNbExistExecutions.put(e.toString(), 0);
        }
    }

    void executeNextEvent(boolean withLog, PrintWriter rvResult) {
        // 执行第一个事件
        Execution ex = futureExecutionList.poll();
        Event ev = ex.event;

        eventExecuteUpTo.put(ev.toString(), eventExecuteUpTo.get(ev.toString()) + 1);
        ex.setExecuteSequence(eventExecuteUpTo.get(ev.toString()));
        executionLog.put(ex.toString(), ex.scheduleTime);
        Execution newEx = null;
        if (!ex.canceled) {
            ev.executeTheEvent(systemState);
            eventNbExistExecutions.put(ev.toString(), eventNbExistExecutions.get(ev.toString()) - 1);
            if (ev.type.equals("zero")) {
                EventPositiveDelay ep = ev.getCountedEvent();
                if (ep != null) {
                    // 新execution
                    this.eventScheduleUpTo.put(ep.toString(), this.eventScheduleUpTo.get(ep.toString()) + 1);
                    double t = ep.getOneSampleOfDelay(new PrintWriter(OutputStream.nullOutputStream()));
                    rvResult.println("【" + ep.toString() + "】 " + t);
                    newEx = new Execution(ep, this.eventScheduleUpTo.get(ep.toString()), clockTime, t);
                    eventNbExistExecutions.put(ep.toString(), eventNbExistExecutions.get(ep.toString()) + 1);

                    // 将新的execution放入futureExecutionList中
                    futureExecutionList.add(newEx);
                    int xi = events.indexOf(ep);
                    int i = this.eventScheduleUpTo.get(ep.toString()) - 1;
                    int k = nbTotalExecutionsSimulated + 1;
                    xSol[xi][i][k] = 1;
                    kxiSol[xi][0][i] = k;
                }
            }
        }

        // 记录
        nbTotalExecutionsSimulated++;
        int i = ex.scheduleSequence - 1;
        int k1 = nbTotalExecutionsSimulated;
        int xi = events.indexOf(ev);
        EpsilonSol[k1] = clockTime;
        eSol[xi][1][i] = clockTime;
        wSol[xi][i][k1] = 1;
        if (!ex.canceled) gammaSol[xi][i][k1] = 1;
        kxiSol[xi][1][i] = k1;
        StringBuilder s = new StringBuilder();
        for (StateVariable sVar : stateVariables) {
            int s1 = stateVariables.indexOf(sVar);
            uSol[s1][k1] = sVar.value;
        }
        for (String k : systemState.keySet())
            s.append(systemState.get(k).printState());
        if (withLog) {
            log.add(new SimLog(nbTotalExecutionsSimulated, clockTime, ex, s.toString()));
            if (newEx != null) log.get(log.size() - 1).addScheduledExe(newEx);
        }


    }

    void scheduleNewEvents(boolean withLog) {
        ArrayList<Execution> exes = new ArrayList<>();
        for (Event e : events) {
            if (e.type.equals("zero") && eventScheduleUpTo.get(e.toString()) < nbExecutions.get(e.toString()) && // 事件的schedule的数量低于设定的数量
                    e.possibleToAddNewExecute(systemState, eventNbExistExecutions.get(e.toString()))) { // schedule conditions 都满足

                // 新execution
                this.eventScheduleUpTo.put(e.toString(), this.eventScheduleUpTo.get(e.toString()) + 1);
                Execution ex = new Execution(e, this.eventScheduleUpTo.get(e.toString()), clockTime, e.getOneSampleOfDelay(new PrintWriter(OutputStream.nullOutputStream())));
                exes.add(ex);
                eventNbExistExecutions.put(e.toString(), eventNbExistExecutions.get(e.toString()) + 1);

                // 将新的execution放入futureExecutionList中
                futureExecutionList.add(ex);

                int xi = events.indexOf(e);
                int i = this.eventScheduleUpTo.get(e.toString()) - 1;
                int k = nbTotalExecutionsSimulated;
                xSol[xi][i][k] = 1;
                eSol[xi][0][i] = clockTime;
                zSol[xi][k] = 1;
                kxiSol[xi][0][i] = k;
            }
        }
        if (withLog) log.get(log.size() - 1).setScheduledExe(exes);
    }

    public double getClockTime() {
        return clockTime;
    }

    protected void printLog() {
        if (!log.isEmpty()) {
            logWriter.println("Sequence\tEvent\tExecutionTime\tState\tScheduledExecutions\tEventScheduleSeq\tEventExecutionSeq\tScheduleTime\tDelay");
            for (SimLog simLog : log) {
                logWriter.println(simLog.toString());
            }
        } else {
            logWriter.println("Log is not saved for this simulation run.");
        }

    }

    public static class SimLog {
        int exeSequence;
        public double exeTime;
        Execution execution;
        String scheduledExe = "";
        String currentState;

        public SimLog(int k, String state) {
            exeSequence = k;
            currentState = state;
        }

        public SimLog(int k, double t, Execution e, String state) {
            exeSequence = k;
            exeTime = t;
            execution = e;
            currentState = state;
        }

        public void setScheduledExe(ArrayList<Execution> exes) {
            for (Execution ex : exes)
                this.scheduledExe += ex.event.toString();
        }

        public void addScheduledExe(Execution ex) {
            this.scheduledExe += ex.event.toString();
        }

        @Override
        public String toString() {
            if (exeSequence == 0) return exeSequence + "\t" +
                    "\t" +
                    "\t" +
                    currentState + "\t" +
                    scheduledExe + "\t" +
                    "\t" +
                    "\t" +
                    "\t"
                    ;

            DecimalFormat df;
            df = new DecimalFormat("#.###");
            df.setRoundingMode(RoundingMode.CEILING);
            return exeSequence + "\t" +
                    execution.event.toString() + "\t" +
                    df.format(exeTime) + "\t" +
                    currentState + "\t" +
                    scheduledExe + "\t" +
                    execution.scheduleSequence + "\t" +
                    execution.executeSequence + "\t" +
                    df.format(execution.scheduleTime) + "\t" +
                    df.format(execution.executiontime - execution.scheduleTime);
        }
    }

    // ************************************************* End Simulate ************************************* //

    // ************************************************* MPR solution from simulation ********************************************** //
    double[] EpsilonSol;
    double[][] uSol;
    double[][][] eSol;
    double[][][] wSol;
    double[][][] xSol;
    double[][] zSol;
    double[][][] gammaSol;
    double[][][] kxiSol;

    //double[][][][] vSol;
    //double[][] vnSol;
    //double[][] vBetaSol;
    void simulateToGetMprSolution() {
        maxXi = events.size();
        maxS = stateVariables.size();
        maxK = totNbExecutions + 1;
        EpsilonSol = new double[maxK];
        for (int k = 0; k < maxK; k++) EpsilonSol[k] = 0;
        uSol = new double[maxS][maxK];
        for (int s = 0; s < maxS; s++) {
            for (int k = 0; k < maxK; k++) uSol[s][k] = 0;
        }
        eSol = new double[maxXi][2][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int l = 0; l < 2; l++) {
                eSol[xi][l] = new double[maxI];
                for (int i = 0; i < maxI; i++) eSol[xi][l][i] = 0;
            }
        }
        wSol = new double[maxXi][][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            wSol[xi] = new double[maxI][maxK];
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++)
                    wSol[xi][i][k] = 0;
            }
        }
        xSol = new double[maxXi][][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            xSol[xi] = new double[maxI][maxK];
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++)
                    xSol[xi][i][k] = 0;
            }
        }
        zSol = new double[maxXi][maxK];
        for (int xi = 0; xi < maxXi; xi++) {
            for (int k = 0; k < maxK; k++) {
                zSol[xi][k] = 0;
            }
        }
        gammaSol = new double[maxXi][][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            gammaSol[xi] = new double[maxI][maxK];
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++) {
                    gammaSol[xi][i][k] = 0;
                }
            }
        }
        kxiSol = new double[maxXi][2][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            kxiSol[xi][0] = new double[maxI];
            kxiSol[xi][1] = new double[maxI];
            for (int i = 0; i < maxI; i++) {
                kxiSol[xi][0][i] = 0;
                kxiSol[xi][1][i] = 0;
            }
        }
    }

    protected void validateMPRFeasibility() throws IloException {
        for (int k = 0; k < maxK; k++) {
            {
                IloLinearNumExpr constA3 = cplex.linearNumExpr();
                constA3.addTerm(1, Epsilon[k]);
                cplex.addEq(constA3, EpsilonSol[k], "Epsilon_" + k);
            }

            for (int s = 0; s < maxS; s++) {
                IloLinearNumExpr constA4 = cplex.linearNumExpr();
                constA4.addTerm(1, u[s][k]);
                cplex.addEq(constA4, uSol[s][k], "u_" + (stateVariables.get(s).name) + "_" + k);
            }
        }
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int l = 0; l < 2; l++) {
                for (int i = 0; i < maxI; i++) {
                    IloLinearNumExpr constA3 = cplex.linearNumExpr();
                    constA3.addTerm(1, e[xi][l][i]);
                    cplex.addEq(constA3, e[xi][l][i], "e_" + events.get(xi).toString() + "_" + (l) + "_" + (i));
                }
            }
        }
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++) {
                    {
                        IloLinearNumExpr constA3 = cplex.linearNumExpr();
                        constA3.addTerm(1, w[xi][i][k]);
                        cplex.addEq(constA3, wSol[xi][i][k], "w_" + events.get(xi).toString() + "_" + (i) + "_" + (k));
                    }
                    {
                        IloLinearNumExpr constA3 = cplex.linearNumExpr();
                        constA3.addTerm(1, x[xi][i][k]);
                        cplex.addEq(constA3, xSol[xi][i][k], "x_" + events.get(xi).toString() + "_" + (i) + "_" + (k));
                    }
                }
            }
            for (int k = 0; k < maxK; k++) {
                {
                    IloLinearNumExpr constA3 = cplex.linearNumExpr();
                    constA3.addTerm(1, z[xi][k]);
                    cplex.addEq(constA3, zSol[xi][k], "z_" + events.get(xi).toString() + "_" + (k));
                }
            }
        }
    }


    // ************************************************* MPR ********************************************** //
    public IloCplex cplex;
    int maxXi;
    int maxS;
    int maxK;
    IloNumVar[] Epsilon;
    IloNumVar[][] u;
    IloNumVar[][][] e;
    IloNumVar[][][] w;
    IloNumVar[][][] x;
    //IloNumVar[][][] y;
    IloNumVar[][] z;
    IloNumVar[][][][] v;
    //IloNumVar[][] sumX;
    IloNumVar[][] vn;
    IloNumVar[][] vBeta;

    protected void buildMilpModel() throws IloException {
        cplex = new IloCplex();
        maxXi = events.size();
        maxS = stateVariables.size();
        maxK = totNbExecutions + 1;
        //for(Event ev: events) maxK += nbExecutions.get(ev.toString());

        //HashMap<String, Integer> eventNameToId = new HashMap<>();
        //for(int xi = 0; xi< maxXi;xi++) eventNameToId.put(events.get(xi).toString(),xi);
        //HashMap<String, Integer> stateVariableNameToId = new HashMap<>();
        //for(int s=0; s<maxS;s++) stateVariableNameToId.put(stateVariables.get(s).toString(),s);

        // 决策变量 decision variables ==================================================================================
        Epsilon = new IloNumVar[maxK];
        for (int k = 0; k < maxK; k++) {
            String label = "E_" + k;
            Epsilon[k] = cplex.numVar(0, MY_INFTY, label);
        }

        u = new IloNumVar[maxS][maxK];
        for (int s = 0; s < stateVariables.size(); s++) {
            StateVariable sv = stateVariables.get(s);
            for (int k = 0; k < maxK; k++) {
                String label = "u_" + sv.name + "_" + k;
                u[s][k] = cplex.intVar(sv.lb, sv.ub, label);
            }
        }

        e = new IloNumVar[maxXi][2][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int l = 0; l < 2; l++) {
                e[xi][l] = new IloNumVar[maxI];
                for (int i = 0; i < maxI; i++) {
                    String label = "e_" + events.get(xi).name + "_" + l + "_" + i;
                    e[xi][l][i] = cplex.numVar(0, MY_INFTY, label);
                }
            }
        }

        w = new IloNumVar[maxXi][][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            w[xi] = new IloNumVar[maxI][maxK];
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++) {
                    String label = "w_" + events.get(xi).name + "_" + i + "_" + k;
                    w[xi][i][k] = cplex.boolVar(label);
                }
            }
        }

        x = new IloNumVar[maxXi][][];
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            x[xi] = new IloNumVar[maxI][maxK];
            for (int i = 0; i < maxI; i++) {
                for (int k = 0; k < maxK; k++) {
                    String label = "x_" + events.get(xi).name + "_" + i + "_" + k;
                    x[xi][i][k] = cplex.boolVar(label);
                }
            }
        }

        /*y = new IloNumVar[maxXi][][];
        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            y[xi] = new IloNumVar[maxI][maxI];
            for(int i=0;i<maxI;i++){
                for(int i0=0;i0<maxI;i0++){
                    String label = "y_"+events.get(xi).name+"_"+i+"_"+i0;
                    y[xi][i][i0] = cplex.boolVar(label);
                }
            }
        }*/

        z = new IloNumVar[maxXi][maxK];
        for (int xi = 0; xi < maxXi; xi++) {
            for (int k = 0; k < maxK; k++) {
                String label = "z_" + events.get(xi).name + "_" + k;
                z[xi][k] = cplex.boolVar(label);
            }
        }

        v = new IloNumVar[maxXi][maxS][2][maxK];
        for (int xi = 0; xi < maxXi; xi++) {
            for (int s = 0; s < maxS; s++) {
                for (int l = 0; l < 2; l++) {
                    for (int k = 0; k < maxK; k++) {
                        String label = "v_" + events.get(xi).name + "_" + stateVariables.get(s).name + "_" + l + "_" + k;
                        v[xi][s][l][k] = cplex.boolVar(label);
                    }
                }
            }
        }

        vn = new IloNumVar[maxXi][maxK];
        for (int xi = 0; xi < maxXi; xi++) {
            for (int k = 0; k < maxK; k++) {
                String label = "vn_" + events.get(xi).name + "_" + k;
                vn[xi][k] = cplex.boolVar(label);
            }
        }

        vBeta = new IloNumVar[maxXi][maxK];
        for (int xi = 0; xi < maxXi; xi++) {
            for (int k = 0; k < maxK; k++) {
                String label = "vBeta_" + events.get(xi).name + "_" + k;
                vBeta[xi][k] = cplex.boolVar(label);
            }
        }

        /*sumX = new IloNumVar[maxXi][maxK];
        for(int xi = 0;xi<maxXi;xi++){
            int maxR = events.get(xi).nbParallelExecutions;
            for(int k=0;k<maxK;k++){
                String label = "sumX_"+events.get(xi).name+"_"+k;
                sumX[xi][k] = cplex.intVar(0,maxR,label);
            }
        }*/

        // 目标函数 objective ===========================================================================================
        IloLinearNumExpr objective = cplex.linearNumExpr();
        for (int k = 0; k < maxK; k++) {
            objective.addTerm(1, Epsilon[k]);
        }
        cplex.addMinimize(objective, "Sum of Epsilon[k]");

        // 约束 A constraints A : binding event execution and Epsilon[k] ===============================================
        for (int xi = 0; xi < maxXi; xi++) {
            for (int k = 1; k < maxK; k++) {
                int maxI = nbExecutions.get(events.get(xi).toString());
                for (int i = 0; i < maxI; i++) {
                    IloLinearNumExpr constA1 = cplex.linearNumExpr();
                    constA1.addTerm(1, e[xi][1][i]);
                    constA1.addTerm(-1, Epsilon[k]);
                    constA1.addTerm(-MY_INFTY, w[xi][i][k]);
                    cplex.addGe(constA1, -MY_INFTY, "A1_" + events.get(xi).name + "_" + k + "_" + i);

                    IloLinearNumExpr constA2 = cplex.linearNumExpr();
                    constA2.addTerm(-1, e[xi][1][i]);
                    constA2.addTerm(1, Epsilon[k]);
                    constA2.addTerm(-MY_INFTY, w[xi][i][k]);
                    cplex.addGe(constA2, -MY_INFTY, "A2_" + events.get(xi).name + "_" + k + "_" + i);
                }
            }
        }

        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int i = 0; i < maxI; i++) {
                IloLinearNumExpr constA3 = cplex.linearNumExpr();
                for (int k = 1; k < maxK; k++) {
                    constA3.addTerm(1, w[xi][i][k]);
                }
                cplex.addLe(constA3, 1, "A3_" + events.get(xi).name + "_" + i);
            }
        }

        for (int k = 1; k < maxK; k++) {
            IloLinearNumExpr constA4 = cplex.linearNumExpr();
            for (int xi = 0; xi < maxXi; xi++) {
                int maxI = nbExecutions.get(events.get(xi).toString());
                for (int i = 0; i < maxI; i++) {
                    constA4.addTerm(1, w[xi][i][k]);
                }
            }
            cplex.addEq(constA4, 1, "A4_" + k);
        }

        for (int k = 1; k < maxK; k++) {
            IloLinearNumExpr constA5 = cplex.linearNumExpr();
            constA5.addTerm(1, Epsilon[k]);
            constA5.addTerm(-1, Epsilon[k - 1]);
            cplex.addGe(constA5, 0, "A5_" + k);
        }

        /*for(int xi=0;xi<maxXi;xi++){
            if(events.get(xi).type.equals("zero")){
                int maxI = nbExecutions.get(events.get(xi).toString());
                for(int i=0;i<maxI-1;i++){
                    IloLinearNumExpr constA5 = cplex.linearNumExpr();
                    for(int k=1;k<maxK;k++){
                        constA5.addTerm(k,w[xi][i+1][k]);
                        constA5.addTerm(-k,w[xi][i][k]);
                    }
                    cplex.addGe(constA5,1,"A6_"+events.get(xi).name+"_"+i);
                }
            }
        }*/

        IloLinearNumExpr constA7 = cplex.linearNumExpr();
        constA7.addTerm(1, Epsilon[0]);
        cplex.addEq(constA7, 0, "A7");

        // 约束 B constraints B : binding event schedule and execution, especially for case of multiple parallel executions
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            Event exi = events.get(xi);
            //if(exi.nbParallelExecutions==1){
            for (int i = 0; i < maxI; i++) {
                IloLinearNumExpr constB1 = cplex.linearNumExpr();
                constB1.addTerm(1, e[xi][1][i]);
                constB1.addTerm(-1, e[xi][0][i]);
                double t = exi.getOneSampleOfDelay(new PrintWriter(OutputStream.nullOutputStream()));
                if (t > 0) System.out.println("【" + exi.toString() + "】 " + t);
                cplex.addEq(constB1, t, "B1_" + events.get(xi).name + "_" + i);
            }
            //}
            /*else{
                for(int i=0;i<maxI;i++){
                    double txii =exi.getOneSampleOfDelay(new PrintWriter(OutputStream.nullOutputStream()));
                    for(int i1=0;i1<maxI;i1++){
                        IloLinearNumExpr constB1 = cplex.linearNumExpr();
                        constB1.addTerm(1,e[xi][1][i1]);
                        constB1.addTerm(-1,e[xi][0][i]);
                        constB1.addTerm(-MY_INFTY,y[xi][i][i1]);
                        cplex.addGe(constB1,txii-MY_INFTY,"B2_"+events.get(xi).name+"_"+i+"_"+i1);

                        IloLinearNumExpr constB2 = cplex.linearNumExpr();
                        constB2.addTerm(-1,e[xi][1][i1]);
                        constB2.addTerm(1,e[xi][0][i]);
                        constB2.addTerm(-MY_INFTY,y[xi][i][i1]);
                        cplex.addGe(constB2,-txii-MY_INFTY,"B3_"+events.get(xi).name+"_"+i+"_"+i1);
                    }
                }

                for(int i1=0;i1<maxI;i1++){
                    IloLinearNumExpr constB3 = cplex.linearNumExpr();
                    for(int i=0;i<maxI;i++){
                        constB3.addTerm(1,y[xi][i][i1]);
                    }
                    cplex.addEq(constB3,1,"B4_"+events.get(xi).name+"_"+i1);
                }

                for(int i=0;i<maxI;i++){
                    IloLinearNumExpr constB4 = cplex.linearNumExpr();
                    for(int i1=0;i1<maxI;i1++){
                        constB4.addTerm(1,y[xi][i][i1]);
                    }
                    cplex.addEq(constB4,1,"B5_"+events.get(xi).name+"_"+i);
                }

                int betaXi=exi.nbParallelExecutions;
                for(int i=0;i<maxI-betaXi;i++){
                    IloLinearNumExpr constB5 = cplex.linearNumExpr();
                    for(int i1=i+betaXi;i1<maxI;i1++){
                        constB5.addTerm(1,y[xi][i][i1]);
                    }
                    cplex.addEq(constB5,0,"B6_"+events.get(xi).name+"_"+i);
                }

                for(int i1=betaXi;i1<maxI;i1++){
                    IloLinearNumExpr constB6 = cplex.linearNumExpr();
                    for(int i=0;i<i1-betaXi;i++){
                        constB6.addTerm(1,y[xi][i][i1]);
                    }
                    cplex.addEq(constB6,0,"B7_"+events.get(xi).name+"_"+i1);
                }
            }*/
        }

        // 约束 C constraints C : schedule events if system states after Epsilon[k] is appropriate =====================
        for (int xi = 0; xi < maxXi; xi++) {
            int maxI = nbExecutions.get(events.get(xi).toString());
            for (int k = 0; k < maxK; k++) {
                for (int i = 0; i < maxI; i++) {
                    {
                        IloLinearNumExpr constC1 = cplex.linearNumExpr();
                        constC1.addTerm(1, e[xi][0][i]);
                        constC1.addTerm(-1, Epsilon[k]);
                        constC1.addTerm(-MY_INFTY, x[xi][i][k]);
                        cplex.addGe(constC1, -MY_INFTY, "C1_" + events.get(xi).name + "_" + k + "_" + i);
                    }

                    {
                        IloLinearNumExpr constC2 = cplex.linearNumExpr();
                        constC2.addTerm(-1, e[xi][0][i]);
                        constC2.addTerm(1, Epsilon[k]);
                        constC2.addTerm(-MY_INFTY, x[xi][i][k]);
                        cplex.addGe(constC2, -MY_INFTY, "C2_" + events.get(xi).name + "_" + k + "_" + i);
                    }
                }
            }

            Event exi = events.get(xi);
            if (exi.type.equals("zero")) {

                for (int k = 0; k < maxK; k++) {
                    IloLinearNumExpr constC10 = cplex.linearNumExpr();
                    for (ConditionToSchedule cs : exi.conditionsToSchedule) {
                        int s = stateVariables.indexOf(cs.stateVariable);
                        int a = cs.lowerBound;
                        int b = cs.upperBound;
                        double M;

                        {
                            IloLinearNumExpr constC4 = cplex.linearNumExpr();
                            M = a - stateVariables.get(s).lb;
                            constC4.addTerm(1, u[s][k]);
                            constC4.addTerm(-M, z[xi][k]);
                            cplex.addGe(constC4, a - M, "C3_" + events.get(xi).name + "_" + stateVariables.get(s).name + "_" + k);
                        }

                        {
                            IloLinearNumExpr constC5 = cplex.linearNumExpr();
                            M = stateVariables.get(s).ub - b;
                            constC5.addTerm(-1, u[s][k]);
                            constC5.addTerm(-M, z[xi][k]);
                            cplex.addGe(constC5, -b - M, "C4_" + events.get(xi).name + "_" + stateVariables.get(s).name + "_" + k);
                        }

                        {
                            IloLinearNumExpr constC6 = cplex.linearNumExpr();
                            M = stateVariables.get(s).ub - (a - 1);
                            constC6.addTerm(-1, u[s][k]);
                            constC6.addTerm(-M, v[xi][s][0][k]);
                            cplex.addGe(constC6, -a + 1 - M, "C5_" + events.get(xi).name + "_" + stateVariables.get(s).name + "_" + k);
                        }

                        {
                            IloLinearNumExpr constC7 = cplex.linearNumExpr();
                            M = b + 1 - stateVariables.get(s).lb;
                            constC7.addTerm(1, u[s][k]);
                            constC7.addTerm(-M, v[xi][s][1][k]);
                            cplex.addGe(constC7, b + 1 - M, "C6_" + events.get(xi).name + "_" + stateVariables.get(s).name + "_" + k);
                        }

                        constC10.addTerm(1, v[xi][s][0][k]);
                        constC10.addTerm(1, v[xi][s][1][k]);
                    }

                    /*{
                        IloLinearNumExpr constCN4 = cplex.linearNumExpr();
                        for(int k1=0;k1<=k;k1++){
                            constCN4.addTerm(1,z[xi][k1]);
                        }
                        cplex.addLe(constCN4,maxI,"C7_"+events.get(xi).name+"_"+k);
                    }*/

                    /*{
                        IloLinearNumExpr constCN6 = cplex.linearNumExpr();
                        for(int k1=0;k1<k;k1++){
                            constCN6.addTerm(1,z[xi][k1]);
                        }
                        constCN6.addTerm(-maxI,vn[xi][k]);
                        cplex.addGe(constCN6,0,"C7_"+events.get(xi).name+"_"+k);
                    }*/

                    {
                        if (k >= 1) {
                            IloLinearNumExpr constC8 = cplex.linearNumExpr();
                            constC8.addTerm(1, vBeta[xi][k]);
                            constC8.addTerm(-1, vBeta[xi][k - 1]);
                            for (int i = 0; i < maxI; i++) {
                                constC8.addTerm(1, w[xi][i][k]);
                            }
                            constC8.addTerm(-1, z[xi][k - 1]);
                            cplex.addEq(constC8, 0, "C7_" + events.get(xi).name + "_" + k);
                        } else {
                            IloLinearNumExpr constC8 = cplex.linearNumExpr();
                            constC8.addTerm(1, vBeta[xi][0]);
                            cplex.addEq(constC8, 0, "C7_" + events.get(xi).name + "_" + k);
                        }
                    }

                    {
                        IloLinearNumExpr constC9 = cplex.linearNumExpr();
                        for (int k1 = 0; k1 <= k - 1; k1++) {
                            constC9.addTerm(1, z[xi][k1]);
                        }
                        constC9.addTerm(-maxI, vn[xi][k]);
                        cplex.addGe(constC9, 0, "C8_" + events.get(xi).name + "_" + k);
                    }

                    constC10.addTerm(1, z[xi][k]);
                    constC10.addTerm(1, vn[xi][k]);
                    constC10.addTerm(1, vBeta[xi][k]);
                    cplex.addGe(constC10, 1, "C9_" + events.get(xi).name + "_" + k);

                    {
                        IloLinearNumExpr constC11 = cplex.linearNumExpr();
                        for (int i = 0; i < maxI; i++) {
                            constC11.addTerm(1, x[xi][i][k]);
                        }
                        constC11.addTerm(-1, z[xi][k]);
                        cplex.addEq(constC11, 0, "C10_" + events.get(xi).name + "_" + k);
                    }
                }

                for (int i = 0; i < maxI; i++) {
                    IloLinearNumExpr constC12 = cplex.linearNumExpr();
                    for (int k = 0; k < maxK; k++) {
                        constC12.addTerm(1, x[xi][i][k]);
                    }
                    cplex.addLe(constC12, 1, "C11_" + events.get(xi).name + "_" + i);
                }

                /*for (int i = 0; i < maxI - 1; i++) {
                    IloLinearNumExpr constC13 = cplex.linearNumExpr();
                    double M = maxK;
                    for (int k = 0; k < maxK - 1; k++) {
                        constC13.addTerm(k, x[xi][i + 1][k]);
                        constC13.addTerm(-k, w[xi][i][k]);
                        constC13.addTerm(-M, x[xi][i + 1][k]);
                    }
                    cplex.addGe(constC13, -M, "C12_" + events.get(xi).name + "_" + i);
                }*/


            } else {
                int tildeXi = 0;
                for (int l = 0; l < events.size(); l++) {
                    if (events.get(l) == exi.countEvent) {
                        tildeXi = l;
                        break;
                    }
                }

                for (int i = 0; i < maxI; i++) {
                    for (int k = 0; k < maxK; k++) {
                        IloLinearNumExpr constC14 = cplex.linearNumExpr();
                        constC14.addTerm(1, x[xi][i][k]);
                        constC14.addTerm(-1, w[tildeXi][i][k]);
                        cplex.addEq(constC14, 0, "C14_" + events.get(xi).name + "_" + i + "_" + k);
                    }
                }


                /*for(int k=0;k<maxK;k++){
                    IloLinearNumExpr constC12 = cplex.linearNumExpr();
                    constC12.addTerm(1,z[xi][k]);
                    constC12.addTerm(-1,z[tildeXi][k]);
                    cplex.addEq(constC12,0,"C17_"+events.get(xi).name+"_"+k);
                }*/
            }

            for (int i = 0; i < maxI; i++) {
                IloLinearNumExpr constC3 = cplex.linearNumExpr();
                double M = maxK + 1;
                for (int k = 0; k < maxK; k++) {
                    constC3.addTerm(k, w[xi][i][k]);
                    constC3.addTerm(-k, x[xi][i][k]);
                    constC3.addTerm(-M, w[xi][i][k]);
                }
                cplex.addGe(constC3, 1 - M, "C13_" + events.get(xi).name + "_" + i);
            }

            for (int i = 0; i < maxI - 1; i++) {
                double M = maxK;
                IloLinearNumExpr constC13 = cplex.linearNumExpr();
                for (int k = 0; k < maxK; k++) {
                    constC13.addTerm(k, x[xi][i + 1][k]);
                    constC13.addTerm(-k, x[xi][i][k]);
                    constC13.addTerm(-M, x[xi][i + 1][k]);
                }
                cplex.addGe(constC13, 1 - M, "C15_" + events.get(xi).name + "_" + i);
            }

            {
                for (int i = 0; i < maxI - 1; i++) {
                    IloLinearNumExpr constA3 = cplex.linearNumExpr();
                    for (int k = 0; k < maxK; k++) {
                        constA3.addTerm(1, x[xi][i + 1][k]);
                        constA3.addTerm(-1, x[xi][i][k]);
                    }
                    cplex.addLe(constA3, 0, "C16_" + events.get(xi).name + "_" + i);
                }
            }

            {
                for (int i = 0; i < maxI; i++) {
                    IloLinearNumExpr constC13 = cplex.linearNumExpr();
                    for (int k = 0; k < maxK; k++) {
                        constC13.addTerm(1, x[xi][i][k]);
                        constC13.addTerm(-1, w[xi][i][k]);
                    }
                    cplex.addGe(constC13, 0, "C17_" + events.get(xi).name + "_" + i);
                }
            }
        }
        // 约束结束 constraints end =====================================================================================
    }

    protected boolean solveMilpModel() throws IloException {
        //cplex.setParam(IloCplex.Param.Threads, 16);
        return cplex.solve();
    }

    protected void printMilpResults(PrintWriter result) throws IloException {
        result.println("Iteration\tOccurringTime\tEventType\tExecutionIndex");
        DecimalFormat df;
        df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        for (int k = 0; k < maxK; k++) {
            result.print(k + "\t");
            int executionIndex = 0;

            String eventName = "";
            if (k > 0) {
                for (int xi = 0; xi < maxXi; xi++) {
                    int maxI = nbExecutions.get(events.get(xi).toString());
                    for (int i = 0; i < maxI; i++) {
                        if (cplex.getValue(w[xi][i][k]) > 0.9999) {
                            eventName = events.get(xi).toString();
                            executionIndex = i;
                            break;
                        }
                    }
                }
            }
            result.print(df.format(cplex.getValue(Epsilon[k])) + "\t");
            result.print(eventName + "\t");
            result.print(executionIndex);

            result.println();
        }
    }

    protected void printMilpModel(PrintWriter model) throws IloException {
        model.println(cplex.getModel());
    }

    // ************************************************* End MPR ****************************************** //

    protected void resetRandomStream() {
        for (Event e : events) {
            e.delay.resetRandomNumberStream();
        }
    }

    // ************************************************* MPR validation ****************************************** //
    HashMap<String, Double> executionLog = new HashMap<>();

    public boolean validateMpr() throws IloException {

        resetRandomStream();
        simulate(false, new PrintWriter(OutputStream.nullOutputStream()));

        resetRandomStream();
        MY_INFTY = Math.max(MY_INFTY, 100);
        buildMilpModel();

        boolean b = solveMilpModel();

        if (b) {
            for (int xi = 0; xi < maxXi; xi++) {
                int maxI = nbExecutions.get(events.get(xi).toString());
                for (int i = 0; i < maxI; i++) {
                    /*int i1;
                    if(events.get(xi).nbParallelExecutions ==1){
                        i1 =i;
                    }else{
                        for(i1=0;i1<maxI;i1++){
                            if(cplex.getValue(y[xi][i1][i])>0.9999)
                                break;
                        }
                    }*/

                    if (executionLog.containsKey(events.get(xi).toString() + (i + 1)) &&
                            (cplex.getValue(e[xi][0][i]) - executionLog.get(events.get(xi).toString() + (i + 1)) > 0.0001 ||
                                    cplex.getValue(e[xi][0][i]) - executionLog.get(events.get(xi).toString() + (i + 1)) < -0.0001)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;

    }

    public boolean validateMpr(PrintWriter input, PrintWriter des, PrintWriter mpr) throws IloException {
        logWriter = des;
        resetRandomStream();
        simulate(true, input);
        printLog();

        resetRandomStream();
        MY_INFTY = Math.max(MY_INFTY, 100);
        buildMilpModel();

        boolean b = solveMilpModel();

        if (b) {
            for (int xi = 0; xi < maxXi; xi++) {
                int maxI = nbExecutions.get(events.get(xi).toString());
                for (int i = 0; i < maxI; i++) {
                    /*int i1;
                    if(events.get(xi).nbParallelExecutions ==1){
                        i1 =i;
                    }else{
                        for(i1=0;i1<maxI;i1++){
                            if(cplex.getValue(y[xi][i1][i])>0.9999)
                                break;
                        }
                    }*/

                    if (executionLog.containsKey(events.get(xi).toString() + (i + 1)) &&
                            (cplex.getValue(e[xi][0][i]) - executionLog.get(events.get(xi).toString() + (i + 1)) > 0.0001 ||
                                    cplex.getValue(e[xi][0][i]) - executionLog.get(events.get(xi).toString() + (i + 1)) < -0.0001)) {
                        return false;
                    }
                }
            }
            printMilpResults(mpr);
            return true;
        }
        return false;

    }
    // ********************************************* End MPR validation ****************************************** //
}
