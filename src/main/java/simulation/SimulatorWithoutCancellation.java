package simulation;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;

public class SimulatorWithoutCancellation extends Simulator {

    public SimulatorWithoutCancellation() throws IloException {
    }

    @Override
    protected void buildMilpModel() throws IloException{
        super.buildMilpModel();

        // 约束 E constraints E : evolution of state variables =========================================================
        for(int s=0;s<maxS;s++){
            String svName = stateVariables.get(s).toString();

            IloLinearNumExpr constD1 = cplex.linearNumExpr();
            constD1.addTerm(1,u[s][0]);
            cplex.addEq(constD1,initialState.get(stateVariables.get(s).toString()),"E1_"+stateVariables.get(s).name+"_"+(0));

            for(int k=1;k<maxK;k++){
                IloLinearNumExpr constD = cplex.linearNumExpr();
                for(int xi=0;xi<maxXi;xi++){
                    int maxI = nbExecutions.get(events.get(xi).toString());
                    Event exi = events.get(xi);
                    int deltaxis;
                    if(exi.stateChanges.get(svName) == null) deltaxis=0;
                    else deltaxis= exi.stateChanges.get(svName);

                    for(int i=0;i<maxI;i++){
                        constD.addTerm(deltaxis,w[xi][i][k]);
                    }
                }

                constD.addTerm(1,u[s][k-1]);
                constD.addTerm(-1,u[s][k]);
                cplex.addEq(constD,0,"E1_"+stateVariables.get(s).name+"_"+k);
            }
        }
    }


}
