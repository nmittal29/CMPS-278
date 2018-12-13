import lsmTree.LSMTree;
import lsmTree.LSMTree_Block;

import java.util.Random;

public class LSMTreeMain {

    public static void main(String[] args) throws CloneNotSupportedException {
        /*LSMTree lsm = new LSMTree();
        lsm.createLSMTree(3,2,10,8,true);
        lsm.buildLSMTreeInMemoryComponents();
        lsm.buildLSMTreeDiskComponents();
        //lsm.writeLSMtoDisk();
        //lsm.readLSMfromDisk();
        Random rn = new Random();
        int key = 0;
        String value = null;
        int range = (1000 - 100 )+ 1;
        for(int i=0;i<170;i++){
            key =  rn.nextInt(range) + 100;
            value = Integer.toString(key) + "ab";
            lsm.insert_lsm(key,value);
        }
        lsm.print_lsm_state();*/

        LSMTree_Block lsm = new LSMTree_Block();
        lsm.createLSMTree(3,5,1,10,12,true);
        lsm.buildLSMTreeInMemoryComponents();
        lsm.buildLSMTreeDiskComponents();

        int array[] = new int[1600];
        for (int i=0; i<1600; i++){
            int rand = zipfCdfApprox(1600.0);
            array[i] = rand;
            lsm.insert_lsm(rand,rand + "ab");
        }
        lsm.printLSMStats();

        Random rn = new Random();
        int range = (1599 - 1)+ 1;
        int key = 0;
        for(int i=0; i<10; i++){
            key = rn.nextInt(range) + 1;
            System.out.println("Index: " + key + " value to be searched: " + array[key]) ;
            System.out.println(lsm.read_lsm(array[key]));
        }

        for(int i=0; i<10; i++){
            key = rn.nextInt(range) + 1;
            System.out.println("Index: " + key + " value has to be deleted: " + array[key]) ;
            lsm.delete_lsm(array[key]);
        }

        for(int i=0; i<10; i++){
            key = rn.nextInt(range) + 1;
            System.out.println("Index: " + key + " value to be searched: " + array[key]) ;
            System.out.println(lsm.read_lsm(array[key]));
        }
    }

    public static int zipfCdfApprox(double N) {
        Random rn = new Random();
        int range = ((int)N - 100)+ 1;
        double k = rn.nextInt(range);
        double s = Math.random();
        double a = (Math.pow(k, 1 - s) - 1) / (1 - s) + 0.5 + Math.pow(k, -s) / 2 + s / 12 - Math.pow(k, -1 - s) * s / 12;
        double b = (Math.pow(N, 1 - s) - 1) / (1 - s) + 0.5 + Math.pow(N, -s) / 2 + s / 12 - Math.pow(N, -1 - s) * s / 12;

        return (int)((a/b) * 100000);
    }
}
