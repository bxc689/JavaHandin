/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handin2;

import ilog.concert.IloException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 * @author Magnus
 */
public class Main {
    public static void main(String[] args) throws FileNotFoundException, IloException {
        // here we get the data needed to model thhe specefic problem.
        int nGen = 31; // number of generators
        int nTime = 24; // number of time points (one per hour)
        
        //here we make the vectors to store parameters
        //Genetartor parameters:
        String[] genNames = new String[nGen]; // generator names - Not used
        double[] pLB = new double[nGen]; //lower production bould
        double[] pUB = new double[nGen]; //upper production bound
        double[] startupCost = new double[nGen]; // startup cost
        double[] commitmentCost = new double[nGen]; // commitment cost
        double[] rampUp = new double[nGen]; // ramping up limit
        double[] rampDown = new double[nGen]; // ramping down limit
        int[] minUptime = new int[nGen];  // minimum uptime 
        int[] minDowntime = new int[nGen]; // minimum downtime
        double[] prodCost = new double[nGen]; // marginal production cost
        // Time parameters:
        double[] lShed = new double[nTime];
        double[] demand = new double[nTime];
        
        //we make f the data file we want to scan, and make a scanner for it:
        File f = new File("generators.txt");
        Scanner s = new Scanner(f);
        
        //for each generator in the data, we run through all its paramets.
        //NOTE: some double appeared as string in the file, this is why 
        // Double.parseDouble(<string>) is used.
        for(int i= 0; i <nGen ;i++){
            s.nextLine();
            genNames[i]=s.next();
            pLB[i] = Double.parseDouble(s.next());
            pUB[i] = s.nextDouble();
            startupCost[i] = Double.parseDouble(s.next());
            commitmentCost[i] = s.nextDouble();
            rampUp[i] = s.nextDouble();
            rampDown[i] = rampUp[i];
            minUptime[i] = s.nextInt();
            minDowntime[i] = s.nextInt();
            prodCost[i] = Double.parseDouble(s.next());

        }
        //here we do the same for the loads file
        File f1 = new File("loads.txt");
        Scanner s1 = new Scanner(f1);
        //and go through and save the demand for every time step.
        for(int i=0;i<nTime;i++){
        s1.nextLine();
        demand[i] = s1.nextDouble();
        lShed[i] = 46;
        
        }
        //Here we initialize your problem by inputting all the data for the problem in the constructor:
        UnitCommitmentProblem ucp = new UnitCommitmentProblem(nGen,nTime,pLB, pUB,startupCost, commitmentCost,rampUp,rampDown,minUptime,minDowntime,prodCost,lShed,demand);
        
        //Here we make the masterproblem for the given UnitCommitmentProblem.
        MasterProblem mp = new MasterProblem(ucp);
        //we solve the master problem
        mp.solve();
        // return the objective for the solution
        System.out.println("optimal Denders objective value ="+mp.getObjective());
        // and print the solution u(g,t)
        mp.printSolution();
        
        System.out.println("The run is complete");
    }
}
