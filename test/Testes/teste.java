package Testes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import robot_conciliate.Conciliate;

public class teste {

    public static void main(String[] args) {
        Integer inte = Integer.valueOf("");
    }
    
    public static void testeAcentos() {
        String str = "Olá mundão caçada ÒÌÈÊÕÃ";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        System.out.println(pattern.matcher(nfdNormalizedString).replaceAll(""));
    }
    
    public static void errorTest(){
        try{
            throw new Error("Deu um erro aqui");
            
        }catch(Error e){
            e.printStackTrace();            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            JOptionPane.showMessageDialog(null, sw.toString());
        }        
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
        Integer enterprise = 662;
        Integer account = 148;
        Integer participant = null;
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
