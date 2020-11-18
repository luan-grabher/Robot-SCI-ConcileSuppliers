package Testes;

import java.util.Calendar;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        Integer enterprise = 729;
        Integer account = 148;
        Integer participant = 327896;
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        Boolean reworkConciliate = true;        

        startDate.set(2020, 0, 1);
        endDate.set(2020, 11, 31);

        System.out.println(
                Conciliate.principal(enterprise, account, participant, startDate, endDate, reworkConciliate)
        );
        
        //Terminar programa
        System.exit(0);
    }

}
