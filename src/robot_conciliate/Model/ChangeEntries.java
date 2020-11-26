package robot_conciliate.Model;

import SimpleView.Loading;
import java.util.Calendar;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                String newDocument;

                String reference = getRefNumber(contabilityEntry.getComplement());
                if ("".equals(reference)) {
                    //Cria um complemento para extrair o documento
                    String complementWithoutYears;
                    complementWithoutYears = getDocumentFromComplement(
                            contabilityEntry.getComplement(),
                            contabilityEntry.getDate().get(Calendar.YEAR)
                    );
                    complementWithoutYears = getDocumentFromComplement(
                            complementWithoutYears,
                            contabilityEntry.getDate().get(Calendar.YEAR) - 1
                    );
                    newDocument = getFirstNumber(complementWithoutYears);
                }else{
                    newDocument = reference;
                }

                //Define o documento como o primeiro número que encontrar no complemento                
                contabilityEntry.setDocument(newDocument);
            }
        }

        loading.dispose();

        return entries;
    }

    /**
     * Se tiver regex "Ref. MM/YYYY" irá retornar uma String no formato MM/YYYY,
     * se não tiver, retorna em branco
     *
     * @param str String do complemento
     * @return retornar uma String no formato MM/YYYY, se não tiver, retorna em
     * branco
     */
    public static String getRefNumber(String str) {
        String regex = "Ref\\. +[0-1][0-9]\\/20[0-9]{2}";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        if (m.find()) {
            return m.group(0).replaceAll("[^0-9]", "");
        }

        return "";
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
    public static String getDocumentFromComplement(String str, Integer year) {
        str = str.replaceAll(year + "000", "");
        str = str.replaceAll(year + "/", "");
        str = str.replaceAll("/" + year, "");
        str = str.replaceAll("000" + year, "");
        str = str.replaceAll("[^a-zA-Z0-9 .-]", "");

        return str;
    }
}
