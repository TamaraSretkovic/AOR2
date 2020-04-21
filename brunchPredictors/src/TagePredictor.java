package rs.ac.bg.etf.predictor.tage;

import rs.ac.bg.etf.automaton.Automaton;
import rs.ac.bg.etf.predictor.BHR;
import rs.ac.bg.etf.predictor.Instruction;
import rs.ac.bg.etf.predictor.Predictor;

public class TagePredictor implements Predictor {

    protected class Entry {
        Automaton aut;
        int tag = 0;
        int U;

        public Entry(Automaton.AutomatonType type) {
            //tag=t;
            aut = Automaton.instance(type);
            U = 0;
        }

        public void setTag(int t) {
            tag = t;
            U = 0;
        }

        public void incU() {
            U++;
        }

        public void decU() {
            U--;
        }

        public int getTag() {
            return tag;
        }

        public Automaton getAut() {
            return aut;
        }

        public void setAut(Automaton a) {
            aut = a;
        } // da li treba novi ili je ovo ok? kako da ima podesim isto stanje??

        public int getU() {
            return U;
        }

        public void setU(int valid) {
            U = valid;
        }
    }

    protected class Table {
        int tableSize;
        Entry[] entries;
        int full = 0;

        public Table(int size) {
            tableSize = size;
            entries = new Entry[size];
            for (int i = 0; i < size; i++) entries[i] = new Entry(type);
        }

        public boolean setEntry(int tag, boolean outcome) {
            if (full == tableSize) {
                for (int i = 0; i < tableSize; i++) {
                    if (entries[i].getU() <= 0) {
                        entries[i].setTag(tag);
                        entries[i].setU(0);
                        entries[i].getAut().updateAutomaton(outcome);

                        return true;
                    }
                }
            } else {
                entries[full].setTag(tag);
                entries[full].setU(0);
                entries[full].getAut().updateAutomaton(outcome);
                full++;
                return true;

            }
            return false;
        }


        public void decrementU() {
            for (int i = 0; i < tableSize; i++) {
                entries[i].decU();
            }
        }

        public int getFull() {
            return full;
        }

        public Entry findEntry(int t) {
            for (int i = 0; i < tableSize; i++) {
                if (entries[i].getTag() == t) {
                    return entries[i];
                }
            }
            return null;
        }
    }

    Table[] tables;
    Automaton[] T0;
    BHR bhr;
    Automaton.AutomatonType type;
    int[] L;
    int mask;
    int provider;
    int altProvider;


    public TagePredictor(Automaton.AutomatonType t, int bhrSize, int numLastbit, int tSize, int numTable, int l0, int alfa) {
        type = t;
        mask = (1 << numLastbit) - 1;
        bhr = new BHR(bhrSize);
        T0 = Automaton.instanceArray(t, 1 << numLastbit);
        tables = new Table[numTable];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = new Table(tSize);
        }

        L = new int[numTable];
        L[0] = l0;
        for (int i = 1; i < L.length; i++)
            L[i] = (int) ((Math.pow(alfa, i)) * l0 + 0.5);

    }


    @Override
    public boolean predict(Instruction branch) {
        int tagg=0;
        int address  = (int) branch.getAddress();
        boolean predict = false;
        provider = -1;
        altProvider = -1;

        for (int i = tables.length - 1; i >= 0; i--) {
            if(tables[i].getFull()==0) continue;
            int bhrL = (1 << L[i]) - 1;
            int bhrV=bhr.getValue();
            Entry e;
            tagg=((bhrV & bhrL) ^ address );
            Integer tag=new Integer(tagg);

            e = tables[i].findEntry(tag.hashCode());
            if (e != null) {
                provider = i;
                predict = e.getAut().predict();
                for (int j = i - 1; j >= 0; j--) {
                    bhrL = (1 << L[j]) - 1;
                    Entry e2;
                    tagg= (address ^ (bhrV & bhrL));
                    Integer tag2=new Integer(tagg);
                    e2 = tables[j].findEntry(tag2.hashCode());
                    if (e2 != null) {
                        altProvider = j;
                        break;
                    }
                }
                break;
            }
        }
        if (provider == -1) predict = T0[address & mask].predict();

        return predict;
    }

    @Override
    public void update(Instruction branch) {

        int addr = (int) branch.getAddress();
        Integer tag1 = 0, tag2 = 0;
        if (provider != -1) {
            tag1 = tag1.hashCode(addr ^ (bhr.getValue() & ((1 << L[provider]) - 1)));
            Entry providerEntry = tables[provider].findEntry(tag1);
            boolean provPredict = providerEntry.getAut().predict();
            boolean altPredict = false;

            if (altProvider != -1) {
                tag2 = tag2.hashCode(addr ^ (bhr.getValue() & ((1 << L[altProvider]) - 1)));
                altPredict = tables[altProvider].findEntry(tag2).getAut().predict();
            } else {
                altPredict = T0[addr & mask].predict();
            }

            if (provPredict != altPredict) {
                if (provPredict == branch.isTaken()) {
                    providerEntry.incU();
                } else {
                    providerEntry.decU();
                    boolean fin = false;
                    int tableSize = tables[provider].tableSize;
                    for (int i = provider + 1; i < tables.length; i++) { //first pass

                        if ((i < tables.length - 1) && tables[i].getFull() == tableSize && tables[i + 1].getFull() <= tableSize)
                            continue;
                        if(tables[i].getFull()==tableSize) continue;
                        fin = tables[i].setEntry(tag1.hashCode(addr ^ (bhr.getValue() & ((1 << L[i]) - 1))), branch.isTaken());
                        if (fin == true) break;
                    }
                    if (fin == false) {
                        for (int i = provider + 1; i < tables.length; i++) { //second pass
                            fin = tables[i].setEntry(tag1.hashCode(addr ^ (bhr.getValue() & ((1 << L[i]) - 1))), branch.isTaken());
                            if (fin == true) break;
                        }
                    }
                    if (fin == false)
                        for (int i = 0; i < tables.length; i++) tables[i].decrementU();
                }
            }
            providerEntry.getAut().updateAutomaton(branch.isTaken());
        } else {
            T0[addr & mask].updateAutomaton(branch.isTaken());
            boolean fin = false;
            int tableSize = tables[0].tableSize;
            for (int i = 0; i < tables.length; i++) { //first pass
                if ((i < tables.length - 1) && tables[i].getFull() == tableSize && tables[i + 1].getFull() <= tableSize)
                    continue;
                if(tables[i].getFull()==tableSize) continue;
                tag1 = tag1.hashCode(addr ^ (bhr.getValue() & ((1 << L[i]) - 1)));
                fin = tables[i].setEntry(tag1, branch.isTaken());
                if (fin == true) {
                    break;
                }
            }
            if (fin == false) {  //second pass
                for (int i = 0; i < tables.length; i++) {
                    tag1 = tag1.hashCode(addr ^ (bhr.getValue() & ((1 << L[i]) - 1)));
                    fin = tables[i].setEntry(tag1, branch.isTaken());
                    if (fin == true) {
                        break;
                    }
                }
            }
            if (fin == false)
                for (int i = 0; i < tables.length; i++) tables[i].decrementU();
        }
        bhr.insertOutcome(branch.isTaken());

    }
}