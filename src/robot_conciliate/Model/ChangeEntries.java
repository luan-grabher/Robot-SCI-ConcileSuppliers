package robot_conciliate.Model;

import Auxiliar.Valor;
import SimpleView.Loading;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import lctocontabil.Entity.ContabilityEntry;

public class ChangeEntries {

    public static Map<Integer, ContabilityEntry> setDocument(Map<Integer, ContabilityEntry> entries) {
        //Inicia barra
        Integer size = entries.size();
        Loading loading = new Loading("Definindo número de documentos", 0, size);

        //Percorre lançamentos
        Integer i = 0;
        for (Map.Entry<Integer, ContabilityEntry> entry : entries.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry contabilityEntry = entry.getValue();

            //Atualiza barra
            i++;
            loading.updateBar(i + " de " + size, i);

            //Se não possuir documento
            if ("".equals(contabilityEntry.getDocument())) {
                //Arruma complemento
                contabilityEntry.setComplement(
                        fixComplement(
                                contabilityEntry.getComplement(),
                                contabilityEntry.getDate().get(Calendar.YEAR)
                        )
                );
                contabilityEntry.setComplement(
                        fixComplement(
                                contabilityEntry.getComplement(),
                                contabilityEntry.getDate().get(Calendar.YEAR) - 1
                        )
                );

                
            }
        }

        return entries;
    }

    public static String fixComplement(String str, Integer year) {
        str = str.replaceAll(year + "000", "");
        str = str.replaceAll(year + "/", "");
        str = str.replaceAll("/" + year, "");
        str = str.replaceAll("000" + year, "");
        str = str.replaceAll("[^a-zA-Z0-9 .-]", "");

        return str;
    }

    public static void setDocumento(List<LctoContabil> lctos) {
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
