package simulation;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;

public abstract class SimulatorWithEventCancellation extends Simulator {

    public SimulatorWithEventCancellation()throws IloException{}

    // **************************** Simulate ************************************************************ //
    @Override
    public void simulate(boolean withLog,PrintWriter rvResult){
        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String logDirectory = programPath + File.separator+"OUTPUT"+File.separator+"log.txt";
        OutputStream logStream = null;
        try {
            logStream  = new FileOutputStream(logDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter result = new PrintWriter(logStream, true);

        resetSimulator();
        simulateToGetMprSolution();
        StringBuilder s = new StringBuilder();
        for(String k: systemState.keySet())
            s.append(systemState.get(k).printState());
        if(withLog) log.add(new SimLog(0, s.toString()));
        scheduleNewEvents(withLog,rvResult);
        while(!isTerminate){
            while( // 将某一时刻可以执行的事件全部执行
                    nbTotalExecutionsSimulated<totNbExecutions // 仿真过的事件总数量小于预设的数量
                            && futureExecutionList.size()>0 // Future event list 不空
                            && futureExecutionList.peek().executiontime == clockTime){
                result.print(futureExecutionList.peek().event.toString() + ": ");
                result.print(futureExecutionList.peek().executiontime + "\n");
                executeNextEvent(withLog,rvResult);
                cancelExecutions();
                scheduleNewEvents(withLog,rvResult);
            }
            if(nbTotalExecutionsSimulated<totNbExecutions
                    && futureExecutionList.size()>0)
                clockTime = futureExecutionList.peek().executiontime; // 仿真时钟向前拨
            else
                isTerminate = true;
        }

        /*MY_INFTY = clockTime;
        if(!futureExecutionList.isEmpty()){
            for(Execution exe: futureExecutionList) MY_INFTY =Math.max(MY_INFTY,exe.executiontime);
        }*/
    }

    public void cancelExecutions(){
        for(Execution exe: futureExecutionList){
            if(exe.event.possibleToCancelExecute(systemState)){
                exe.canceled = true;
                eventNbExistExecutions.put(exe.event.toString(),eventNbExistExecutions.get(exe.event.toString())-1);
                exe.event.countVariable.value-=1;

                int xi = events.indexOf(exe.event);
                int k = nbTotalExecutionsSimulated;
                zbarSol[xi][k] = 1;
                int i= exe.scheduleSequence-1;
                gammaSol[xi][i][k] = 0;
            }
        }
    }

    // **************************** End Simulate ******************************************************** //

    double[][] zbarSol;
    //double[][][][] vbarSol;
    //double[][][] thetaSol;
    //double[][][][] phiSol;
    //double[][] vbarBetaSol;

    @Override
    void simulateToGetMprSolution(){
        super.simulateToGetMprSolution();
        zbarSol = new double[maxXi][maxK];
        for(int xi = 0;xi<maxXi;xi++){
            for(int k=0;k<maxK;k++){
                zbarSol[xi][k] = 0;
            }
        }
    }

    @Override
    protected void validateMPRFeasibility() throws IloException{
        super.validateMPRFeasibility();
        for(int xi = 0;xi<maxXi;xi++){
            for(int k=0 ; k<maxK;k++){
                IloLinearNumExpr constA3 = cplex.linearNumExpr();
                constA3.addTerm(1,zbar[xi][k]);
                cplex.addEq(constA3,zbarSol[xi][k],"zbar_"+k);
            }
        }

        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            for(int i=0;i<maxI;i++){
                for(int k=0;k<maxK;k++){
                    IloLinearNumExpr constA3 = cplex.linearNumExpr();
                    constA3.addTerm(1,gamma[xi][i][k]);
                    cplex.addEq(constA3,gammaSol[xi][i][k],"gamma_"+k);
                }
            }
        }

        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            for(int i=0;i<maxI;i++){
                for(int l=0;l<2;l++){
                    IloLinearNumExpr constA3 = cplex.linearNumExpr();
                    constA3.addTerm(1,kxi[xi][l][i]);
                    cplex.addEq(constA3,kxiSol[xi][l][i],"kxi_");
                }
            }
        }
    }



    IloNumVar[][] zbar;
    IloNumVar[][][][] vbar;
    IloNumVar[][][] theta;
    IloNumVar[][][][] phi;
    IloNumVar[][][] gamma;
    IloNumVar[][][] kxi;
    //IloNumVar[][] vbarBeta;
    @Override
    protected void buildMilpModel() throws IloException{
        super.buildMilpModel();

        // 决策变量 decision variables ==================================================================================
        // Event cancellation ==========================================================================================
        zbar = new IloNumVar[maxXi][maxK];
        for(int xi = 0;xi<maxXi;xi++){
            for(int k=0 ; k<maxK;k++){
                String label = "zbar_"+events.get(xi).name+"_"+k;
                zbar[xi][k] = cplex.boolVar(label);
            }
        }

        vbar = new IloNumVar[maxXi][maxS][2][maxK];
        for(int xi = 0;xi<maxXi;xi++){
            for(int s=0;s<maxS;s++){
                for(int l=0;l<2;l++){
                    for(int k=0;k<maxK;k++){
                        String label = "vbar_"+events.get(xi).name+"_"+stateVariables.get(s).name+"_"+l+"_"+k;
                        vbar[xi][s][l][k] = cplex.boolVar(label);
                    }
                }
            }
        }

        /*vbarBeta = new IloNumVar[maxXi][maxK];
        for(int xi = 0;xi<maxXi;xi++){
            for(int k=0;k<maxK;k++){
                String label = "vbarBeta_"+events.get(xi).name+"_"+k;
                vbarBeta[xi][k] = cplex.boolVar(label);
            }
        }*/


        theta = new IloNumVar[maxXi][][];
        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            theta[xi] = new IloNumVar[maxI][maxK];
            for(int i=0;i<maxI;i++){
                for(int k=0;k<maxK;k++){
                    String label = "theta_"+events.get(xi).name+"_"+i+"_"+k;
                    theta[xi][i][k] = cplex.boolVar(label);
                }
            }
        }

        phi = new IloNumVar[maxXi][2][][];
        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            phi[xi][0] = new IloNumVar[maxI][maxK];
            phi[xi][1] = new IloNumVar[maxI][maxK];
            for(int i=0;i<maxI;i++){
                for(int k=0;k<maxK;k++){
                    String label = "phi_"+events.get(xi).name+"_"+"0"+"_"+i+"_"+k;
                    phi[xi][0][i][k] = cplex.boolVar(label);
                    label = "phi_"+events.get(xi).name+"_"+"1"+"_"+i+"_"+k;
                    phi[xi][1][i][k] = cplex.boolVar(label);
                }
            }
        }

        gamma = new IloNumVar[maxXi][][];
        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            gamma[xi] = new IloNumVar[maxI][maxK];
            for(int i=0;i<maxI;i++){
                for(int k=0;k<maxK;k++){
                    String label = "gamma_"+events.get(xi).name+"_"+i+"_"+k;
                    gamma[xi][i][k] = cplex.boolVar(label);
                }
            }
        }

        kxi = new IloNumVar[maxXi][2][];
        for(int xi = 0;xi<maxXi;xi++){
            int maxI = nbExecutions.get(events.get(xi).toString());
            kxi[xi][0] = new IloNumVar[maxI];
            kxi[xi][1] = new IloNumVar[maxI];
            for(int i=0;i<maxI;i++){
                String label = "k_"+events.get(xi).name+"_"+"0"+"_"+i;
                kxi[xi][0][i] = cplex.intVar(0,maxK,label);
                label = "k_"+events.get(xi).name+"_"+"1"+"_"+i;
                kxi[xi][1][i] = cplex.intVar(0,maxK,label);
            }
        }

        // 约束 D constraints D : event cancellation ===================================================================
        for(int xi=0;xi<maxXi;xi++){
            Event exi = events.get(xi);
            if(exi.type.equals("positive") && exi.conditionsToCancel!=null){
                int maxI = nbExecutions.get(events.get(xi).toString());
                for(int k=1;k<maxK;k++){
                    IloLinearNumExpr constD5 = cplex.linearNumExpr();
                    for(ConditionToSchedule cs: exi.conditionsToCancel){
                        int s = stateVariables.indexOf(cs.stateVariable);
                        int a = cs.lowerBound;
                        int b = cs.upperBound;
                        double M;

                        {
                            IloLinearNumExpr constD1 = cplex.linearNumExpr();
                            M=a-stateVariables.get(s).lb;
                            constD1.addTerm(1,u[s][k]);
                            constD1.addTerm(-M,zbar[xi][k]);
                            cplex.addGe(constD1,a-M,"D1_"+events.get(xi).name+"_"+stateVariables.get(s).name+"_"+k);
                        }

                        {
                            IloLinearNumExpr constD2 = cplex.linearNumExpr();
                            M = stateVariables.get(s).ub-b;
                            constD2.addTerm(-1,u[s][k]);
                            constD2.addTerm(-M,zbar[xi][k]);
                            cplex.addGe(constD2,-b-M,"D2_"+events.get(xi).name+"_"+stateVariables.get(s).name+"_"+k);
                        }

                        {
                            IloLinearNumExpr constD3 = cplex.linearNumExpr();
                            M=stateVariables.get(s).ub-(a-1);
                            constD3.addTerm(-1,u[s][k]);
                            constD3.addTerm(-M,vbar[xi][s][0][k]);
                            cplex.addGe(constD3,-a+1-M,"D3_"+events.get(xi).name+"_"+stateVariables.get(s).name+"_"+k);
                        }

                        {
                            IloLinearNumExpr constD4 = cplex.linearNumExpr();
                            M = b+1-stateVariables.get(s).lb;
                            constD4.addTerm(1,u[s][k]);
                            constD4.addTerm(-M,vbar[xi][s][1][k]);
                            cplex.addGe(constD4,b+1-M,"D4_"+events.get(xi).name+"_"+stateVariables.get(s).name+"_"+k);
                        }

                        constD5.addTerm(1,vbar[xi][s][0][k]);
                        constD5.addTerm(1,vbar[xi][s][1][k]);

                    }

                    /*double M;
                    int s1 = stateVariables.indexOf(events.get(xi).countVariable);
                    {
                        IloLinearNumExpr constD20 = cplex.linearNumExpr();
                        M = 1;
                        constD20.addTerm(1,u[s1][k-1]);
                        constD20.addTerm(-M,zbar[xi][k]);
                        cplex.addGe(constD20,1-M,"D5_"+events.get(xi).name+"_"+k);
                    }

                    {
                        IloLinearNumExpr constD40 = cplex.linearNumExpr();
                        M=events.get(xi).nbParallelExecutions;
                        constD40.addTerm(-1,u[s1][k-1]);
                        constD40.addTerm(-M,vbarBeta[xi][k]);
                        cplex.addGe(constD40,-M,"D6_"+events.get(xi).name+"_"+k);
                    }*/

                    constD5.addTerm(1,zbar[xi][k]);
                    //constD5.addTerm(1,vbarBeta[xi][k]);
                    cplex.addGe(constD5,1,"D5_"+events.get(xi).name+"_"+k);
                }

                for(int i=0;i<maxI;i++){
                    {
                        IloLinearNumExpr constD6 = cplex.linearNumExpr();
                        constD6.addTerm(1,kxi[xi][1][i]);
                        for(int k=0;k<maxK;k++){
                            constD6.addTerm(-k,w[xi][i][k]);
                        }
                        cplex.addEq(constD6,0,"D6_"+events.get(xi).name);
                    }

                    //if(exi.nbParallelExecutions == 1){
                        IloLinearNumExpr constD9 = cplex.linearNumExpr();
                        constD9.addTerm(1,kxi[xi][0][i]);
                        for(int k=0;k<maxK;k++){
                            constD9.addTerm(-k,x[xi][i][k]);
                        }
                        cplex.addEq(constD9,0,"D7_"+events.get(xi).name);
                    /*}else{
                        for(int k=1;k<maxK;k++){
                            for(int i1 = 0;i1<maxI;i1++){
                                double M = k;
                                {
                                    IloLinearNumExpr constD7 = cplex.linearNumExpr();
                                    constD7.addTerm(1,kxi[xi][0][i]);
                                    constD7.addTerm(-M,y[xi][i1][i]);
                                    constD7.addTerm(-M,x[xi][i1][k]);
                                    cplex.addGe(constD7,k-2*M,"D7_"+events.get(xi).name+"_"+i+"_"+i1+"_"+k);
                                }

                                M = maxK;
                                {
                                    IloLinearNumExpr constD8 = cplex.linearNumExpr();
                                    constD8.addTerm(1,kxi[xi][0][i]);
                                    constD8.addTerm(M,y[xi][i1][i]);
                                    constD8.addTerm(M,x[xi][i1][k]);
                                    cplex.addLe(constD8,k+2*M,"D8_"+events.get(xi).name+"_"+i+"_"+i1+"_"+k);
                                }
                            }
                        }
                    }*/
                }

                for(int k=1;k<maxK;k++){
                    for(int i=0;i<maxI;i++){
                        double M;
                        {
                            M = maxK;
                            IloLinearNumExpr constD10 = cplex.linearNumExpr();
                            constD10.addTerm(k,zbar[xi][k]);
                            constD10.addTerm(-1,kxi[xi][0][i]);
                            constD10.addTerm(-M,theta[xi][i][k]);
                            cplex.addGe(constD10,1-M,"D8_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            M = maxK+1;
                            IloLinearNumExpr constD11 = cplex.linearNumExpr();
                            constD11.addTerm(1,kxi[xi][1][i]);
                            constD11.addTerm(-k,zbar[xi][k]);
                            constD11.addTerm(-M,theta[xi][i][k]);
                            cplex.addGe(constD11,1-M,"D9_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            M = maxK+1;
                            IloLinearNumExpr constD12 = cplex.linearNumExpr();
                            constD12.addTerm(1,kxi[xi][0][i]);
                            constD12.addTerm(-k,zbar[xi][k]);
                            constD12.addTerm(-M,phi[xi][0][i][k]);
                            cplex.addGe(constD12,-M,"D10_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            M = maxK;
                            IloLinearNumExpr constD13 = cplex.linearNumExpr();
                            constD13.addTerm(-1,kxi[xi][1][i]);
                            constD13.addTerm(k,zbar[xi][k]);
                            constD13.addTerm(-M,phi[xi][1][i][k]);
                            cplex.addGe(constD13,-M,"D11_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            IloLinearNumExpr constD14 = cplex.linearNumExpr();
                            constD14.addTerm(1,theta[xi][i][k]);
                            constD14.addTerm(1,phi[xi][0][i][k]);
                            constD14.addTerm(1,phi[xi][1][i][k]);
                            cplex.addGe(constD14,1,"D12_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            IloLinearNumExpr constD15 = cplex.linearNumExpr();
                            constD15.addTerm(1,gamma[xi][i][k]);
                            constD15.addTerm(-1,w[xi][i][k]);
                            for(int k1 = 0;k1<maxK;k1++){
                                constD15.addTerm(1,theta[xi][i][k1]);
                            }
                            cplex.addGe(constD15,0,"D13_"+events.get(xi).name+"_"+i+"_"+k);
                        }

                        {
                            M = maxK;
                            IloLinearNumExpr constD16 = cplex.linearNumExpr();
                            constD16.addTerm(1,w[xi][i][k]);
                            for(int k1=0;k1<maxK;k1++){
                                constD16.addTerm(-1,theta[xi][i][k1]);
                            }
                            constD16.addTerm(-M,gamma[xi][i][k]);
                            cplex.addGe(constD16,-M+1,"D14_"+events.get(xi).name+"_"+i+"_"+k);
                        }
                    }
                }
            }
            else {
                int maxI = nbExecutions.get(events.get(xi).toString());
                for(int i=0;i<maxI;i++){
                    for(int k=0;k<maxK;k++){
                        IloLinearNumExpr constD1 = cplex.linearNumExpr();
                        constD1.addTerm(1,w[xi][i][k]);
                        constD1.addTerm(-1,gamma[xi][i][k]);
                        cplex.addEq(constD1,0,"D1_"+events.get(xi).name+"_"+i+"_"+k);
                    }
                }

                /*for(int k=1;k<maxK;k++){
                    IloLinearNumExpr constD2 = cplex.linearNumExpr();
                    constD2.addTerm(1,zbar[xi][k]);
                    cplex.addEq(constD2,0,"D2_"+events.get(xi).name+"_"+k);
                }*/
            }
        }

        // 约束 E constraints E : evolution of state variables =========================================================
        HashSet<StateVariable> countVars = new HashSet<>();
        for(int xi=0;xi<maxXi;xi++){
            if(events.get(xi).type.equals("positive") && events.get(xi).conditionsToCancel != null){
                countVars.add(events.get(xi).countVariable);
                for(int k=1;k<maxK;k++){
                    int s = stateVariables.indexOf(events.get(xi).countVariable);

                    {
                        IloLinearNumExpr constE1 = cplex.linearNumExpr();
                        constE1.addTerm(1,u[s][0]);
                        cplex.addEq(constE1,initialState.get(stateVariables.get(s).toString()),"E1_"+stateVariables.get(s).name+"_"+(0));
                    }

                    {
                        IloLinearNumExpr constE2 = cplex.linearNumExpr();
                        constE2.addTerm(1, u[s][k]);
                        constE2.addTerm(events.get(xi).nbParallelExecutions, zbar[xi][k-1]);
                        cplex.addLe(constE2, events.get(xi).nbParallelExecutions, "E2_" + stateVariables.get(s).name + "_" + k);
                    }

                    int tildeXi =  0;
                    for(int l = 0;l<events.size();l++){
                        if(events.get(l)==events.get(xi).countEvent){
                            tildeXi = l;
                            break;
                        }
                    }

                    {
                        IloLinearNumExpr constE3 = cplex.linearNumExpr();
                        constE3.addTerm(1,u[s][k]);
                        constE3.addTerm(-1,u[s][k-1]);
                        int maxI = nbExecutions.get(events.get(xi).toString());
                        for(int i=0;i<maxI;i++){
                            constE3.addTerm(1,gamma[xi][i][k]);
                            constE3.addTerm(-1,gamma[tildeXi][i][k]);
                        }
                        constE3.addTerm(-events.get(xi).nbParallelExecutions, zbar[xi][k-1]);
                        cplex.addLe(constE3,0,"E3_" + stateVariables.get(s).name + "_" + k);
                    }

                    {
                        IloLinearNumExpr constE4 = cplex.linearNumExpr();
                        int maxI = nbExecutions.get(events.get(xi).toString());
                        constE4.addTerm(1,u[s][k]);
                        constE4.addTerm(-1,u[s][k-1]);
                        for(int i=0;i<maxI;i++){
                            constE4.addTerm(1,gamma[xi][i][k]);
                            constE4.addTerm(-1,gamma[tildeXi][i][k]);
                        }
                        constE4.addTerm(events.get(xi).nbParallelExecutions, zbar[xi][k-1]);
                        cplex.addGe(constE4,0,"E4_" + stateVariables.get(s).name + "_" + k);
                    }

                }
            }
        }

        for(int s=0;s<maxS;s++){

            {
                IloLinearNumExpr constD1 = cplex.linearNumExpr();
                constD1.addTerm(1,u[s][0]);
                cplex.addEq(constD1,initialState.get(stateVariables.get(s).toString()),"E1_"+stateVariables.get(s).name+"_"+(0));
            }

            if(!countVars.contains(stateVariables.get(s))){
                String svName = stateVariables.get(s).toString();
                for(int k=1;k<maxK;k++){

                    IloLinearNumExpr constD = cplex.linearNumExpr();
                    for(int xi=0;xi<maxXi;xi++){
                        int maxI = nbExecutions.get(events.get(xi).toString());
                        Event exi = events.get(xi);
                        int deltaxis;
                        if(exi.stateChanges.get(svName) == null) deltaxis=0;
                        else deltaxis= exi.stateChanges.get(svName);

                        for(int i=0;i<maxI;i++){
                            constD.addTerm(deltaxis,gamma[xi][i][k]);
                        }
                    }

                    constD.addTerm(1,u[s][k-1]);
                    constD.addTerm(-1,u[s][k]);
                    cplex.addEq(constD,0,"E1_"+stateVariables.get(s).name+"_"+k);
                }
            }
        }
    }

    @Override
    protected void printMilpResults(PrintWriter result) throws IloException {
        result.println("Sequence\tEvent\tExecutionTime\tState\tScheduleEvent\tCancelEvent");
        DecimalFormat df;
        df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        for(int k=0;k<maxK;k++) {
            result.print(k + "\t");
            int executionIndex=0;

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
            result.print(eventName + "\t");
            result.print(df.format(cplex.getValue(Epsilon[k])) + "\t");

            for(int s=0;s<maxS;s++)
                result.print((int) (cplex.getValue(u[s][k])+0.001)+"\t");

            for(int xi=0;xi<maxXi;xi++) {
                if (events.get(xi).type.equals("zero") && cplex.getValue(z[xi][k])>0.9999)
                    result.print(events.get(xi)+"\t");
                else if(events.get(xi).type.equals("positive")){
                    int maxI = nbExecutions.get(events.get(xi).toString());
                    for(int i = 0; i < maxI; i++){
                        if(cplex.getValue(x[xi][i][k])>0.9999){
                            result.print(events.get(xi)+"\t");
                            break;
                        }
                    }
                }
            }

            result.print("【");
            for(int xi=0;xi<maxXi;xi++) {
                if (k>0 && events.get(xi).type.equals("positive") && events.get(xi).conditionsToCancel!=null && cplex.getValue(zbar[xi][k])>0.9999)
                    result.print(events.get(xi)+"\t");
            }
            result.print("】");

            result.print("\t"+executionIndex);

            result.println();
        }

        /*result.println("========================= z ===========================================================");
        for(int xi=0;xi<maxXi;xi++){
            for(int k=0;k<maxK;k++){
                if(cplex.getValue(z[xi][k])>0.9){
                    result.print("[" + events.get(xi)+": " + k +"] ");
                }
            }
        }
        result.println();

        result.println("========================= gamma ===========================================================");
        for(int xi=0;xi<maxXi;xi++){
            for(int k=1;k<maxK;k++){
                int maxI = nbExecutions.get(events.get(xi).toString());
                for(int i=0;i<maxI;i++){
                    if(cplex.getValue(gamma[xi][i][k])>0.9){
                        result.print("[" + events.get(xi)+": " + k +"] ");
                    }
                }

            }
        }
        result.println();

        result.println("========================= w ===========================================================");
        for(int xi=0;xi<maxXi;xi++){
            for(int k=1;k<maxK;k++){
                int maxI = nbExecutions.get(events.get(xi).toString());
                for(int i=0;i<maxI;i++){
                    if(cplex.getValue(w[xi][i][k])>0.9){
                        result.print("[" + events.get(xi)+": " + k +"] ");
                    }
                }

            }
        }
        result.println();

        result.println("========================= theta ===========================================================");
        for(int xi=0;xi<maxXi;xi++){
            if(events.get(xi).type.equals("positive") && !events.get(xi).conditionsToCancel.isEmpty()){
                for(int k=1;k<maxK;k++){
                    int maxI = nbExecutions.get(events.get(xi).toString());
                    for(int i=0;i<maxI;i++){
                        if(cplex.getValue(theta[xi][i][k])>0.9){
                            result.print("[" + events.get(xi)+": " + k +"] ");
                        }
                    }
                }
            }
        }
        result.println();

        result.println("========================= zbar ===========================================================");
        for(int xi=0;xi<maxXi;xi++){
            for(int k=1;k<maxK;k++){
                if(cplex.getValue(zbar[xi][k])>0.9){
                    result.print("[" + events.get(xi)+": " + k +"] ");
                }
            }
        }*/
        result.println();
    }


}
