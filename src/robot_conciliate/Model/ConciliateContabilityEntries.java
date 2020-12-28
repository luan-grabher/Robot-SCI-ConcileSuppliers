package robot_conciliate.Model;

import Dates.Dates;
import SimpleView.Loading;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Model.ContabilityEntries_Model;

public class ConciliateContabilityEntries {

    private final Integer enterprise;
    private final Integer account;
    private final Integer participantFilter;
    private final Calendar dateStart;
    private final Calendar dateEnd;
    private final Map<Integer, ContabilityEntry> entries;
    private Map<Integer, ContabilityEntry> notConcileds = new TreeMap<>();

    private final Map<Integer, Integer> participants = new TreeMap<>();
    private final Map<String, String> documents = new TreeMap<>();

    private Predicate<Entry<Integer, ContabilityEntry>> defaultPredicate;
    private Predicate<Entry<Integer, ContabilityEntry>> accountPredicateCredit;
    private Predicate<Entry<Integer, ContabilityEntry>> accountPredicateDebit;
    private Predicate<Entry<Integer, ContabilityEntry>> conciledPredicate;

    private final static Integer PARTICIPANT_TYPE_CREDIT = 0;
    private final static Integer PARTICIPANT_TYPE_DEBIT = 1;

    private final StringBuilder infos = new StringBuilder("Conferências:\n");
    private long entriesConciledBefore = 0;
    private long entriesConciledAfter = 0;

    //FOR É MAIS RAPIDO QUE STREAM OU .FOREACH
    public ConciliateContabilityEntries(Map<Integer, ContabilityEntry> entries, Integer enterprise, Integer account, Integer participant, Calendar dateStart, Calendar dateEnd) {
        this.entries = entries;
        this.enterprise = enterprise;
        this.account = account;
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
        entriesConciledAfter = this.entries.entrySet().stream().filter(conciledPredicate).count();

        BigDecimal beforePercent = new BigDecimal(entriesConciledBefore * 100 / entries.size());
        BigDecimal afterPercent = new BigDecimal(entriesConciledAfter * 100 / entries.size());

        infos.append("TOTAL DE CONCILIADOS dos ").append(entries.size()).append(" lançamentos:");
        infos.append("\nAntes: ").append(entriesConciledBefore).append(" (").append(beforePercent.toString()).append("%)");
        infos.append("\nDepois: ").append(entriesConciledAfter).append(" (").append(afterPercent.toString()).append("%)");

        return infos.toString();
    }

    /**
     * Reseta o mapa de não conciliados e coloca todos lançamentos nao
     * conciliados nele
     */
    public void setNotConcileds() {
        notConcileds = new TreeMap<>();
        for (Entry<Integer, ContabilityEntry> mapEntry : entries.entrySet()) {
            ContabilityEntry entry = mapEntry.getValue();

            if (!entry.isConciliated()) {
                notConcileds.put(entry.getKey(), entry);
            }
        }
    }

    /**
     * Remove dos não conciliados dos conciliados
     */
    public void refreshNotConcileds() {
        List<Integer> toRemove = new ArrayList<>();

        for (Entry<Integer, ContabilityEntry> mapEntry : notConcileds.entrySet()) {
            ContabilityEntry entry = mapEntry.getValue();

            //Se estiver conciliado
            if (entry.isConciliated()) {
                //Adiciona na lista para remover
                toRemove.add(entry.getKey());
            }
        }

        //remove entradas não conciliadas
        for (Integer removeId : toRemove) {
            notConcileds.remove(removeId);
        }
    }

    /**
     * Remove dos não conciliados dos conciliados
     *
     * @param removeMap removerá todos os lançaentos deste mapa do mapa de nao
     * conciliados
     */
    public void removeOfNotConcileds(Map<Integer, ContabilityEntry> removeMap) {
        //Percorre o mapa fornecido
        for (Entry<Integer, ContabilityEntry> mapEntry : removeMap.entrySet()) {
            //Remove lançamento dos nao conciliados
            notConcileds.remove(mapEntry.getKey());
        }
    }

    /**
     * Define predicados padrões
     */
    public void setDefaultPredicates() {
        //Não precisa utilizar o account predicate porque na teoria ja deveria ter somente os lançamentos da conta
        accountPredicateCredit = e -> e.getValue().getAccountCredit().equals(account);
        accountPredicateDebit = e -> e.getValue().getAccountDebit().equals(account);

        conciledPredicate = e -> e.getValue().isConciliated();
        defaultPredicate = conciledPredicate.negate();

        entriesConciledBefore = this.entries.entrySet().stream().filter(conciledPredicate).count();
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
                participants.put(ce.getParticipantDebit(), ce.getParticipantDebit());
                documents.put(ce.getDocument(), ce.getDocument());
            }
        }
    }

    /**
     * Mostra valores atuais de conciliados do participante com o Metodo Simple
     *
     * @param participant Codigo participante
     */
    private void showConciledInfos(Integer participant) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();

        Predicate<Entry<Integer, ContabilityEntry>> creditPredicate = accountPredicateCredit.and(conciledPredicate.and(e -> e.getValue().getParticipantCredit().equals(participant)));
        Predicate<Entry<Integer, ContabilityEntry>> debitPredicate = accountPredicateDebit.and(conciledPredicate.and(e -> e.getValue().getParticipantDebit().equals(participant)));

        BigDecimal credit = entries.entrySet().stream().filter(creditPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = entries.entrySet().stream().filter(debitPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);

        infos.append("\n    Débito: ").append(nf.format(debit));
        infos.append("\n    Crédito: ").append(nf.format(credit));
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
            infos.append("\nPARTICIPANTE ").append(participant).append(":");
            infos.append("\n    Conciliados ANTES da conciliação:");
            showConciledInfos(participant);

            //Define os não conciliados para percorrer a lista mais rapido
            setNotConcileds();

            //Concilia por saldo
            System.out.println(participant + ": Conciliando por Saldo");
            conciliateByBalance(participant);
            //Concilia por documentos
            System.out.println(participant + ": Conciliando por Documento");
            conciliateByDocuments(participant);
            //Concilia por valor
            System.out.println(participant + ": Conciliando por Valor");
            conciliateByValues(participant);
            //Concilia pelos valores apos cada valor
            System.out.println(participant + ": Conciliando por Valores Futuros");
            conciliateByAfterValues(participant);

            //mostra informações
            infos.append("\n    Conciliados APÓS conciliação:");
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

            Predicate<Entry<Integer, ContabilityEntry>> creditPredicate = conciledPredicate.negate().and(accountPredicateCredit.and(documentPredicate.and(e -> e.getValue().getParticipantCredit().equals(participant))));
            Predicate<Entry<Integer, ContabilityEntry>> debitPredicate = conciledPredicate.negate().and(accountPredicateDebit.and(documentPredicate.and(e -> e.getValue().getParticipantDebit().equals(participant))));

            conciliateByPredicates(creditPredicate, debitPredicate);
        }

        refreshNotConcileds();
    }

    /**
     * Concilia lançamentos se filtro de crédito e débito forem iguais na soma
     */
    private void conciliateByPredicates(Predicate<Entry<Integer, ContabilityEntry>> creditPredicate, Predicate<Entry<Integer, ContabilityEntry>> debitPredicate) {
        //Busca total crédito e débito
        BigDecimal credit = notConcileds.entrySet().stream().filter(creditPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = notConcileds.entrySet().stream().filter(debitPredicate).map(e -> e.getValue().getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);

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
        Map<Long, Calendar> participantDates = getNotConciledsCalendarsMap(participantCode);

        try {
            participantDates.forEach((position, date) -> {
                System.out.print("\r" + participantCode + ": " + Dates.getCalendarInThisStringFormat(date, "dd/MM/yyyy"));

                Long entriesBeforeDateNotConcileds = ContabilityEntries_Model.getEntriesCountBeforeDate(date, enterprise, account, Boolean.FALSE, participantCode);

                //Se tiver lançamentos nao conciliados antes daquela data no participante
                if (entriesBeforeDateNotConcileds > 0) {
                    //Pega saldo de credito e débito na data
                    Map<String, BigDecimal> balances = ContabilityEntries_Model.selectAccountBalance(enterprise, account, participantCode, date);
                    //Se o credito != 0 e o debito for igual ao credito
                    if (balances.get("credit").compareTo(BigDecimal.ZERO) != 0
                            && balances.get("credit").compareTo(balances.get("debit")) == 0) {

                        //Concilia lançamentos da lista que eu tenho não conciliados
                        notConcileds.forEach((key, ce) -> {

                            if ( //Conta em debito ou credito
                                    (ce.getAccountCredit().equals(account) || ce.getAccountDebit().equals(account))
                                    //Data menor ou igual a data atual
                                    && ce.getDate().compareTo(date) <= 0
                                    //participante nulo ou igual
                                    && (participantCode == null || (ce.isParticipant(participantCode)))) {
                                //concilia o lançamento
                                ce.conciliate();

                                //Sai do foreach
                                throw new Error("Break");
                            }
                        });

                        //Concilia no banco também pois depois pode ter erro, pois só conciliou os lançamentos que estou usando
                        String listToConciliate = ContabilityEntries_Model.getEntriesListBeforeDate(date, enterprise, account, false, participantCode);
                        ContabilityEntries_Model.conciliateKeysOnDatabase(enterprise, listToConciliate, Boolean.TRUE);
                    }
                } else {
                    //Sai do foreach
                    throw new Error("Break");
                }
            });
        } catch (Error e) {
            //Saiu do foreach
        }

        //Quebra linha porque tava usando somente print
        System.out.println();

        refreshNotConcileds();
    }

    /**
     * Retorna um mapa com as datas dos lançamentos não conciliados
     * @param participantCode Codigo do participante, deixe nulo para qualquer participante
     */
    private Map<Long, Calendar> getNotConciledsCalendarsMap(Integer participantCode) {
        //Cria mapa que irá ordenar por data
        Map<Long, Calendar> datesReversed = new TreeMap<>();

        //Cria calendar na data de hoje
        Calendar now = Calendar.getInstance();

        //Percorre não conciliados para pegar as datas
        notConcileds.forEach((key, entry) -> {
            //Se o participante de credito ou debito for o participante
            if (participantCode == null || entry.isParticipant(participantCode)) {
                //Pega diferença em dias
                Long diffDays = (now.getTimeInMillis() - entry.getDate().getTimeInMillis()) / (24 * 60 * 60 * 1000);

                datesReversed.put(diffDays, entry.getDate());
            }
        });

        return datesReversed;
    }

    /**
     * Percorre todos lançamentos e procura por valores iguais, múltiplos
     *
     * @param participant Codigo do participante
     */
    private void conciliateByValues(Integer participant) {
        //Loading
        Loading loading = new Loading("Conciliando por valor Participante " + participant, 0, notConcileds.size());
        int i = 0;

        //Percorre todos lançamentos
        for (Entry<Integer, ContabilityEntry> entry : notConcileds.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();

            //Loading
            i++;
            loading.updateBar(i + " de " + notConcileds.size(), i);

            //Se não estiver conciliado
            if (!ce.isConciliated()) {

                //Define onde a conta está, se em crédito ou em débito 
                Integer participantType = ce.getAccountCredit().equals(account) ? PARTICIPANT_TYPE_CREDIT : PARTICIPANT_TYPE_DEBIT;

                //Define os totais de credito e débito
                Map<Integer, ContabilityEntry> credits = new LinkedHashMap<>();
                Map<Integer, ContabilityEntry> debits = new LinkedHashMap<>();
                BigDecimal totalCredits = new BigDecimal("0.00");
                BigDecimal totalDebits = new BigDecimal("0.00");

                //Coloca o próprio valor do lançamento atual verificado na soma                
                if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                    credits.put(key, ce);
                    totalCredits = totalCredits.add(ce.getValue());
                } else if (participantType.equals(PARTICIPANT_TYPE_DEBIT)) {
                    debits.put(key, ce);
                    totalDebits = totalDebits.add(ce.getValue());
                }

                //Enquanto não tiver procurado mais do que o proprio numero de lançamentos e o crédito for diferente do débito
                while (totalCredits.compareTo(totalDebits) != 0) {

                    /* --- VALOR IGUAL EXATO ---*/
                    //Define diferença atual para procurá-la
                    BigDecimal diference;

                    //Se o participante estiver no credito, procura no debito
                    if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                        //Define valor da difereça
                        diference = totalCredits.add(totalDebits.negate());

                        //Procura lançamento com valor exato que não esteja nos debitos e adiciona nos totais de achar
                        if (findEntryWithDiference(debits, totalDebits, diference, PARTICIPANT_TYPE_CREDIT, participant)) {
                            //Se encontrou o valor reverso exato, vai fechar então não precisa mais tentar fechar
                            break;
                        }
                    } else {
                        //Define valor da diferença
                        diference = totalDebits.add(totalCredits.negate());

                        //Procura lançamento com valor exato que não esteja nos debitos e adiciona nos totais de achar
                        if (findEntryWithDiference(credits, totalCredits, diference, PARTICIPANT_TYPE_DEBIT, participant)) {
                            //Adicionou o credito, entao pode vazar
                            break;
                        }
                    }

                    /* --- VALOR MULTIPLO --- */
                    //Procura valor multiplo
                    ContabilityEntry multipleEntry;

                    multipleEntry = getEntryMultipleOfValue(
                            participantType,
                            participant,
                            ce.getValue(),
                            diference,
                            participantType.equals(PARTICIPANT_TYPE_CREDIT) ? debits : credits
                    );

                    //Se existir o múltiplo, adiciona na lista de reversa
                    if (multipleEntry != null) {
                        if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                            debits.put(multipleEntry.getKey(), multipleEntry);
                            totalDebits = totalDebits.add(multipleEntry.getValue());
                        } else {
                            credits.put(multipleEntry.getKey(), multipleEntry);
                            totalCredits = totalCredits.add(multipleEntry.getValue());
                        }
                    } else {
                        //Se nao tiver mais multiplos, tem que parar de procurar
                        break;
                    }

                }

                //Verifica se debito fecha com credito
                if (totalCredits.compareTo(totalDebits) == 0) {
                    ContabilityEntries_Model.defineConciliatedsTo(debits, Boolean.TRUE);
                    ContabilityEntries_Model.defineConciliatedsTo(credits, Boolean.TRUE);

                    //Remove dos nao conciliados, refresh iria demorar mais
                    Map<Integer, ContabilityEntry> mapToRemove = new TreeMap<>();
                    mapToRemove.putAll(debits);
                    mapToRemove.putAll(credits);
                    removeOfNotConcileds(mapToRemove);
                }
            }
        }

        //Acaba com a barra
        loading.dispose();
    }

    /**
     * Encontra o lançamento com valor reverso (diferença) e se encontrar
     * adiciona no mapa informado e soma o valor no total informado
     *
     * @param mapValues mapa com valores que serão implementados caso ache
     * @param total Total do mapa
     * @param diference Total do valor maior
     * @param totalReverse total do valor menor(reverso, valor procurado)
     * @param participantReversePredicate Predicato para filtrar o participante
     * reverso
     *
     * @return Retorna TRUE caso encontre e implemente no mapa
     * @return Retorna FALSE caso não encontre e não implementa no mapa
     */
    private boolean findEntryWithDiference(Map<Integer, ContabilityEntry> mapValues, BigDecimal total, BigDecimal diference, Integer participantReverseType, Integer participantReverse) {
        //Procura lançamento com valor igual
        ContabilityEntry entry = getEntryWithValue(participantReverseType, participantReverse, diference, mapValues);
        //Se encontrar adiciona na lista de debitos
        if (entry != null) {
            mapValues.put(entry.getKey(), entry);
            total = total.add(diference);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retorna ou não lançamento com o valor múltiplo do valor pesquisado
     *
     * @param map Mapa que o lançamento não pode estar
     * @param predicateParticipant Predicato para filtrar o participante
     * @param diference Valor procurado
     *
     * @return Lançamento opcional, pode ser verificado com "isPresent"
     */
    private ContabilityEntry getEntryMultipleOfValue(Integer typeParticipant, Integer participant, BigDecimal value, BigDecimal diference, Map<Integer, ContabilityEntry> map) {
        for (Entry<Integer, ContabilityEntry> entry : notConcileds.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();

            //Se o valor
            if (ce.getValue().compareTo(diference) == -1 //Menor que a diferenca
                    && !map.containsKey(key) //nao estiver no mapa
                    && ((typeParticipant.equals(PARTICIPANT_TYPE_CREDIT)// o tipo for de crédito E
                    && ce.getParticipantCredit().equals(participant)) //o participante de credito for o participante
                    || (typeParticipant.equals(PARTICIPANT_TYPE_DEBIT)// ou o tipo for de debito E
                    && ce.getParticipantDebit().equals(participant)))//o participante de debito for o participante
                    && value.remainder(ce.getValue()).compareTo(BigDecimal.ZERO) == 1) {
                return ce;
            }
        }

        return null;
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
    private ContabilityEntry getEntryWithValue(Integer typeParticipant, Integer participant, BigDecimal value, Map<Integer, ContabilityEntry> map) {
        for (Entry<Integer, ContabilityEntry> entry : notConcileds.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();

            //Se o valor for igual ao procurado e não estiver no mapa definido
            if (ce.getValue().compareTo(value) == 0
                    && !map.containsKey(key)
                    && ((typeParticipant.equals(PARTICIPANT_TYPE_CREDIT) && ce.getParticipantCredit().equals(participant))
                    || (typeParticipant.equals(PARTICIPANT_TYPE_DEBIT) && ce.getParticipantDebit().equals(participant)))) {
                return ce;
            }
        }

        return null;
    }

    /**
     * Concilia valores do participante conforme proximos valores contrários.
     * Para cada lançamento tenta fechar com os proximos lançamentos contrários.
     *
     * @param participant Codigo de participante
     */
    private void conciliateByAfterValues(Integer participant) {
        //Cria predicatos
        Predicate<Entry<Integer, ContabilityEntry>> participantCreditPredicate = conciledPredicate.negate().and(accountPredicateCredit.and(e -> e.getValue().getParticipantCredit().equals(participant)));
        Predicate<Entry<Integer, ContabilityEntry>> participantDebitPredicate = conciledPredicate.negate().and(accountPredicateDebit.and(e -> e.getValue().getParticipantDebit().equals(participant)));

        //Loading
        Loading loading = new Loading("Conciliando por valor Participante " + participant, 0, entries.size());
        int i = 0;

        //Percorre todos lançamentos
        for (Entry<Integer, ContabilityEntry> entry : entries.entrySet()) {
            Integer key = entry.getKey();
            ContabilityEntry ce = entry.getValue();

            i++;
            loading.updateBar(i + " de " + entries.size(), i);

            //Se não estiver conciliado
            if (!ce.isConciliated()) {
                //Define onde a conta está, se em crédito ou em débito 
                Integer participantType = ce.getAccountCredit().equals(account) ? PARTICIPANT_TYPE_CREDIT : PARTICIPANT_TYPE_DEBIT;

                Predicate<Entry<Integer, ContabilityEntry>> reverseParticipantPredicate;

                //Define predicatos dos participantes                
                if (participantType.equals(PARTICIPANT_TYPE_CREDIT)) {
                    reverseParticipantPredicate = participantDebitPredicate;
                } else {
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

        loading.dispose();
    }
}
