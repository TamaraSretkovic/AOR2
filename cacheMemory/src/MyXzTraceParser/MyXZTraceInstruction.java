package rs.ac.bg.etf.aor.parser.MyXzTraceParser;

import rs.ac.bg.etf.aor.parser.Instruction;

public class MyXZTraceInstruction implements Instruction {


    Long  ip;  // instruction pointer (program counter) value

    int is_branch;    // is this branch
    int branch_taken; // if so, is this taken

    int destination_registers[]; //2 output registers
    int source_registers[];      //4 input registers

    Long destination_memory[]; //2 output memory
    Long source_memory[];      //4 input memory


    public MyXZTraceInstruction( Long  ip, int is_branch, int branch_taken,
                                 int destination_registers[], int source_registers[],
                                 Long destination_memory[], Long source_memory[]){

        this.ip = ip;
        this.is_branch = is_branch;
        this.branch_taken = branch_taken;
        this.destination_registers = destination_registers;
        this.source_registers = source_registers;
        this.destination_memory = destination_memory;
        this.source_memory = source_memory;


    }

    public Long getIp() {
        return ip;
    }

    public int getIs_branch() {
        return is_branch;
    }

    public int getBranch_taken() {
        return branch_taken;
    }

    public Long[] getDestination_memory() {
        return destination_memory;
    }

    public Long[] getSource_memory() {
        return source_memory;
    }

    @Override
    public long getAddress() {
        return ip;
    }

    @Override
    public boolean isTaken() {
        return branch_taken == 1;
    }

    @Override
    public boolean isConditional() {
        return is_branch == 1;
    }

    @Override
    public boolean isBackwardBranch() {
        return false;
    }

    @Override
    public boolean isBranch() {
        return is_branch == 1;
    }

    public String toString(){

        return "ip: "+Long.toHexString(ip)+"   "+
                "branch? "+is_branch+"    "+ "taken? "+branch_taken+"  "+
                "dest_regs: "+destination_registers[0]+ ", "+destination_registers[1]+"    "+
                "source_registers: "+source_registers[0]+ ", "+source_registers[1]+", "+ source_registers[2]+ ", "+source_registers[3]+"    "+
                "dest_mem: "+Long.toHexString(destination_memory[0])+ ", "+Long.toHexString(destination_memory[1])+"    "+
                "source_mem: "+Long.toHexString(source_memory[0])+ ", "+Long.toHexString(source_memory[1])+", "+
                Long.toHexString(source_memory[2])+ ", "+Long.toHexString(source_memory[3]);

    }
}
