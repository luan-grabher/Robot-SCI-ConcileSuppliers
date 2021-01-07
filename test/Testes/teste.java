package Testes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        bigTest();
    }
    
    public static void bigTest(){
        BigDecimal bd = new BigDecimal(BigInteger.ONE);
        
        for (int i = 0; i < 10; i++) {
            bd.add(BigDecimal.TEN);
        }
        
        System.out.println(bd);
    }
    
    public static void testCalendardif(){
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();

        startDate.set(2020, 11, 1);
        endDate.set(2020, 10, 30);
        
        long days = (endDate.getTimeInMillis() - startDate.getTimeInMillis())/ (24 * 60 * 60 * 1000);
        
        System.out.println(days);
    }
    
    public static void printTest(){
        for (int i = 0; i < 100; i++) {
            try {Thread.sleep(500);} catch (Exception e) {}
            System.out.print("\bEHEHEHE " + i);
        }
    }

    public static void mapTest(){
        Map<Integer, Integer> ints =  new TreeMap<>();
        
        ints.put(1, 1);
        ints.put(2, 2);
        ints.put(3, 3);
        ints.put(4, 4);
        ints.put(5, 5);
        
        
        for (Map.Entry<Integer, Integer> mapEntry : ints.entrySet()) {
            Integer mapKey = mapEntry.getKey();
            Integer entry = mapEntry.getValue();
            
            System.out.println(entry);
        }
    }
    
    public static void test() {
        Integer enterprise = 331;
        Integer account = 148;
        Integer participant = 909796;
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        Boolean reworkConciliate = false;

        startDate.set(2020, 0, 1);
        endDate.set(2020, 11, 31);

        System.out.println(
                Conciliate.principal(enterprise, account, participant, startDate, endDate, reworkConciliate)
        );

        //Terminar programa
        System.exit(0);
    }

}
