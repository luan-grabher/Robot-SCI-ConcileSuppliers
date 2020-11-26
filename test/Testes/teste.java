package Testes;

import java.io.File;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        String regex = "Ref\\. +[0-1][0-9]\\/20[0-9]{2}";
        String str = "INSS Ref.  09/2020";
        String numbersOnly = "";
        
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        if(m.find()){
            numbersOnly = m.group(0).replaceAll("[^0-9]", "");
        }
        
        System.out.println(numbersOnly);
    }

    public static void test() {
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
