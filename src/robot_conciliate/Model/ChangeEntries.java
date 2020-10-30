package robot_conciliate.Model;

import SimpleView.Loading;
import java.util.Calendar;
import java.util.Map;
import lctocontabil.Entity.ContabilityEntry;

public class ChangeEntries {

    /**
     * Percorre todos os lançamentos passados e coloca o primeiro número do
     * complemento como documento caso o lançamento não tenha documento.
     *
     * @param entries lançamentos a ganhar documento
     * @return O próprio mapa passado de volta modificado. A referencia já é
     * modificada, não é necessário implementar.
     */
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

                //Define o documento como o primeiro número que encontrar no complemento
                contabilityEntry.setDocument(getFirstNumber(contabilityEntry.getComplement()));
            }
        }

        loading.dispose();

        return entries;
    }

    /**
     * Pega o primeiro número que aparecer em uma string.
     *
     * @param str String que contém números
     * @return Se encontrar um número retorna o número, se não retorna uma
     * string em branco.
     */
    public static String getFirstNumber(String str) {
        String[] numbers = str.split("[^0-9]+");
        for (String number : numbers) {
            if (!number.equals("")) {
                return number;
            }
        }

        return "";
    }

    /**
     * Remove do complemento os anos sequidos de varios zeros REmove caracteres
     * diferentes de letras, numeros, "." e "-"
     *
     * @param str String que será modificada.
     * @param year Ano de referencia
     * @return A própria String modificada, não precisa implementar ela pois a
     * referência recebida da variavel já é modificada
     */
    public static String fixComplement(String str, Integer year) {
        str = str.replaceAll(year + "000", "");
        str = str.replaceAll(year + "/", "");
        str = str.replaceAll("/" + year, "");
        str = str.replaceAll("000" + year, "");
        str = str.replaceAll("[^a-zA-Z0-9 .-]", "");

        return str;
    }
}
