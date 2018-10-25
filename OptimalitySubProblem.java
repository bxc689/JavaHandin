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
public class OptimalitySubProblem{
    // we declare the variables we intent to use in the class.
    private final IloCplex model;
    private final IloNumVar p[][];
    private final IloNumVar l[];
    private final UnitCommitmentProblem ucp;
    private final IloRange demandGeConstraints[];
    private final IloRange demandLeConstraints[];
    private final IloRange lowerBoundConstraints[][];
    private final IloRange upperBoundConstraints[][];
    private final IloRange rampUpConstraints[][];
    private final IloRange rampDownConstraints[][];
    
    // we make the constructor  for the Optimalitysubproblem given a ucp and a solution U to the master problem.
    public OptimalitySubProblem(UnitCommitmentProblem ucp, double U[][]) throws IloException{
        //System.out.println("New optimality problem made");
        this.ucp = ucp;
        
        //1. First we makethe model
        this.model = new IloCplex();
        
        //2. we initialize the variables
        this.p = new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        this.l =  new IloNumVar[ucp.getnTime()];
        
        // say that both l and p are positve constraits (1l) and (1m)
        for(int t = 0; t< ucp.getnTime();t++){
            l[t] = model.numVar(0, Double.POSITIVE_INFINITY);
            for(int g = 0; g < ucp.getnGen();g++){
                p[g][t] = model.numVar(0, Double.POSITIVE_INFINITY);
            }
        }
        //3. we construct and add the objective function
        // make empty obj to add terms to
        IloLinearNumExpr obj = model.linearNumExpr();
        // we add the terms L_t*l_t and C^P_g*p_{g,t}
        for(int t = 0; t< ucp.getnTime();t++){
            obj.addTerm(ucp.getlShed()[t] , l[t]);
             for(int g = 0; g < ucp.getnGen();g++){
                obj.addTerm(ucp.getProdCost()[g] , p[g][t]);
             }
        }
        // tell the model that we want to minimize the object function.
        model.addMinimize(obj);
        
        //4. we construct and add the constraints to the model.
        // start with demand constraints (1e)
        // just as in the Feasibilitysubproblem we split the equality into two
        // inequalities.
        
        // initilize constraints
        this.demandGeConstraints = new IloRange[ucp.getnTime()];
        this.demandLeConstraints = new IloRange[ucp.getnTime()];
        for(int t= 0; t < ucp.getnTime();t++){
            IloLinearNumExpr lhs1 = model.linearNumExpr(); // make emtrp LHS
            for(int g = 0; g < ucp.getnGen();g++){
                lhs1.addTerm(1,p[g][t]); // add p- terms
            }
            lhs1.addTerm(1,l[t]); // add l-terms
            // add the constraints to the model.
            demandGeConstraints[t] = model.addGe(lhs1, ucp.getDemand()[t]);
            demandLeConstraints[t] = model.addLe(lhs1, ucp.getDemand()[t]);
        }
        
        // we construct and add the bound constraints:
        this.lowerBoundConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        this.upperBoundConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        for(int g = 0; g< ucp.getnGen();g++){
            for(int t = 0; t< ucp.getnTime();t++){
                IloLinearNumExpr lhs2 = model.linearNumExpr();
                lhs2.addTerm(1,p[g][t]);
                lowerBoundConstraints[g][t] =model.addGe(lhs2,ucp.getpLB()[g]*U[g][t]);
                upperBoundConstraints[g][t] =model.addLe(lhs2,ucp.getpUB()[g]*U[g][t]);
            }
        }
        // and lastly we add the ramp up and down constraints:
        this.rampUpConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        this.rampDownConstraints = new IloRange[ucp.getnGen()][ucp.getnTime()];
        // here we look at two cases t = 0 and t > 0 to avoid inxex out of bound.
        for(int g = 0; g < ucp.getnGen();g++){
            for(int t = 0; t < ucp.getnTime();t++){
                IloLinearNumExpr lhs3 = model.linearNumExpr();
                IloLinearNumExpr lhs4 = model.linearNumExpr();
                lhs3.addTerm(1, p[g][t]);
                lhs4.addTerm(-1, p[g][t]);
                if(t > 0){
                    lhs3.addTerm(-1, p[g][t-1]);
                    lhs4.addTerm(1, p[g][t-1]);
                }
                
                rampUpConstraints[g][t] = model.addLe(lhs3, ucp.getRampUp()[g]);
                rampDownConstraints[g][t] = model.addLe(lhs4,ucp.getRampDown()[g]);
            } 
        }
        
    }

    // add solve method
    public void solve() throws IloException{
        model.setOut(null);
        model.solve();
    }
    
    // add a getter for the objective of the optimality sub problem
    public double getObjective() throws IloException{
        return model.getObjValue();
    }
    
    // we make a getter that calculates and gets the constant par of the cut.
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
    
    // we make a getter that constructs the linearterm part of the cut ( the part that includes u_{g,t})
    public IloLinearNumExpr getCutLinearTerm(IloIntVar u[][]) throws IloException{
        IloLinearNumExpr cutTerm = model.linearNumExpr();
        for(int t = 0; t < ucp.getnTime();t++){
            for(int g = 0; g < ucp.getnGen();g++){
                //Print to check cuts:
                //System.out.println("dual consts :"+model.getDual(lowerBoundConstraints[g][t])+ " plb "+ ucp.getpLB()[g]);
                //System.out.println("dual consts :"+model.getDual(upperBoundConstraints[g][t])+ " pub "+ ucp.getpUB()[g]);
                cutTerm.addTerm(model.getDual(lowerBoundConstraints[g][t])*ucp.getpLB()[g],u[g][t]);
                cutTerm.addTerm(model.getDual(upperBoundConstraints[g][t])*ucp.getpUB()[g],u[g][t]);
            }
        }
        return cutTerm;
    }
    
    
    //Make a end() method to release the model when done.
    public void end(){
        model.end();
    }
    
}
