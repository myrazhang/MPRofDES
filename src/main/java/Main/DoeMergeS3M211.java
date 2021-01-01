package Main;

import ilog.concert.IloException;
import jdk.jfr.StackTrace;
import systems.MergeS3M211;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DoeMergeS3M211 {


    private static final ExecutorService executor = Executors.newFixedThreadPool(1);


    public static void main(String argv[]) throws ExecutionException, InterruptedException, IloException, FileNotFoundException{

        // log 文件 ====================================================================================================
        String programPath = System.getProperty("user.dir");
        String resultDirectory = programPath + File.separator+"OUTPUT"+File.separator+"log_mergeS3M211.txt";
        OutputStream resultStream = null;
        resultStream  = new FileOutputStream(resultDirectory);
        PrintWriter result = new PrintWriter(resultStream, true);
        PrintWriter log = new PrintWriter(OutputStream.nullOutputStream());


        result.println("--------------------------------- pt1=20, pt2=8 ---------------------------------------------------------------------");
        result.println();
        List<Task> tasks = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        {int nbPart = 20;
            result.println("nbParts= " + nbPart);
            result.println("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15");
            for(int r=0;r<10;r++){
                long seed = r+10;
                for(int q = 1;q<=15;q++){
                    Task t = new Task(nbPart,q,20,8,seed,log);
                    tasks.add(t);

                    Future<?> future = executor.submit(t::execute);
                    futures.add(future);
                }
            }
        }

        for (Future<?> future : futures) {
            future.get();
        }
        for (Task task : tasks) {
            if(task.q == 1){
                result.println();
            }
            task.transferOutput(result);
        }
        result.println();
        result.println();



        result.println("--------------------------------- pt1=16, pt2=8 ---------------------------------------------------------------------");
        result.println();
        tasks.clear();
        futures.clear();

        {int nbPart = 20;
            result.println(nbPart);
            result.println("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15");
            for(int r=0;r<10;r++){
                long seed = r+1000;
                for(int q = 1;q<=15;q++){
                    Task t = new Task(nbPart,q,16,8,seed,log);
                    tasks.add(t);

                    Future<?> future = executor.submit(t::execute);
                    futures.add(future);
                }
            }
        }
        for (Future<?> future : futures) {
            future.get();
        }
        for (Task task : tasks) {
            if(task.q == 1){
                result.println();
            }
            task.transferOutput(result);
        }
        result.println();
        result.println();


        result.println("--------------------------------- pt1=10, pt2=8 ---------------------------------------------------------------------");
        result.println();
        tasks.clear();
        futures.clear();

        {int nbPart = 20;
            result.println(nbPart);
            result.println("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15");
            for(int r=0;r<10;r++){
                long seed = r+100;
                for(int q = 1;q<=15;q++){
                    Task t = new Task(nbPart,q,20,8,seed,log);
                    tasks.add(t);

                    Future<?> future = executor.submit(t::execute);
                    futures.add(future);
                }
            }
        }
        for (Future<?> future : futures) {
            future.get();
        }
        for (Task task : tasks) {
            if(task.q == 1){
                result.println();
            }
            task.transferOutput(result);
        }
        result.println();


        executor.shutdown();
        /*String programPath = System.getProperty("user.dir");
        String milpDirectory = programPath + File.separator+"OUTPUT"+File.separator+"MilpSol_mergeS3M211.txt";
        OutputStream milpStream = null;
        try {
            milpStream  = new FileOutputStream(milpDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PrintWriter milpResult = new PrintWriter(milpStream, true);
        system.buildMilpModel();

        boolean b = system.solveMilpModel();
        if(b)
            system.printMilpResults(milpResult);*/
    }


    private static class Task{
        private final ByteArrayOutputStream buffer0 = new ByteArrayOutputStream();
        private final PrintWriter output0 = new PrintWriter(buffer0);

        private final int nbPart;
        private final int q;
        private final double pt1;
        private final double pt2;
        private final PrintWriter log;
        private final long sd;

        public Task(int nbPart,
                int q,
                double pt1,
                double pt2,
                    long sd,
                    PrintWriter log){
            this.nbPart = nbPart;
            this.q = q;
            this.pt1 = pt1;
            this.pt2 =pt2;
            this.log = log;
            this.sd = sd;
        }

        private void execute() {
            try{
                MergeS3M211 system = new MergeS3M211(nbPart,nbPart,q,pt1,pt2,sd,log);
                system.simulate(false,new PrintWriter(OutputStream.nullOutputStream()));
                output0.print(system.getClockTime()/nbPart + " ");
            }
            catch(Exception e){
                e.printStackTrace();
                System.exit(-1);
            }
        }

        private void transferOutput(PrintWriter writer0) {
            output0.flush();
            writer0.print(buffer0.toString());
            writer0.flush();
        }

    }



}
