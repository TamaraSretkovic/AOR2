package rs.ac.bg.etf.aor.replacementpolicy;

import rs.ac.bg.etf.aor.memory.MemoryOperation;
import rs.ac.bg.etf.aor.memory.cache.CacheMemory;
import rs.ac.bg.etf.aor.memory.cache.ICacheMemory;
import rs.ac.bg.etf.aor.memory.cache.Tag;

import java.util.ArrayList;

public class RandomPseudoLRUReplacementPolicy implements IReplacementPolicy {

    protected ICacheMemory ICacheMemory;
    protected int setAsoc;
    protected int[] LRUCnts;
    protected int randomCnt;
    protected int randomSize;




    public RandomPseudoLRUReplacementPolicy(){
        LRUCnts = new int[1];
        LRUCnts[0] = 0;
        randomCnt=0;
        randomSize=0;
    }



    @Override
    public void init(ICacheMemory c) {
        this.ICacheMemory = c;
        setAsoc = (int) c.getSetAsociativity();
        int size = (int) ICacheMemory.getSetNum();

        randomSize=setAsoc/8;
        LRUCnts = new int[size * randomSize];

        for (int i = 0; i < size*randomSize; i++) {
            LRUCnts[i] = 0;
        }
    }


    private int getEntry(long adr){
        int set = (int) ICacheMemory.extractSet(adr);
        ArrayList<Tag> tagMemory = ICacheMemory.getTags();
        int result = 0;
        for (int i = 0; i < setAsoc; i++) {
            int block = set * setAsoc + i;
            Tag tag = tagMemory.get(block);
            if (!tag.V) {
                return i;
            }
        }
        int LRUCnt = LRUCnts[set * randomSize + randomCnt];

        if((LRUCnt&104)==104)return 0;
        if((LRUCnt&104)==96)return 1;
        if((LRUCnt&100)==68)return 2;
        if((LRUCnt&100)==64)return 3;
        if((LRUCnt&82)==18)return 4;
        if((LRUCnt&82)==16)return 5;
        if((LRUCnt&81)==1)return 6;
        if((LRUCnt&81)==0)return 7;

        return result;
    }


    @Override
    public int getBlockIndexToReplace(long adr) {
        randomCnt=(1 + randomCnt)%randomSize;
        int set = (int) ICacheMemory.extractSet(adr);
        return set * setAsoc + getEntry(adr);
    }

    @Override
    public void doOperation(MemoryOperation operation) {
        MemoryOperation.MemoryOperationType opr = operation.getType();

        if ((opr == MemoryOperation.MemoryOperationType.READ)
                || (opr == MemoryOperation.MemoryOperationType.WRITE)) {

            long adr = operation.getAddress();
            int set = (int) ICacheMemory.extractSet(adr);
            long tagTag = ICacheMemory.extractTag(adr);
            ArrayList<Tag> tagMemory = ICacheMemory.getTags();
            int entry = 0;
            for (int i = 0; i < setAsoc; i++) {
                int block = set * setAsoc + i;
                Tag tag = tagMemory.get(block);
                if (tag.V && (tag.tag == tagTag)) {
                    entry = i;
                    break;
                }
            }
            int LRUCnt = LRUCnts[set * randomSize + entry/8 ];
            LRUCnt = LRUCnt & 127;
            switch (entry % 8) {
                case 0:
                    LRUCnt = LRUCnt & 23;
                    break;
                case 1:
                    LRUCnt = (LRUCnt & 23) | 1;
                    break;
                case 2:
                    LRUCnt = (LRUCnt & 59) | 32;
                    break;
                case 3:
                    LRUCnt = (LRUCnt & 59) | 36;
                    break;
                case 4:
                    LRUCnt = (LRUCnt & 45) | 64;
                    break;
                case 5:
                    LRUCnt = (LRUCnt & 45) | 66;
                    break;
                case 6:
                    LRUCnt = (LRUCnt & 46) | 80;
                    break;
                case 7:
                    LRUCnt = (LRUCnt & 46) | 81;
                    break;
            }
            LRUCnts[set * randomSize + entry/8 ] = LRUCnt;

        } else if (operation.getType() == MemoryOperation.MemoryOperationType.FLUSHALL) {
            for (int i = 0; i < LRUCnts.length; i++) {
                LRUCnts[i] = 0;
            }

        }
    }

    @Override
    public String printValid() {
        return null;
    }

    @Override
    public String printAll() {
        String s = "";
        int size=(int) ICacheMemory.getSetNum();
        for (int i = 0; i <size; i++) {
            s = s + "Set " + i ;
            for(int j=0; j<randomSize;j++){
                s+=", Group "+j+" LRUcnt " + LRUCnts[i*setAsoc+j];
            }
            s+="\n";
        }
        return s;
    }

    @Override
    public void reset() {
        for (int i = 0; i < LRUCnts.length; i++) {
            LRUCnts[i] = 0;
        }
    }
}
