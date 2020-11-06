package Testes;

import java.util.Calendar;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        Integer enterprise = 729;
        Integer account = 148;
        Integer participant = null;
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        Boolean reworkConciliate = false;

        startDate.set(2020, 01, 01);
        endDate.set(2020, 10, 31);

        System.out.println(
                Conciliate.principal(enterprise, account, participant, startDate, endDate, reworkConciliate)
        );
    }

}
