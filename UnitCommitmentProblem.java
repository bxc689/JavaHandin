/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handin2;

/**
 *
 * @author Magnus
 */
public class UnitCommitmentProblem {
    //first we declare our class variables
    private final int nGen;
    private final int nTime;
    private final double pLB[];
    private final double pUB[];
    private final double startupCost[];
    private final double commitmentCost[];
    private final double rampUp[];
    private final double rampDown[];
    private final int minUptime[];
    private final int minDowntime[];
    private final double prodCost[];
    private final double lShed[];
    private final double demand[];
    
    //then we make a constructor for the problem.
    public UnitCommitmentProblem(int nGen, int nTime, double[] pLB, double[] pUB, double[] startupCost, double[] commitmentCost, double[] rampUp, double[] rampDown, int[] minUptime, int[] minDowntime, double[] prodCost, double[] lShed, double[] demand) {
        this.nGen = nGen;
        this.nTime = nTime;
        this.pLB = pLB;
        this.pUB = pUB;
        this.startupCost = startupCost;
        this.commitmentCost = commitmentCost;
        this.rampUp = rampUp;
        this.rampDown = rampDown;
        this.minUptime = minUptime;
        this.minDowntime = minDowntime;
        this.prodCost = prodCost;
        this.lShed = lShed;
        this.demand = demand;
    }
            
    
    //we make a ger'er for every parameter in the problem.
    public int getnGen() {
        return nGen;
    }

    public int getnTime() {
        return nTime;
    }

    public double[] getpLB() {
        return pLB;
    }

    public double[] getpUB() {
        return pUB;
    }

    public double[] getStartupCost() {
        return startupCost;
    }

    public double[] getCommitmentCost() {
        return commitmentCost;
    }

    public double[] getRampUp() {
        return rampUp;
    }

    public double[] getRampDown() {
        return rampDown;
    }

    public int[] getMinUptime() {
        return minUptime;
    }

    public int[] getMinDowntime() {
        return minDowntime;
    }

    public double[] getProdCost() {
        return prodCost;
    }

    public double[] getlShed() {
        return lShed;
    }

    public double[] getDemand() {
        return demand;
    }
    
}
