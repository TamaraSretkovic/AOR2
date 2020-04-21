package rs.ac.bg.etf.predictor.yags;

import rs.ac.bg.etf.automaton.Automaton;
import rs.ac.bg.etf.predictor.BHR;
import rs.ac.bg.etf.predictor.Instruction;
import rs.ac.bg.etf.predictor.Predictor;

import java.util.ArrayList;

//import java.lang.reflect.Array;

public class YagsPredictor implements Predictor {
    protected class Entry{
        Automaton aut;
        int tag;
        boolean valid=false;

        public Entry(int tag, Automaton.AutomatonType type){
            this.tag=tag;
            aut=Automaton.instance(type);
            valid=false;
        }

        public Automaton getAut() {
            return aut;
        }

        public int getTag() {
            return tag;
        }
        public boolean getValid(){
            return valid;
        }
        public void setTag(int t){tag=t;}
        public void setValid(boolean v){valid=v;}
    }

     ArrayList<Entry> cacheTaken, cacheNotTaken;
     Automaton[] selector;

     Automaton.AutomatonType type;
     BHR bhr;
     int mask;
     int cacheSize;
     int cacheMask;
     int tagMask;
     int predictionUsed;



     public YagsPredictor(int bhrSize, int numOfLastBitsAddress, int cacheSize, int tagSize, Automaton.AutomatonType t){
         type=t;
         bhr=new BHR(bhrSize);
         int selectorSize = 1 << (numOfLastBitsAddress);
         mask = selectorSize - 1;
         selector = Automaton.instanceArray(type, selectorSize);
         cacheMask=(1<<(cacheSize))-1;
         cacheTaken=new ArrayList<>(cacheSize);
         cacheNotTaken=new ArrayList<>(cacheSize);
         this.cacheSize=1<<(cacheSize);
         for (int i = 0; i < this.cacheSize; i++) {
             cacheTaken.add(new Entry(0,t));
             cacheNotTaken.add(new Entry(0,t));
         }
         tagMask = (1 << (tagSize)) - 1;

     }

    @Override
    public boolean predict(Instruction branch) {

        boolean selectorPrediction=selector[(int)(branch.getAddress()&mask)].predict();
        if(selectorPrediction==true){
            Entry en=cacheNotTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            if(en.getValid()==true && en.getTag()==(branch.getAddress()&tagMask)){
                predictionUsed=2;
                return en.aut.predict();}
        }
        else {
            Entry en=cacheTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            if(en.getValid()==true&&en.getTag()==(branch.getAddress()&tagMask)){
                predictionUsed=3;
                return en.aut.predict();}
        }
         predictionUsed=1;
         return selectorPrediction;
    }

    @Override
    public void update(Instruction branch) {
        boolean outCome=branch.isTaken();

        //za notTaken
        if(predictionUsed==2){
            Entry en=cacheNotTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            en.aut.updateAutomaton(outCome);
        }
        else if(predictionUsed==1 && outCome==false && selector[(int)(branch.getAddress()&mask)].predict()==true){
            Entry en=cacheNotTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            if(en.valid==true)
            en.aut.updateAutomaton(outCome);
            else{
                en.setTag((int)(branch.getAddress()^bhr.getValue())&tagMask);
                en.setValid(true);
                cacheNotTaken.set((int)((branch.getAddress()^bhr.getValue())&cacheMask),en);
            }
        }
        //za Taken
        if(predictionUsed==3){
            Entry en=cacheTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            en.aut.updateAutomaton(outCome);
        }
        else if(predictionUsed==1 && outCome==true && selector[(int)(branch.getAddress()&mask)].predict()==false){
            Entry en=cacheTaken.get((int)((branch.getAddress()^bhr.getValue())&cacheMask));
            if(en.valid==true)
                en.aut.updateAutomaton(outCome);
            else{
                en.setTag((int)(branch.getAddress()^bhr.getValue())&tagMask);
                en.setValid(true);
                cacheTaken.set((int)((branch.getAddress()^bhr.getValue())&cacheMask),en);
            }
        }
        //za selector
        selector[(int)(branch.getAddress()&mask)].updateAutomaton(outCome);

        bhr.insertOutcome(outCome);

    }
}
