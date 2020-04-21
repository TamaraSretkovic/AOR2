package rs.ac.bg.etf.aor.loader;

import rs.ac.bg.etf.aor.memory.MemoryOperation;
import rs.ac.bg.etf.aor.parser.Instruction;
import rs.ac.bg.etf.aor.parser.MyXzTraceParser.MyXZTraceInstruction;
import rs.ac.bg.etf.aor.parser.MyXzTraceParser.MyXZTraceParser;
import rs.ac.bg.etf.aor.parser.Parser;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class MyXZLoader implements ITraceLoader, Closeable {


    MyXZTraceParser parser;
    String fileName;

    MyXZTraceInstruction currentInstruction;
    MyXZTraceInstruction nextInstruction;


    LinkedList<MemOp> operations;

    private class MemOp{

        String operation;
        long adress;

        public MemOp(String operation, long adress){
            this.operation = operation;
            this.adress = adress;
        }

    }

    public MyXZLoader(String fileName) {

        this.fileName = fileName;

        this.parser = new MyXZTraceParser(fileName);

        this.operations = new LinkedList<>();

        currentInstruction = (MyXZTraceInstruction) parser.getNext();
        nextInstruction = (MyXZTraceInstruction) parser.getNext();


        parseCurrentInstruction();


    }


    private void getNextInstruction(){

        currentInstruction = nextInstruction;
        nextInstruction = (MyXZTraceInstruction) parser.getNext();

        parseCurrentInstruction();


    }

    private void parseCurrentInstruction() {

            if ( currentInstruction == null) return;


            int instSize = 0;

            if(nextInstruction == null) instSize = 1;// last instruction. lupljena vrednost

            else instSize = (int)(nextInstruction.getAddress() - currentInstruction.getAddress());//dif to next

            if(currentInstruction.isBranch() && currentInstruction.isTaken()) instSize = 3; // branch. lupljena vrednost

            if(instSize>20 || instSize<1) instSize=4;//ono neobjasnjivo ponasanje neko. lupljena vrednost

            if(instSize == 0) instSize = 4;// ako je ona fora sa istom inst. lupljena vrednost

            long adress = currentInstruction.getAddress();

            for (int i = 0; i < instSize; i++) {
                    operations.add(  new MemOp("I", adress+i));
            }



            //prvo source mem
            for (int i = 0; i < currentInstruction.getSource_memory().length; i++) {

                if(currentInstruction.getSource_memory()[i] != 0)
                    operations.add(new MemOp("R",currentInstruction.getSource_memory()[i]));
            }

            // pa dest
            for (int i = 0; i < currentInstruction.getDestination_memory().length; i++) {

                if(currentInstruction.getDestination_memory()[i] != 0)
                    operations.add(new MemOp("W",currentInstruction.getDestination_memory()[i]));
            }







    }

    @Override
    public MemoryOperation getNextOperation() {


        if(!hasOperationToLoad()) return null;

        MemOp memop = operations.removeFirst();

        if( memop.operation == "W") return MemoryOperation.write(memop.adress);

        else if(memop.operation == "I" || memop.operation == "R") return MemoryOperation.read(memop.adress);

        return  null;
    }

    @Override
    public boolean isInstructionOperation() {
        return operations.peekFirst().operation == "I";
    }//???

    @Override
    public boolean hasOperationToLoad() {

        if( operations.isEmpty() ) getNextInstruction();

        return !operations.isEmpty();
    }

    @Override
    public void reset() {
        try {
            close();
        } catch (Exception e) {
        }
        try {
            this.parser = new MyXZTraceParser(fileName);

            getNextInstruction();
        } catch (Exception e) {
        }
    }

    @Override
    public void close() throws IOException {

        parser.close();
    }
}
