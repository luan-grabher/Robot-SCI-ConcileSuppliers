package robot_conciliate.Model;

import Dates.Dates;
import SimpleView.Loading;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
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
    //private final Map<String, String> documents = new TreeMap<>();

    private final static Integer TYPE_CREDIT = 0;
    private final static Integer TYPE_DEBIT = 1;

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

        /*Define total de lctos conciliados antes*/
        setNotConcileds();
        entriesConciledBefore = entries.size() - notConcileds.size();
    }

    /**
     * Retorna String com inforamções sobre conciliados
     *
     * @return Retorna String com inforamções sobre conciliados
     */
    public String getInfos() {

        entriesConciledAfter = entries.size() - notConcileds.size();

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
        entries.forEach((key, entry) -> {
            if (!entry.isConciliated()) {
                notConcileds.put(entry.getKey(), entry);
            }
        });
    }

    /**
     * Remove dos não conciliados dos conciliados
     */
    public void refreshNotConcileds() {
        List<Integer> toRemove = new ArrayList<>();
        notConcileds.forEach((k, entry) -> {
            //Se estiver conciliado
            if (entry.isConciliated()) {
                toRemove.add(entry.getKey());
                /*Adiciona na lista para remover*/
            }
        });

        //remove entradas não conciliadas
        toRemove.forEach((key) -> {
            notConcileds.remove(key);
        });
    }

    /**
     * Remove dos não conciliados dos conciliados
     *
     * @param removeMap removerá todos os lançamentos deste mapa do mapa de nao
     * conciliados
     */
    public void removeOfNotConcileds(Map<Integer, ContabilityEntry> removeMap) {
        //Percorre o mapa fornecido
        removeMap.forEach((key, entry) -> {
            //Remove lançamento dos nao conciliados
            notConcileds.remove(key);
        });
    }

    /**
     * Cria lista com participantes e documentos dos lançamentos ou unicamente
     * com o participante passado
     */
    public void createParticipantList() {
        //Se tiver filtro de participante
        if (participantFilter != null) {
            //Adiciona o participante na lista
            participants.put(participantFilter, participantFilter);
        } else {
            //Se não cria uma lista com os participantes das entradas
            entries.forEach((key, ce) -> {
                participants.put(ce.getParticipantCredit(), ce.getParticipantCredit());
                participants.put(ce.getParticipantDebit(), ce.getParticipantDebit());
            });
        }
    }

    /**
     * Mostra valores atuais de conciliados do participante com o Metodo Simple
     *
     * @param participant Codigo participante
     */
    private void showConciledInfos(Integer participant) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();

        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("credit", new BigDecimal("0.00"));
        totals.put("debit", new BigDecimal("0.00"));

        entries.forEach((k, e) -> {
            if (e.isConciliated()) {
                if (e.getParticipantCredit().equals(participant)) {
                    totals.put("credit", totals.get("credit").add(e.getValue()));
                } else if (e.getParticipantDebit().equals(participant)) {
                    totals.put("debit", totals.get("debit").add(e.getValue()));
                }
            }
        });

        infos.append("\n    Débito: ").append(nf.format(totals.get("debit")));
        infos.append("\n    Crédito: ").append(nf.format(totals.get("credit")));
    }

    /**
     * Concilia a lista de participantes atual
     */
    public void conciliateParticipants() {
        Loading loading = new Loading("Conciliando participantes", 0, participants.size());
        int i = 0;

        for (Map.Entry<Integer, Integer> part : participants.entrySet()) {
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                throw new Error("Ocorreu um erro no participante " + part.getKey() + ": " + sw.toString() + "\n\n");
            }

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
        //Cria lista de documentos do participante
        Map<String, BigDecimal> documentCreditTotals = new TreeMap<>();
        Map<String, BigDecimal> documentDebitTotals = new TreeMap<>();
        Map<String, BigDecimal> documents = new TreeMap<>();

        //Percorre lançamentos do participante para fazer soma dos creditos e debitos dos documentos
        notConcileds.forEach((key, ce) -> {
            if (ce.getParticipantCredit().equals(participant)) {//Se o participante for de credito
                String doc = ce.getDocument() == null ? "" : ce.getDocument();//pega doc
                BigDecimal newVal = documentCreditTotals.getOrDefault(doc, BigDecimal.ZERO).add(ce.getValue());
                documentCreditTotals.put(doc, newVal);
            } else if (ce.getParticipantDebit().equals(participant)) {//Se o participante for de debito
                String doc = ce.getDocument() == null ? "" : ce.getDocument();//pega doc
                BigDecimal newVal = documentDebitTotals.getOrDefault(doc, BigDecimal.ZERO).add(ce.getValue());
                documentDebitTotals.put(doc, newVal);
            }
        });

        //Cria lista de documentos
        documents.putAll(documentCreditTotals);
        documents.putAll(documentDebitTotals);

        //Percorre os documentos
        documents.forEach((doc, b) -> {
            //Se o credito for maior que zero e o credito for igual ao debito
            if (documentCreditTotals
                    .getOrDefault(doc, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) > 0
                    && documentCreditTotals
                            .getOrDefault(doc, BigDecimal.ZERO)
                            .compareTo(
                                    documentDebitTotals
                                            .getOrDefault(doc, BigDecimal.ZERO)) == 0) {
                //Percorre nao conciliados para conciliar
                notConcileds.forEach((key, ce) -> {
                    if (ce.getDocument().equals(doc)) { //Se for o mesmo doc
                        ce.conciliate();//concilia o lançamento
                    }
                });
            }
        });
        refreshNotConcileds(); //atualiza os lançamentos nao conciliados
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

                        try {
                            //Concilia no banco também pois depois pode ter erro, pois só conciliou os lançamentos que estou usando
                            String listToConciliate = ContabilityEntries_Model.getEntriesListBeforeDate(date, enterprise, account, false, participantCode);
                            ContabilityEntries_Model.conciliateKeysOnDatabase(enterprise, listToConciliate, Boolean.TRUE);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    //Sai do foreach
                    throw new Error("Break");
                }
            });
        } catch (Error e) {
            //Saiu do foreach
        } catch (RuntimeException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            throw new Error("Erro ao pegar saldo do aprticipante " + participantCode + sw.toString());
        }

        //Quebra linha porque tava usando somente print
        System.out.println();

        refreshNotConcileds();
    }

    /**
     * Retorna um mapa com as datas dos lançamentos não conciliados
     *
     * @param participantCode Codigo do participante, deixe nulo para qualquer
     * participante
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
        Object[] loading = new Object[]{null, (Integer) 0};
        //Loading
        loading[0] = new Loading("Conciliando por valor Participante " + participant, 0, notConcileds.size());

        //Mapa para conciliar
        Map<Integer, ContabilityEntry> mapToConciliate = new TreeMap<>();

        //Percorre todos lançamentos
        notConcileds.forEach((key, ce) -> {
            //Loading
            loading[1] = ((Integer) loading[1]) + 1;
            ((Loading) loading[0]).updateBar(loading[1] + " de " + notConcileds.size(), (Integer) loading[1]);

            //Verifica se nao está conciliado, pois no meio do processo irá conciliar alguns
            if (!ce.isConciliated()) {

                //Define onde a conta está, se em crédito ou em débito 
                Integer participantType = ce.getAccountCredit().equals(account) ? TYPE_CREDIT : TYPE_DEBIT;

                //Define os totais de credito e débito
                Map<Integer, ContabilityEntry> credits = new LinkedHashMap<>();
                Map<Integer, ContabilityEntry> debits = new LinkedHashMap<>();
                BigDecimal totalCredits = new BigDecimal("0.00");
                BigDecimal totalDebits = new BigDecimal("0.00");

                //Coloca o próprio valor do lançamento atual verificado na soma                
                if (participantType.equals(TYPE_CREDIT)) {
                    credits.put(key, ce);
                    totalCredits = totalCredits.add(ce.getValue());
                } else if (participantType.equals(TYPE_DEBIT)) {
                    debits.put(key, ce);
                    totalDebits = totalDebits.add(ce.getValue());
                }

                //Enquanto não tiver procurado mais do que o proprio numero de lançamentos e o crédito for diferente do débito
                while (totalCredits.compareTo(totalDebits) != 0) {

                    /* --- VALOR IGUAL EXATO ---*/
                    //Define diferença atual para procurá-la
                    BigDecimal diference;

                    //Se o participante estiver no credito, procura no debito
                    if (participantType.equals(TYPE_CREDIT)) {
                        //Define valor da difereça
                        diference = totalCredits.add(totalDebits.negate());

                        //Procura lançamento com valor exato que não esteja nos debitos e adiciona nos totais de achar
                        if (findEntryWithDiference(debits, totalDebits, diference, TYPE_CREDIT, participant)) {
                            //Se encontrou o valor reverso exato, vai fechar então não precisa mais tentar fechar
                            break;
                        }
                    } else {
                        //Define valor da diferença
                        diference = totalDebits.add(totalCredits.negate());

                        //Procura lançamento com valor exato que não esteja nos debitos e adiciona nos totais de achar
                        if (findEntryWithDiference(credits, totalCredits, diference, TYPE_DEBIT, participant)) {
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
                            participantType.equals(TYPE_CREDIT) ? debits : credits
                    );

                    //Se existir o múltiplo, adiciona na lista de reversa
                    if (multipleEntry != null) {
                        if (participantType.equals(TYPE_CREDIT)) {
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
                    mapToConciliate.putAll(debits);
                    mapToConciliate.putAll(credits);
                }
            }
        });

        //Remove dos nao conciliados pois dar refresh iria demorar mais
        removeOfNotConcileds(mapToConciliate);

        //Acaba com a barra
        ((Loading) loading[0]).dispose();
    }

    /**
     * Encontra o lançamento com valor reverso (diferença) e se encontrar
     * adiciona no mapa informado e soma o valor no total informado
     *
     * @param mapValues mapa com valores que serão implementados caso ache
     * @param total Total do mapa
     * @param diference Total do valor maior
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
     * Retorna ou não o lançamento com o valor múltiplo do valor pesquisado que
     * não esteja no mapa passado
     *
     * @param map Mapa que o lançamento não pode estar
     * @param predicateParticipant Predicato para filtrar o participante
     * @param diference Valor procurado
     *
     * @return Lançamento opcional, pode ser verificado com "isPresent"
     */
    private ContabilityEntry getEntryMultipleOfValue(Integer typeParticipant, Integer participant, BigDecimal value, BigDecimal diference, Map<Integer, ContabilityEntry> map) {
        ContabilityEntry[] return0 = new ContabilityEntry[]{null};
        /*Se o valor for maior que zero*/
        if (value.compareTo(BigDecimal.ZERO) == 1) {
            notConcileds.forEach((key, ce) -> {
                //Se o valor
                if ( //O valor for maior do que zero
                        ce.getValue().compareTo(BigDecimal.ZERO) == 1
                        /*O valor seja menor ou igual a diferença*/
                        && ce.getValue().compareTo(diference) <= 0
                        /*Não esteja no mapa indicado*/
                        && !map.containsKey(key)
                        /*o tipo do participante seja de credito*/
                        && ((typeParticipant.equals(TYPE_CREDIT)
                        /*E o participante de credito for o mesmo participante*/
                        && ce.getParticipantCredit().equals(participant))
                        /* OU o tipo for de debito */
                        || (typeParticipant.equals(TYPE_DEBIT)
                        /*E o participante de debito for o participante*/
                        && ce.getParticipantDebit().equals(participant)))
                        /*Se for múltiplo do valor*/
                        && value.remainder(ce.getValue()).compareTo(BigDecimal.ZERO) == 1) {
                    return0[0] = ce;
                }
            });
        }

        return return0[0];
    }

    /**
     * Retorna ou não lançamento com o valor procurado
     *
     * @param map Mapa que o lançamento não pode estar
     * @param value Valor procurado
     *
     * @return Lançamento opcional, pode ser verificado com "isPresent"
     */
    private ContabilityEntry getEntryWithValue(Integer typeParticipant, Integer participant, BigDecimal value, Map<Integer, ContabilityEntry> map) {
        ContabilityEntry[] return0 = new ContabilityEntry[]{null};
        notConcileds.forEach((key, ce) -> {
            //Se o valor for igual ao procurado e não estiver no mapa definido
            if (ce.getValue().compareTo(value) == 0
                    && !map.containsKey(key)
                    && ((typeParticipant.equals(TYPE_CREDIT) && ce.getParticipantCredit().equals(participant))
                    || (typeParticipant.equals(TYPE_DEBIT) && ce.getParticipantDebit().equals(participant)))) {
                return0[0] = ce;
            }
        });

        return return0[0];
    }

    /**
     * Concilia valores do participante conforme proximos valores contrários.
     * Para cada lançamento tenta fechar com os proximos lançamentos contrários.
     *
     * @param participant Codigo de participante
     */
    private void conciliateByAfterValues(Integer participant) {
        //Loading
        Map<String, Object> vars = new HashMap<>();
        vars.put("count", (Integer) 0);
        vars.put("loading", (Loading) new Loading("Conciliando por valor Participante " + participant, 0, entries.size()));
        vars.put("entries size", (String) " de " + entries.size());

        //Percorre todos lançamentos
        notConcileds.forEach((key, ce) -> {
            //Atualiza
            vars.put("count", (Integer) vars.get("count") + 1);
            ((Loading) vars.get("loading"))
                    .updateBar(
                            (Integer) vars.get("count") + (String) vars.get("entries size"),
                            (Integer) vars.get("count")
                    );

            //Se não estiver conciliado
            if (!ce.isConciliated()) {
                //Define onde a conta está, se em crédito ou em débito 
                Integer entryType = ce.getAccountCredit().equals(account) ? TYPE_CREDIT : TYPE_DEBIT;

                //Cria mapa com lançamentos ordenados por data                
                SortedMap<Calendar, ContabilityEntry> reverses = new TreeMap<>();
                //Cria lista de lançamentos reversos menores e depois
                notConcileds.forEach((k, e) -> {
                    if (!e.isConciliated()
                            && e.getDate().compareTo(ce.getDate()) >= 0 //Data posterior
                            && e.getValue().compareTo(ce.getValue()) <= 0 // Valor Menor ou igual
                            /*Pega o contrario pela conta e participante*/
                            && ((entryType.equals(TYPE_DEBIT) && e.getAccountCredit().equals(account) && e.getParticipantCredit().equals(participant))
                            || (entryType.equals(TYPE_CREDIT) && e.getAccountDebit().equals(account) && e.getParticipantDebit().equals(participant)))) {
                        reverses.put(e.getDate(), e);
                    }
                });

                //Cria lista que irá receber os lançamentos para conciliar
                Map<Integer, ContabilityEntry> toConciliate = new HashMap<>();

                //Cria variavel com soma dos reversos
                BigDecimal[] calculated = new BigDecimal[]{new BigDecimal("0.00")};

                //Percorre mapa ordenado por data
                try {
                    reverses.forEach((d, e) -> {
                        //Se valor somado ao valor atual apurado reverso for menor ou igual ao valor verificado
                        if (e.getValue().add(calculated[0]).compareTo(ce.getValue()) <= 0) {
                            //Adiciona na lista de conciliar
                            toConciliate.put(e.getKey(), e);
                            //Soma no total de reversos
                            calculated[0] = calculated[0].add(e.getValue());
                        } else {
                            throw new Error("Break code!");
                        }
                    });
                } catch (Error e) {
                    //Breaked
                }

                // Se valor dos reversos for igual ao valor verificado
                if (calculated[0].compareTo(ce.getValue()) == 0) {
                    //Adiciona na lista o proprio lançamento atual
                    toConciliate.put(key, ce);
                    //Concilia lançamentos da lista de conciliar
                    ContabilityEntries_Model.defineConciliatedsTo(toConciliate, Boolean.TRUE);
                }

            }
        });

        ((Loading) vars.get("loading")).dispose();
        //Dá um refresh nos não conciliados para poder mostrar as infos mais rapido
        refreshNotConcileds();
    }
}
