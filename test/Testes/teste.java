package Testes;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        test();
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
        Integer enterprise = 657;
        Integer account = 148;
        Integer participant = null;
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        Boolean reworkConciliate = false;

        startDate.set(2019, 0, 1);
        endDate.set(2020, 10, 30);

        System.out.println(
                Conciliate.principal(enterprise, account, participant, startDate, endDate, reworkConciliate)
        );

        //Terminar programa
        System.exit(0);
    }

}
