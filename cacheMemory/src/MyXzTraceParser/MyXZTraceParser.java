package rs.ac.bg.etf.aor.parser.MyXzTraceParser;

import org.tukaani.xz.XZInputStream;
import rs.ac.bg.etf.aor.parser.Instruction;
import rs.ac.bg.etf.aor.parser.Parser;

import java.io.*;

public class MyXZTraceParser implements Parser, Closeable {


    private BufferedReader r;
    private FileWriter fw;

    public MyXZTraceParser(String pathToTrace) {
        try {
            r = new BufferedReader(
                    new InputStreamReader(new XZInputStream(new FileInputStream(pathToTrace)), "US-ASCII"));

           // fw = new FileWriter("./traces/omnetpp_17B.traceContent.txt",true);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public boolean hasNext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instruction getNext() {

        try {

            int first = r.read();

            if(first == -1) return null;

            Long ip = (long)0;

            ip|=first;

            for (int i = 1; i < 8; i++) {

                long in = (long)r.read();

                in<<=8*i;

                ip|=in;

            }


            int is_branch = r.read();
            int branch_taken = r.read();


            int dest_regs[] = {r.read(), r.read()};

            int source_regs[]={r.read(), r.read(), r.read(), r.read()};

            Long dest_mem[] = {(long)0, (long)0};
            Long source_mem[] = {(long)0,(long)0,(long)0,(long)0};

            for (int j = 0; j < 2; j++)
                for (int i = 0; i < 8; i++) {

                    long in = (long)r.read();

                    in<<=8*i;

                    dest_mem[j]|=in;

                }

            for (int j = 0; j < 4; j++)
                for (int i = 0; i < 8; i++) {

                    long in = (long)r.read();

                    in<<=8*i;

                    source_mem[j]|=in;

                }


           MyXZTraceInstruction inst = new MyXZTraceInstruction(ip,is_branch,branch_taken,dest_regs,source_regs,dest_mem,source_mem);

//            System.out.println(inst.toString());
            //fw.append(inst.toString()).append("\n");

            return inst;





        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        r.close();
        if(fw !=null)        fw.close();
    }
}
