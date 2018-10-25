/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handin2;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 *
 * @author Magnus
 */
public class FeasibilitySubProblem {
    // we declare the variable we will use in the class:
    private final IloCplex model;
    private final IloNumVar p[][];
    private final IloNumVar l[];
    private final IloNumVar v1[];
    private final IloNumVar v2[];
    private final IloNumVar v3[][];
    private final IloNumVar v4[][];
    private final IloNumVar v5[][];
    private final IloNumVar v6[][];
    private final UnitCommitmentProblem ucp;
    private final IloRange demandGeConstraints[];
    private final IloRange demandLeConstraints[];
    private final IloRange lowerBoundConstraints[][];
    private final IloRange upperBoundConstraints[][];
    private final IloRange rampUpConstraints[][];
    private final IloRange rampDownConstraints[][];
    
    public FeasibilitySubProblem(UnitCommitmentProblem ucp, double U[][]) throws IloException{
        //System.out.println("New feasibility problem made");
        this.ucp = ucp;
        
        //1. first we construct the model
        this.model = new IloCplex();

        //2. Then we initilize the variables
        // The vx-variables is the auxiliary vaiables
        this.l =  new IloNumVar[ucp.getnTime()];
        this.v1 =  new IloNumVar[ucp.getnTime()];
        this.v2 =  new IloNumVar[ucp.getnTime()];

        this.p = new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        this.v3 =  new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        this.v4 =  new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        this.v5 =  new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        this.v6 =  new IloNumVar[ucp.getnGen()][ucp.getnTime()];

        for(int t = 0; t< ucp.getnTime();t++){
            l[t] = model.numVar(0, Double.POSITIVE_INFINITY);
            v1[t] = model.numVar(0, Double.POSITIVE_INFINITY);
            v2[t] = model.numVar(0, Double.POSITIVE_INFINITY);
            for(int g = 0; g < ucp.getnGen();g++){
                p[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
                v3[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
                v4[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
                v5[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
                v6[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
            }
        }
        //3. We construct and add the objective function
        // first we make an empty linear expression to add the terms to
        IloLinearNumExpr obj = model.linearNumExpr();
        // the we add all the auxiliary vaiables to the objective function
        for(int t = 0; t< ucp.getnTime();t++){
            obj.addTerm(1, v1[t]);
            obj.addTerm(1, v2[t]);
             for(int g = 0; g < ucp.getnGen();g++){
                obj.addTerm(1, v3[g][t]);
                obj.addTerm(1, v4[g][t]);
                obj.addTerm(1, v5[g][t]);
                obj.addTerm(1, v6[g][t]);
             }
        }
        //and tell the model to minimize the objective.
        
        model.addMinimize(obj);
        // 4. Now it's time to construct and add the constraints:
        // We start with the demand constraint (1e), we split the equality into two
        // inequalities.
        
        // we initialize the IloRanges (constraints)
        this.demandGeConstraints = new IloRange[ucp.getnTime()];
        this.demandLeConstraints = new IloRange[ucp.getnTime()];
        
        //Then we for each set of sonctraints add the P-terms to the lhs numerical expression.
        for(int t= 0; t < ucp.getnTime();t++){
            IloLinearNumExpr lhs1 = model.linearNumExpr();
            IloLinearNumExpr lhs2 = model.linearNumExpr();
            for(int g = 0; g < ucp.getnGen();g++){
                lhs1.addTerm(1,p[g][t]);
                lhs2.addTerm(1,p[g][t]);
            }
            // then we add the l_t-terms
            lhs1.addTerm(1,l[t]);
            lhs2.addTerm(1,l[t]);
            //now we add our auxiliary vaiables 
            // the sign is based on the way of the inequality.
            lhs1.addTerm(1,v1[t]);
            lhs2.addTerm(-1,v2[t]);
            //finally we add the constraint to the model
            demandGeConstraints[t] = model.addGe(lhs1, ucp.getDemand()[t]);
            demandLeConstraints[t] = model.addLe(lhs2, ucp.getDemand()[t]);
        }
        
        //next us is the bound constraints (1f) and (1g)
        // again we initialize the Ranges that will hold the constraints
        this.lowerBoundConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        this.upperBoundConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        // we make a lhs for both set of constraints and add the p- and v-terms.
        for(int g = 0; g< ucp.getnGen();g++){
            for(int t = 0; t< ucp.getnTime();t++){
                IloLinearNumExpr lhs3 = model.linearNumExpr();
                IloLinearNumExpr lhs4 = model.linearNumExpr();
                lhs3.addTerm(1,p[g][t]);
                lhs4.addTerm(1,p[g][t]);
                lhs3.addTerm(1,v3[g][t]);
                lhs4.addTerm(-1,v4[g][t]);
                // and we add the constraints to the model:
                lowerBoundConstraints[g][t] =model.addGe(lhs3,ucp.getpLB()[g]*U[g][t]);
                upperBoundConstraints[g][t] =model.addLe(lhs4,ucp.getpUB()[g]*U[g][t]);
            }
        }
        // and lastly we add the ramp up/down constraints (1h) and (1i)
        // this is done in a way similair to above.
        this.rampUpConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        this.rampDownConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        for(int g = 0; g < ucp.getnGen();g++){
            for(int t = 0; t < ucp.getnTime();t++){
                IloLinearNumExpr lhs5 = model.linearNumExpr();
                IloLinearNumExpr lhs6 = model.linearNumExpr();
                lhs5.addTerm(1, p[g][t]);
                lhs6.addTerm(-1, p[g][t]);
                // to avoid gettin an index out of bould we do it for two cases:
                // the first for t=0 (where t-1 is out of bound) and for t > 0.
                if(t > 0){
                    lhs5.addTerm(-1, p[g][t-1]);
                    lhs6.addTerm(1, p[g][t-1]);
                }
                lhs5.addTerm(-1, v5[g][t]);
                lhs6.addTerm(-1, v6[g][t]);
                
                rampUpConstraints[g][t] = model.addLe(lhs5, ucp.getRampUp()[g]);
                rampDownConstraints[g][t] = model.addLe(lhs6,ucp.getRampDown()[g]);
            } 
        }
        
    }
    // we give the Feasibility a solve method
    public void solve() throws IloException{
        model.setOut(null); // dont give us solve prints
        model.solve(); // solve the model
    }
    
    // make a getter to get the objective fro the Feasibilitysubproblem:
    public double getObjective() throws IloException{
        return model.getObjValue();// get objective from model
    }
    
    // in the cut we have both a constant term (term without u) and one with u.
    // this method calculates the constant and returns it.
    // Note that we only use the constants where U is not a part.
    public double getCutConstant() throws IloException{
        double constant = 0;
        for(int t = 0; t < ucp.getnTime();t++){
            constant = constant + model.getDual(demandGeConstraints[t])*ucp.getDemand()[t];
            constant = constant + model.getDual(demandLeConstraints[t])*ucp.getDemand()[t];
            for(int g = 0; g< ucp.getnGen();g++){
                constant = constant + model.getDual(rampUpConstraints[g][t])*ucp.getRampUp()[g];
                constant = constant + model.getDual(rampDownConstraints[g][t])*ucp.getRampDown()[g];
            }
        }
        return constant;
    }
    
    // here we make the getter for the linear term needed to make the feasisibility cut,
    // we construct the cut and return it.
    // note that we only use the constraints where u is included.
    public IloLinearNumExpr getCutLinearTerm(IloIntVar u[][]) throws IloException{
        IloLinearNumExpr cutTerm = model.linearNumExpr();
        for(int t = 0; t < ucp.getnTime();t++){
            for(int g = 0; g < ucp.getnGen();g++){
                cutTerm.addTerm(model.getDual(lowerBoundConstraints[g][t])*ucp.getpLB()[g],u[g][t]);
                cutTerm.addTerm(model.getDual(upperBoundConstraints[g][t])*ucp.getpUB()[g],u[g][t]);
            }
        }return cutTerm;
        
    }
    // again we have and end() to realease the model when done.
    public void end(){
        model.end();
    }
    
}
