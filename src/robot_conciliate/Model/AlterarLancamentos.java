package robot_conciliate.Model;

import Auxiliar.Valor;
import Executor.View.Carregamento;
import java.util.Calendar;
import java.util.List;
import lctocontabil.Entity.LctoContabil;
import lctocontabil.Model.LctoContabil_Model;

public class AlterarLancamentos {

    public static void zerarConciliacao(LctoContabil_Model modelo) {
        
    }

    public static void definirNroDocto(List<LctoContabil> lctos) {
        Carregamento barra = new Carregamento("Definindo número doc", 0, lctos.size());

        for (int i = 0; i < lctos.size(); i++) {
            barra.atualizar(i);
            LctoContabil lcto = lctos.get(i);

            if (lcto.getDocumento().getLong() == (long) 0) {
                Integer ano = lcto.getData().getCalendar("ymd").get(Calendar.YEAR);
                lcto.setComplemento(arrumarComplemento(lcto.getComplemento(), ano));
                lcto.setComplemento(arrumarComplemento(lcto.getComplemento(), ano - 1));

                List<String> numerosComplemento = lcto.getComplemento().getNumbersList("/");
                if (numerosComplemento.size() > 0) {
                    Valor novoDoc = new Valor(numerosComplemento.get(0).replaceAll("[^0-9]", ""));
                    String nroDocString = novoDoc.getLong().toString();
                    Integer numeroComeca = nroDocString.length() - 14;
                    nroDocString = nroDocString.substring(numeroComeca < 0 ? 0 : numeroComeca, nroDocString.length());

                    lcto.setDocumento(new Valor(nroDocString));
                }
            }
        }
        
        barra.dispose();
    }

    private static Valor arrumarComplemento(Valor valor, Integer ano) {
        valor = new Valor(valor.getString().replaceAll(ano + "000", ""));
        valor = new Valor(valor.getString().replaceAll(ano + "/", ""));
        valor = new Valor(valor.getString().replaceAll("/" + ano, ""));
        valor = new Valor(valor.getString().replaceAll("000" + ano, ""));
        valor = new Valor(valor.getString().replaceAll("[^a-zA-Z0-9 .-]", ""));

        return valor;
    }
}
