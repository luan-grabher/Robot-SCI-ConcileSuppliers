package robot_conciliate.Model;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;
import java.util.stream.Collectors;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Model.ContabilityEntries_Model;

public class SaldoConta {

    /**
     * Concilia lançamentos da conta/participante informado quando o saldo
     * estiver zerado entre as datas informadas dentro do mapa passado
     * 
     * @param entries Mapa com os lançamentos que serão modificados com base na conta/participante informada(o)
     * @param enterprise Empresa dos lançamentos
     * @param account Conta contábil dos lançamentos
     * @param participant participante dos lançamentos, caso não exista, deve ser null
     * @param dateStart Data que inicia as verificações
     * @param dateEnd Data que terminam as verificações
     */
    private static void conciliateByBalance(Map<Integer, ContabilityEntry> entries, Integer enterprise, Integer account, Integer participant, Calendar dateStart, Calendar dateEnd) {
        //Inicia na data final e coloca mais um pois vai ir diminuindo e quero considerar a data final
        Calendar date = Calendar.getInstance();
        date.set(dateEnd.get(Calendar.YEAR), dateEnd.get(Calendar.MONTH), dateEnd.get(Calendar.DATE));
        date.add(Calendar.DATE, 1);

        //Se a data atual for maior que a data inicial, segue
        while (date.compareTo(dateStart) > 0) {
            //Diminui um dia
            date.add(Calendar.DATE, -1);

            //Pega saldo de credito e débito na data
            Map<String, BigDecimal> balances = ContabilityEntries_Model.selectAccountBalance(enterprise, account, participant, date);
            //Compara o debito e credito
            if (balances.get("credit").compareTo(balances.get("debit")) == 0) {

                //Pega Lançamentos da conta e dos participantes
                Map<Integer, ContabilityEntry> toConciliate = entries.entrySet().stream().filter(
                        c
                        //Conta em debito ou credito
                        -> (c.getValue().getAccountCredit().equals(account) || c.getValue().getAccountDebit().equals(account))
                        //Data menor ou igual a data atual
                        && c.getValue().getDate().compareTo(date) <= 0
                        //participante nulo ou igual
                        && (participant == null || (c.getValue().getParticipantCredit().equals(participant) || c.getValue().getParticipantDebit().equals(participant)))
                ).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

                //Concilia lançamentos
                ContabilityEntries_Model.defineConciliatedsTo(toConciliate, Boolean.TRUE);

                //Concilia no banco também pois depois pode ter erro, pois só conciliou os lançamentos que estou usando
                String listToConciliate = ContabilityEntries_Model.getEntriesListBeforeDate(date, enterprise, account, participant);
                ContabilityEntries_Model.conciliateKeysOnDatabase(enterprise, listToConciliate, Boolean.TRUE);
            }
        }
    }
}
