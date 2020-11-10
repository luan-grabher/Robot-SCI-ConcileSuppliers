package robot_conciliate.Model;

import SimpleView.Loading;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Model.ContabilityEntries_Model;

public class ConciliateContabilityEntries {

    private final Integer enterprise;
    private final Integer accountFilter;
    private final Integer participantFilter;
    private final Calendar dateStart;
    private final Calendar dateEnd;
    private final Map<Integer, ContabilityEntry> entries;

    private final Map<Integer, Integer> participants = new HashMap<>();
    private final Map<String, String> documents = new HashMap<>();

    private Predicate<Entry<Integer, ContabilityEntry>> defaultPredicate;
    private Predicate<Entry<Integer, ContabilityEntry>> accountPredicate;
    private Predicate<Entry<Integer, ContabilityEntry>> enterprisePredicate;
    private Predicate<Entry<Integer, ContabilityEntry>> conciledPredicate;

    private final static Integer PARTICIPANT_TYPE_CREDIT = 0;
    private final static Integer PARTICIPANT_TYPE_DEBIT = 1;
    
    private final StringBuilder infos = new StringBuilder("Conferências:\n");

    public ConciliateContabilityEntries(Map<Integer, ContabilityEntry> entries, Integer enterprise, Integer account, Integer participant, Calendar dateStart, Calendar dateEnd) {
        this.entries = entries;
        this.enterprise = enterprise;
        this.accountFilter = account;
        this.participantFilter = participant;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

    }

    /**
     * Retorna String com inforamções sobre conciliados
     * 
     * @return Retorna String com inforamções sobre conciliados
     */
    public String getInfos() {
        return infos.toString();
    }    
    

    /**
     * Define predicados padrões
     */
    public void setDefaultPredicates() {
        //Não precisa utilizar o account predicate porque na teoria ja deveria ter somente os lançamentos da conta
        accountPredicate = e -> e.getValue().getAccountCredit().equals(accountFilter) && e.getValue().getAccountDebit().equals(accountFilter);
        //Não precisa usare o enterprise pq na teoria todos lançamentos são dessa empresa
        enterprisePredicate = e -> e.getValue().getEnterprise().equals(enterprise);

        conciledPredicate = e -> e.getValue().isConciliated();
        defaultPredicate = conciledPredicate.negate();
    }

    /**
     * Cria lista com participantes e documentos dos lançamentos ou unicamente
     * com o participante passado
     */
    public void createParticipantAndDcoumentList() {
        //Se tiver filtro de participante
        if (participantFilter != null) {
            //Adiciona o participante na lista
            participants.put(participantFilter, participantFilter);
        } else {
            //Se não cria uma lista com os participantes das entradas
            for (Map.Entry<Integer, ContabilityEntry> entry : entries.entrySet()) {
                Integer key = entry.getKey();
                ContabilityEntry ce = entry.getValue();
                participants.put(ce.getParticipantCredit(), ce.getParticipantCredit());
                participants.put(ce.getAccountDebit(), ce.getParticipantDebit());
                documents.put(ce.getDocument(), ce.getDocument());
            }
        }
    }
    
    /**
     * Mostra valores atuais de conciliados do participante com o Metodo Simple
     * 
     * @param participant Codigo participante
     */
    private void showConciledInfos(Integer participant){
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        
        Predicate<Entry<Integer, ContabilityEntry>> creditPredicate = conciledPredicate.and(e -> e.getValue().getParticipantCredit().equals(participant));
        Predicate<Entry<Integer, ContabilityEntry>> debitPredicate = conciledPredicate.and(e -> e.getValue().getParticipantDebit().equals(participant));
        
        BigDecimal credit = entries.entrySet().stream().filter(creditPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = entries.entrySet().stream().filter(debitPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
                
        infos.append("\nDébito: ").append(nf.format(debit));
        infos.append("\nCrédito: ").append(nf.format(credit));
    }

    /**
     * Concilia a lista de participantes atual
     */
    public void conciliateParticipants() {
        Loading loading = new Loading("Conciliando participantes", 0, participants.size());
        int i = 0;
        
        for (Map.Entry<Integer, Integer> part : participants.entrySet()) {            
            //Pega numero participante
            Integer participant = part.getKey();
            
            //Atualiza barra
            i++;
            loading.updateBar(i + " de " + participants.size() + "(" + participant + ")", i);
            
            //Mostra informações
            infos.append("\nParticipante ").append(participant).append(":\n");
            infos.append("\nConciliados ANTES da conciliação:");
            showConciledInfos(participant);
            
            //Concilia por saldo
            System.out.println(participant + " - Por Saldo");
            conciliateByBalance(participant);
            System.out.println(participant + " - Por Documento");
            conciliateByDocuments(participant);
            System.out.println(participant + " - Por Valor");
            conciliateByValues(participant);
            System.out.println(participant + " - Por Valores seguintes");
            conciliateByAfterValues(participant);

            //mostra informações
            infos.append("\nConciliados APÓS conciliação:");
            showConciledInfos(participant);
            
            infos.append("\n");
        }
        
        loading.dispose();
    }

    /**
     * Concilia lançamentos pelo débito e crédito dos documentos
     *
     * @param participant Código de participante para os filtros de documento de
     * credito e debito
     */
    private void conciliateByDocuments(Integer participant) {
        for (Entry<String, String> entry : documents.entrySet()) {
            String document = entry.getKey();
            Predicate<Entry<Integer, ContabilityEntry>> documentPredicate = defaultPredicate.and(e -> e.getValue().getDocument().equals(document));

            Predicate<Entry<Integer, ContabilityEntry>> creditPredicate = documentPredicate.and(e -> e.getValue().getParticipantCredit().equals(participant));
            Predicate<Entry<Integer, ContabilityEntry>> debitPredicate = documentPredicate.and(e -> e.getValue().getParticipantDebit().equals(participant));

            conciliateByPredicates(creditPredicate, debitPredicate);
        }
    }

    /**
     * Concilia lançamentos se filtro de crédito e débito forem iguais na soma
     */
    private void conciliateByPredicates(Predicate<Entry<Integer, ContabilityEntry>> creditPredicate, Predicate<Entry<Integer, ContabilityEntry>> debitPredicate) {
        //Busca total crédito e débito
        BigDecimal credit = entries.entrySet().stream().filter(creditPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = entries.entrySet().stream().filter(debitPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);

        //Se o credito for igual ao debito
        if (credit.compareTo(debit) == 0) {

            //Concilia lançamentos que tiverem o predicato
            ContabilityEntries_Model.defineConciliatedsTo(
                    entries.entrySet().stream().filter(debitPredicate.or(creditPredicate))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())),
                    Boolean.TRUE
            );
        }
    }

    /**
     * Concilia lançamentos da conta/participante informado quando o saldo
     * estiver zerado entre as datas informadas dentro do mapa passado
     *
     * @param entries Mapa com os lançamentos que serão modificados com base na
     * conta/participante informada(o)
     * @param enterprise Empresa dos lançamentos
     * @param account Conta contábil dos lançamentos
     * @param participantCode participante dos lançamentos, caso não exista,
     * deve ser null
     * @param dateStart Data que inicia as verificações
     * @param dateEnd Data que terminam as verificações
     */
    private void conciliateByBalance(Integer participantCode) {
        //Inicia na data final e coloca mais um pois vai ir diminuindo e quero considerar a data final
        Calendar date = Calendar.getInstance();
        date.set(dateEnd.get(Calendar.YEAR), dateEnd.get(Calendar.MONTH), dateEnd.get(Calendar.DATE));
        date.add(Calendar.DATE, 1);

        //Se a data atual for maior que a data inicial, segue
        while (date.compareTo(dateStart) > 0) {
            //Diminui um dia
            date.add(Calendar.DATE, -1);

            //Pega saldo de credito e débito na data
            Map<String, BigDecimal> balances = ContabilityEntries_Model.selectAccountBalance(enterprise, accountFilter, participantCode, date);
            //Se o credito != 0 e o debito for igual ao credito
            if (balances.get("credit").compareTo(BigDecimal.ZERO) != 0 && balances.get("credit").compareTo(balances.get("debit")) == 0) {

                //Pega Lançamentos da conta e dos participantes
                Map<Integer, ContabilityEntry> toConciliate = entries.entrySet().stream().filter(
                        c
                        //Conta em debito ou credito
                        -> (c.getValue().getAccountCredit().equals(accountFilter) || c.getValue().getAccountDebit().equals(accountFilter))
                        //Data menor ou igual a data atual
                        && c.getValue().getDate().compareTo(date) <= 0
                        //participante nulo ou igual
                        && (participantCode == null || (c.getValue().getParticipantCredit().equals(participantCode) || c.getValue().getParticipantDebit().equals(participantCode)))
                ).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

                //Concilia lançamentos
                ContabilityEntries_Model.defineConciliatedsTo(toConciliate, Boolean.TRUE);

                //Concilia no banco também pois depois pode ter erro, pois só conciliou os lançamentos que estou usando
                String listToConciliate = ContabilityEntries_Model.getEntriesListBeforeDate(date, enterprise, accountFilter, participantCode);
                ContabilityEntries_Model.conciliateKeysOnDatabase(enterprise, listToConciliate, Boolean.TRUE);
            }
        }
    }

    /**
     * Percorre todos lançamentos e procura por valores iguais, múltiplos
     *
     * @param participant Codigo do participante
     */
    private void conciliateByValues(Integer participant) {
        //Cria predicatos
        Predicate<Entry<Integer, ContabilityEntry>> participantCreditPredicate = e -> e.getValue().getParticipantCredit().equals(participant);
        Predicate<Entry<Integer, ContabilityEntry>> participantDebitPredicate = e -> e.getValue().getParticipantDebit().equals(participant);

        //Loading
        Loading loading = new Loading("Conciliando por valor Participante " + participant, 0, entries.size());
        int i = 0;
        
        //Percorre todos lançamentos
        for (Entry<Integer, ContabilityEntry> entry : entries.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();
            
            //Loading
            i++;
            loading.updateBar(i + " de " + entries.size(), i);

            //Se não estiver conciliado
            if (!ce.isConciliated()) {

                //Define onde a conta está, se em crédito ou em débito 
                Integer participantType = ce.getParticipantCredit().equals(participant) ? PARTICIPANT_TYPE_CREDIT : PARTICIPANT_TYPE_DEBIT;

                //Define os totais de credito e débito
                Map<Integer, BigDecimal> credits = new LinkedHashMap<>();
                Map<Integer, BigDecimal> debits = new LinkedHashMap<>();

                //Coloca o valor do lançamento atual verificado na soma                
                if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                    credits.put(key, ce.getValue());
                } else if (participantType.equals(PARTICIPANT_TYPE_DEBIT)) {
                    debits.put(key, ce.getValue());
                }

                //Enquanto o crédito for diferente do débito
                while (credits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(
                        debits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add)
                ) != 0) {

                    //Soma lista de creditos e debitos atuais
                    BigDecimal totalCredit = credits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalDebit = debits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal diference;

                    //Se o participante estiver no credito, procura no debito
                    if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                        //Procura o valor e adiciona no mapa se ecnontrar
                        diference = totalCredit.add(totalDebit.negate());
                        if (findEntryWithDiference(debits, diference, participantDebitPredicate)) {
                            //Adicionou o debito, entao pode vazar
                            break;
                        }
                    } else {
                        //Procura o valor e adiciona no mapa se ecnontrar
                        diference = totalDebit.add(totalCredit.negate());
                        if (findEntryWithDiference(credits, diference, participantCreditPredicate)) {
                            //Adicionou o credito, entao pode vazar
                            break;
                        }
                    }

                    //Procura valor multiplo
                    Optional<Entry<Integer, ContabilityEntry>> multipleEntry;
                    
                    if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                        multipleEntry = entries.entrySet().stream().filter(
                                conciledPredicate.negate()
                                        .and(participantDebitPredicate)
                                        .and(predicateNotInMap(debits))
                                        .and(e -> e.getValue().getValue().compareTo(diference) == -1)//Menor que a diferença
                                        .and(e -> ce.getValue().remainder(e.getValue().getValue()).compareTo(BigDecimal.ZERO) == 0)//multiplo do valor original                            
                        ).findFirst();
                    }else{
                        multipleEntry = entries.entrySet().stream().filter(
                                conciledPredicate.negate()
                                        .and(participantCreditPredicate)
                                        .and(predicateNotInMap(credits))
                                        .and(e -> e.getValue().getValue().compareTo(diference) == -1)//Menor que a diferença
                                        .and(e -> ce.getValue().remainder(e.getValue().getValue()).compareTo(BigDecimal.ZERO) == 0)//multiplo do valor original                            
                        ).findFirst();
                    }

                    //Se existir o múltiplo, adiciona na lista de reversa
                    if (multipleEntry.isPresent()) {
                        if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                            debits.put(multipleEntry.get().getKey(), multipleEntry.get().getValue().getValue());
                        } else {
                            credits.put(multipleEntry.get().getKey(), multipleEntry.get().getValue().getValue());
                        }
                    }
                }

                //Verifica se debito fecha com credito
                if (credits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(
                        debits.entrySet().stream().map(t -> t.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add)
                ) == 0) {
                    //Concilia lançamentos de crédito e de débito
                    debits.entrySet().stream().map((entryDebit) -> entryDebit.getKey()).forEachOrdered((keyDebit) -> {
                        entries.get(keyDebit).conciliate();
                    });
                    credits.entrySet().stream().map((entryCredit) -> entryCredit.getKey()).forEachOrdered((keyCredit) -> {
                        entries.get(keyCredit).conciliate();
                    });
                }
            }
        }
        
        //Acaba com a barra
        loading.dispose();
    }

    /**
     * Encontra o lançamento com valor reverso (diferença) e se encontrar
     * adiciona no mapa informado
     *
     * @param mapValues mapa com valores que serão implementados caso ache
     * @param total Total do valor maior
     * @param totalReverse total do valor menor(reverso, valor procurado)
     * @param participantReversePredicate Predicato para filtrar o participante
     * reverso
     *
     * @return Retorna TRUE caso encontre e implemente no mapa
     * @return Retorna FALSE caso não encontre e não implementa no mapa
     */
    private boolean findEntryWithDiference(Map<Integer, BigDecimal> mapValues, BigDecimal diference, Predicate<Entry<Integer, ContabilityEntry>> participantReversePredicate) {
        //Procura lançamento com valor igual
        Optional<Entry<Integer, ContabilityEntry>> entryWithEqualValue = getFirstEntryWithValue(participantReversePredicate, diference, mapValues);
        //Se encontrar adiciona na lista de debitos
        if (entryWithEqualValue.isPresent()) {
            mapValues.put(entryWithEqualValue.get().getKey(), entryWithEqualValue.get().getValue().getValue());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retorna ou não lançamento com o valor procurado
     *
     * @param map Mapa que o lançamento não pode estar
     * @param predicateParticipant Predicato para filtrar o participante
     * @param value Valor procurado
     *
     * @return Lançamento opcional, pode ser verificado com "isPresent"
     */
    private Optional<Entry<Integer, ContabilityEntry>> getFirstEntryWithValue(Predicate<Entry<Integer, ContabilityEntry>> predicateParticipant, BigDecimal value, Map<Integer, BigDecimal> map) {
        return entries.entrySet().stream().filter(
                predicateParticipant
                        .and(predicateValueEqual(value))
                        .and(conciledPredicate.negate())
                        .and(predicateNotInMap(map))
        ).findFirst();
    }

    /**
     * Retorna predicato para filtrar o valor bigdecimal
     *
     * @param value Valor BigDecimal que deve filtrar
     * @return Retorna o predicato para filtrar valor igual ao informado
     */
    private Predicate<Entry<Integer, ContabilityEntry>> predicateValueEqual(BigDecimal value) {
        return e -> e.getValue().getValue().compareTo(value) == 0;
    }

    /**
     * Retorna predicato para encontrar os lançamentos que nao estao no mapa
     *
     * @param map Mapa com valores bigdecimal e chaves
     * @return Retorna o predicato para filtrar lançamentos que NÃO estejam
     * naquele mapa
     */
    private Predicate<Entry<Integer, ContabilityEntry>> predicateNotInMap(Map<Integer, BigDecimal> map) {
        return e -> !map.containsKey(e.getKey());
    }

    /**
     * Concilia valores do participante conforme proximos valores contrários.
     * Para cada lançamento tenta fechar com os proximos lançamentos contrários.
     * 
     * @param participant Codigo de participante
     */
    private void conciliateByAfterValues(Integer participant) {
        //Cria predicatos
        Predicate<Entry<Integer, ContabilityEntry>> participantCreditPredicate = e -> e.getValue().getParticipantCredit().equals(participant);
        Predicate<Entry<Integer, ContabilityEntry>> participantDebitPredicate = e -> e.getValue().getParticipantDebit().equals(participant);

        //Percorre todos lançamentos
        for (Entry<Integer, ContabilityEntry> entry : entries.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();

            //Se não estiver conciliado
            if (!ce.isConciliated()) {
                //Define onde a conta está, se em crédito ou em débito 
                Integer participantType = ce.getParticipantCredit().equals(participant) ? PARTICIPANT_TYPE_CREDIT : PARTICIPANT_TYPE_DEBIT;

                Predicate<Entry<Integer, ContabilityEntry>> participantPredicate;
                Predicate<Entry<Integer, ContabilityEntry>> reverseParticipantPredicate;

                //Define os totais de credito e débito
                Map<Integer, BigDecimal> reverseValues = new LinkedHashMap<>();

                //Define predicatos dos participantes                
                if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                    participantPredicate = participantCreditPredicate;
                    reverseParticipantPredicate = participantDebitPredicate;
                } else {
                    participantPredicate = participantDebitPredicate;
                    reverseParticipantPredicate = participantCreditPredicate;
                }

                //Cria lista de lançamentos reversos menores e depois
                Map<Integer, ContabilityEntry> reverses = entries.entrySet().stream().filter(
                        reverseParticipantPredicate //Conta contrária
                                .and(conciledPredicate.negate()) //Não conciliado
                                .and(e -> e.getValue().getDate().compareTo(ce.getDate()) >= 0) //Data posterior
                                .and(e -> e.getValue().getValue().compareTo(ce.getValue()) <= 0) //Valor Menor ou igual
                ).collect(
                        Collectors.toMap(e -> e.getKey(), e -> e.getValue())
                );

                //Cria mapa com lançamentos ordenados por data
                SortedMap<Calendar, ContabilityEntry> reversesSorted = new TreeMap<>();
                //Popula mapa com lançamentos ordenados por data
                reverses.entrySet().forEach((reverseEntry) -> {
                    reversesSorted.put(reverseEntry.getValue().getDate(), reverseEntry.getValue());
                });

                //Cria lista que irá receber os lançamentos para conciliar
                Map<Integer, ContabilityEntry> toConciliate = new HashMap<>();

                //Cria variavel com soma dos reversos
                BigDecimal calculated = new BigDecimal("0.00");

                //Percorre mapa ordenado por data
                for (Entry<Calendar, ContabilityEntry> reverseEntry : reversesSorted.entrySet()) {
                    ContabilityEntry obj = reverseEntry.getValue();
                    //Se valor somado ao valor atual apurado reverso for menor ou igual ao valor verificado
                    if (obj.getValue().add(calculated).compareTo(ce.getValue()) <= 0) {
                        //Adiciona na lista de conciliar
                        toConciliate.put(obj.getKey(), obj);
                        //Soma no total de reversos
                        calculated = calculated.add(obj.getValue());
                        //Se não
                    } else {
                        //----Sai do loop
                        break;
                    }
                }

                // Se valor dos reversos for igual ao valor verificado
                if (calculated.compareTo(ce.getValue()) == 0) {
                    //Adiciona na lista o proprio lançamento atual
                    toConciliate.put(key, ce);
                    //Concilia lançamentos da lista de conciliar
                    ContabilityEntries_Model.defineConciliatedsTo(toConciliate, Boolean.TRUE);
                }

            }
        }
    }
}
