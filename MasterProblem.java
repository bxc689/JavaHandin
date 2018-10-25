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
import ilog.cplex.IloCplex;

/**
 *
 * @author Magnus
 */
public class MasterProblem {
    
    //define our variables:
    private final IloCplex model;
    private final IloIntVar u[][];
    private final IloNumVar c[][];
    private final IloNumVar phi;
    private final UnitCommitmentProblem ucp;
    
    //Here we make our Master Problem constructor that takes a UCP as input.
    public MasterProblem(UnitCommitmentProblem ucp) throws IloException {
        this.ucp = ucp;
        
        //1. we make the model
        this.model = new IloCplex();
        
        //2. we make variables for the masterproblem:
        //   - We let c_{g,t} and u_{g,t} be complicating variables
        this.u = new IloIntVar[ucp.getnGen()][ucp.getnTime()];
        this.c = new IloNumVar[ucp.getnGen()][ucp.getnTime()];
        
        // We make all our u's binary and c's positive.
        for(int g =0;g < ucp.getnGen() ; g++){
            for(int t = 0; t < ucp.getnTime(); t++){
                this.u[g][t] = model.boolVar(); // constraint (1k)
                this.c[g][t] = model.numVar(0, Double.POSITIVE_INFINITY); // constraint (1j)
            }
        }
        
        // we construct Phi for the master poblem as a positive numerical variable
        this.phi = model.numVar(0,Double.POSITIVE_INFINITY);
        
        // 3. we make the objective funtion for the master problem 
        IloLinearNumExpr obj = model.linearNumExpr(); //Make an empty linear expression 
        for(int g =0;g < ucp.getnGen() ; g++){
            for(int t = 0; t < ucp.getnTime(); t++){
                obj.addTerm(1,c[g][t]); //add the c_{g,t} terms
                obj.addTerm(ucp.getCommitmentCost()[g],u[g][t]); // add the C_g^C*u_{g,t} terms
            }
        }
        obj.addTerm(1,phi);// add Phi to the objective
        
        model.addMinimize(obj); // tell Cplex that we want to minimize the objective we just made.
        
        //4. We make and add constraints to the master problem
        // - THe first constraints we add is startUpCost constraints (1b)
        for(int g =0;g < ucp.getnGen() ; g++){
            for(int t = 0; t < ucp.getnTime(); t++){
                //For all g,t er make an empty linear expession to store the LHS in.
                IloLinearNumExpr rhs = model.linearNumExpr();
                
                // We split this into 2 cases: for t = 0 and t > 0.
                // this is to avoid calling u[g][t-1] that would be out of bound.
                if(t == 0){
                    rhs.addTerm(ucp.getStartupCost()[g], u[g][t]);
                    model.addGe(c[g][t],rhs); // add the constraint to model
                }else{
                    rhs.addTerm(ucp.getStartupCost()[g], u[g][t]);
                    rhs.addTerm((-1)*ucp.getStartupCost()[g], u[g][t-1]);
                    model.addGe(c[g][t],rhs);// add the constraint to model
                }
            }
        }
        // next constraints we add is the minimum uptime constraints (1c) 
        for(int g =0;g < ucp.getnGen() ; g++){
            for(int t = 0; t < ucp.getnTime(); t++){
                // we make a variable Tu to store T^U_{g,t} and define it as in the formulation.
                int Tu;
                if(t+ucp.getMinUptime()[g]-1 - (ucp.getnTime()-1)>0.5){
                    Tu = ucp.getnTime()-1;
                }else{
                    Tu = t+ucp.getMinUptime()[g]-1;
                }
                //make an empty expression to store terms in:
                IloLinearNumExpr lhs = model.linearNumExpr();
                //here we make dif, which count how many times we wil have the 
                // u_{g,t} and u_{g,t-1} terms.
                int dif = Tu-t;
                
                // again we split it into 2 cases to avoid getting out of bound of index.
                if(t == 0){
                    // we sum from t to Tu and add the terms
                    for(int i = t; i<= Tu; i++){
                        lhs.addTerm(1,u[g][i]);
                    }
                    lhs.addTerm(-dif,u[g][t]); // subtact - diff times u_{g,t}
                    model.addGe(lhs,0); // add constraint to the model.
                }else{
                    // here we do the same as for t=0 we just also add the 
                    // 'u_{g,t-1}'-term to the constraint.
                    for(int i = t; i<= Tu; i++){
                        lhs.addTerm(1,u[g][i]);
                    }
                    lhs.addTerm(-dif,u[g][t]);
                    lhs.addTerm(dif,u[g][t-1]);
                    model.addGe(lhs,0);
                }
            }
            
        }
        // Now we add the minimum downtime constraints (1d)
        // This is done similairly to (1c) above.
        for(int g =0;g < ucp.getnGen() ; g++){
            for(int t = 0; t < ucp.getnTime(); t++){
                int Td;
                if(t+ucp.getMinDowntime()[g]-1 - (ucp.getnTime()-1)>0.5){
                    Td = ucp.getnTime()-1;
                }else{
                    Td = t+ucp.getMinDowntime()[g]-1;
                }
                IloLinearNumExpr lhs = model.linearNumExpr();
                
                int dif = Td-t;
                
                if(t == 0){
                    for(int i = t; i<= Td; i++){
                        lhs.addTerm(-1,u[g][i]);
                    }
                    lhs.addTerm(dif,u[g][t]);
                    model.addGe(lhs,-dif);
                }else{
                    for(int i = t; i<= Td; i++){
                        lhs.addTerm(-1,u[g][i]);
                    }
                    lhs.addTerm(dif,u[g][t]);
                    lhs.addTerm(-dif,u[g][t-1]);
                    model.addGe(lhs,-dif);
                }
            }
            
        }
        
    }
    
    // we make a solve metod for the Master problem
    public void solve() throws IloException{
        // we tell the model to use the callback metod so we can do Benders 
        // decomposition.
        model.use(new Callback());  
        // we solve the model:
        model.solve();
    }
    
    //We make the callback class that will do the decomposition each node.
    private class Callback extends IloCplex.LazyConstraintCallback{
        //First we override the callback function:
        public Callback(){
            //empty
        }
        //Now we make the main that is called when ever we find a solution in a node.
        protected void main() throws IloException{
            // we optain the solution from the master problem:
            double[][] U = getU();
            double Phi = getPhi();
            // note that we don't need to get C since it will not be used.
            
            // here we construct our FeasibilitySubProblem for the current problem and solution U
            FeasibilitySubProblem fsp = new FeasibilitySubProblem(ucp,U);
            
            // solve the problem:
            fsp.solve();
            
            // we get the objective from the feasibility sub problem
            double fspObjective = fsp.getObjective();
            
            //and print it:
            System.out.println("FSP obj: "+fspObjective);
            
            // Now we test if the objevtive is positive, if this is the case
            // our MP solution would lead to in infeasible second stage problem.
            if(fspObjective >= 0+1e-6){ //If x not in X_k
                //If the objective is positive we add a feasibility cut:
                System.out.println("Generating feasibility cut");
                // we get the constant part and the linearterms for the cut
                double constant = fsp.getCutConstant();
                IloLinearNumExpr linearTerm = fsp.getCutLinearTerm(u);
                // and add the cut (constraint) to the master problem.
                add(model.le(linearTerm, -constant));// add lazy constraint 
            }else{ //If x in X_k
                //if the objective is 0, we know the solution leads to a 
                // feasible second stage problem. Now we check if the solution 
                // is optimal.
                
                // we generate the optimalitySubProblem given the ucp and mp solution.
                OptimalitySubProblem osp = new OptimalitySubProblem(ucp,U);
                // and solve the optimalitySubProblem
                osp.solve();
                
                // we get the objevtive for the optimality sub problem
                double ospObjective = osp.getObjective();
                System.out.println("Phi :"+Phi+" OSP Obj :"+ospObjective);
                //Now we check if Phi >= objective of the optimality sub problem,
                // if it is then we have an optimal solution the the problem
                if(Phi >= ospObjective-1e-6){
                    //if we get to this point, then we have foind the optimal
                    // solution for the current node.
                    System.out.println("Currtent node is optimal");
                }else{
                    //if phi is not greater than the objective of the optimality 
                    //sub problem, then we ad an optimaltiy cut.
                    System.out.println("Generating optimality cut");
                    
                    // we get the constant and linear term for the optimality cut
                    double cutConstant = osp.getCutConstant();
                    IloLinearNumExpr cutTerm = osp.getCutLinearTerm(u);
                    // we add -phi to the terms
                    cutTerm.addTerm(-1, phi);
                    
                    //Prints to check cuts:
                    //System.out.println("opt cut terms "+cutTerm);
                    //System.out.println("opt cut constant "+cutConstant);
                    
                    //and the we add the cut (constraint) to the master problem.
                    add(model.le(cutTerm,-cutConstant)); // add constraint to MP in the lazy way.
                }
            }
          
        //System.out.println("End of callback");
        }
        
        // we make a 'getter' to get the values of U from the master problem
        public double[][] getU() throws IloException{
            double U[][] = new double[ucp.getnGen()][ucp.getnTime()];
            for(int g= 0; g < ucp.getnGen(); g++){
                for(int t = 0; t < ucp.getnTime();t++){
                    U[g][t] = getValue(u[g][t]);
                }
            }
            return U;
        }
        
        // we make a 'getter' to get the values of U from the master problem
        // this one is not used.
        public double[][] getC() throws IloException{
            double C[][] = new double[ucp.getnGen()][ucp.getnTime()];
            for(int g= 0; g < ucp.getnGen(); g++){
                for(int t = 0; t < ucp.getnTime();t++){
                    C[g][t] = getValue(c[g][t]);
                }
            }
            return C;
        }
        
        // make a getter to get Phi fromt the master problem
        public double getPhi() throws IloException{
            return getValue(phi);
        }
        
    }
    // make getter to return the objective value of the master problem
    public double getObjective() throws IloException {
        return model.getObjValue();
    }
    // make a method to print the solution to the mater problem (U)
    public void printSolution() throws IloException {
        System.out.print("\n"+"u(g,t):"+"\n");
        int uRound = 0;
        for(int g = 0; g < ucp.getnGen() ; g++){
            for( int t = 0; t < ucp.getnTime(); t++){
                if(model.getValue(u[g][t]) < 0.5){
                    uRound = 0;
                }else{
                    uRound = 1;
                }
                
                
                System.out.print(uRound+" ");
            }
            System.out.print("\n");
        }
    }
    
    // make an end metod to release the model when done.
    public void end(){
        model.end();
    }
}
