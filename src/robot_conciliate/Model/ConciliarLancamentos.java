package robot_conciliate.Model;

import Auxiliar.Valor;
import Executor.View.Carregamento;
import robot_conciliate.Control.Controller;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Entity.LctoContabil;
import lctocontabil.Model.ContabilityEntries_Model;
import lctocontabil.Model.Filtro;

public class ConciliarLancamentos {

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

    public ConciliarLancamentos(Map<Integer, ContabilityEntry> entries, Integer enterprise, Integer account, Integer participant, Calendar dateStart, Calendar dateEnd) {
        this.enterprise = enterprise;
        this.accountFilter = account;
        this.participantFilter = participant;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
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

    public void conciliateParticipants() {
        for (Map.Entry<Integer, Integer> part : participants.entrySet()) {
            Integer participant = part.getKey();

            //Concilia por saldo
            conciliateByBalance(participant);
        }
    }

    /**
     * Concilia lançamentos pelo débito e crédito dos documentos
     * 
     * @param participant Código de participante para os filtros de documento de credito e debito
     */
    public void conciliateByDocuments(Integer participant) {
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
            //Compara o debito e credito
            if (balances.get("credit").compareTo(balances.get("debit")) == 0) {

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

    /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
 /*--------------------------------------FUNÇÕES DE ANTIGAS-------------------------------------------*/
    private static final int TIPO_PARTICIPANTE = 1;
    private static final int TIPO_CONTACTB = 2;

    private static Predicate<LctoContabil> filtroContaCredito;
    private static Predicate<LctoContabil> filtroContaDebito;
    private static Predicate<LctoContabil> filtroConta;

    private static Integer tipoConta;
    private static List<LctoContabil> lctos;

    private static Integer dataInicial;
    private static Integer dataFinal;

    private static String tipoContaContabil;

    private static final List<Integer> contasAtuais = new ArrayList<>();

    public static void conciliar(Integer codigoEmpresa, Integer contaCtb, String tipoContaContabil, List<LctoContabil> lctos, Integer dataInicial, Integer dataFinal) {
        //Define Lançamentos
        ConciliarLancamentos.enterprise = codigoEmpresa;
        ConciliarLancamentos.accountFilter = contaCtb;
        ConciliarLancamentos.lctos = lctos;
        ConciliarLancamentos.dataInicial = dataInicial;
        ConciliarLancamentos.dataFinal = dataFinal;
        ConciliarLancamentos.tipoContaContabil = tipoContaContabil;

        iniciarConciliação();
        exibirResultado();
    }

    public static void iniciarConciliação() {
        if (tipoContaContabil.toLowerCase().equals("participante")) {
            conciliarParticipantesOuContasCtb(TIPO_PARTICIPANTE);
        } else {
            conciliarParticipantesOuContasCtb(TIPO_CONTACTB);
        }
    }

    private static void exibirResultado() {
        long conciliados = lctos.stream().filter(Filtro.conciliado()).count();
        long naoConciliados = lctos.stream().filter(Filtro.naoConciliado()).count();

        double creditoConciliados = lctos.stream().filter(Filtro.conciliado().and(Filtro.contaCredito(accountFilter))).mapToDouble(l -> l.getValor().getDouble()).sum();
        double debitoConciliados = lctos.stream().filter(Filtro.conciliado().and(Filtro.contaDebito(accountFilter))).mapToDouble(l -> l.getValor().getDouble()).sum();

        StringBuilder resultado = new StringBuilder();

        resultado.append("Lctos totais: ").append(lctos.size()).append("<br>");
        resultado.append("Lctos conciliados: ").append(conciliados).append("<br>");
        resultado.append("Lctos NÃO conciliados: ").append(naoConciliados).append("<br>");
        resultado.append("TOTAIS:<br>");
        resultado.append("Débito Conciliados: ").append(creditoConciliados).append("<br>");
        resultado.append("Crédito Conciliados: ").append(debitoConciliados).append("<br>");

        Controller.setResultado(resultado.toString());
        System.out.println(resultado.toString().replaceAll("<br>", "\n"));
    }

    /*--------------------------------------FUNÇÕES DE CONTROLE-------------------------------------------*/
    /**
     * Faz conciliação de cada participante na lista
     */
    private static void conciliarParticipantesOuContasCtb(int tipoContaDefinida) {
        tipoConta = tipoContaDefinida;

        popularContasAtuais();

        Carregamento barra = new Carregamento("Conciliando contas", 0, contasAtuais.size());

        int count = 0;
        for (Integer conta : contasAtuais) {
            count++;
            barra.atualizar(count);

            participantFilter = conta;
            definirFiltrosConta();
            conciliarParticipanteOuContaCtb();
        }

        barra.dispose();
    }

    /**
     * Faz todos os tipos de conciliação naquele participante
     */
    private static void conciliarParticipanteOuContaCtb() {
        //Se tiver conciliado tudo, não tem porque verificar as outras coisas
        if (!conciliaçãoBruta()) {
            conciliaçãoSaldo();
            conciliarDocumentos();
            conciliaçãoValores();
            conciliaçãoPróximosValoresContasInversas();
        }
    }

    /*--------------------------------------FUNÇÕES DE LISTAS-------------------------------------------*/
    /**
     * Cria lista de todos os participantes sem repetir que estão nos
     * lançamentos
     */
    private static void popularContasAtuais() {
        for (LctoContabil lcto : lctos) {
            if (tipoConta == TIPO_PARTICIPANTE) {
                adicionaContaNaLista(lcto.getTerceiroDeb());
                adicionaContaNaLista(lcto.getTerceiroCred());
            } else {
                adicionaContaNaLista(lcto.getDeb());
                adicionaContaNaLista(lcto.getCred());
            }
        }
    }

    /**
     * Adiciona participante na lista se ele não estiver na lista
     */
    private static void adicionaContaNaLista(Integer conta) {
        if (conta > 0) {
            if (contasAtuais.stream().noneMatch(p -> Objects.equals(p, conta))) {
                contasAtuais.add(conta);
            }
        }
    }

    /**
     * Retorna lista de documentos de um participante
     */
    private static List<String> getListaDocumentosConta() {
        List<String> documentos = new ArrayList<>();
        lctos.stream().filter(filtroConta).forEach(lcto -> {
            String doc = lcto.getDocumento().getString();
            if (!doc.equals("")) {
                if (documentos.stream().noneMatch(d -> d.equals(doc))) {
                    documentos.add(doc);
                }
            }
        });

        return documentos;
    }

    /*--------------------------------------FUNÇÕES DE CONCILIAÇÃO-------------------------------------------*/
    /**
     * Confronto direto de débito VS crédito do participante
     */
    private static boolean conciliaçãoBruta() {
        Predicate<LctoContabil> filtroCredito = Filtro.naoConciliado().and(filtroContaCredito);
        Predicate<LctoContabil> filtroDebito = Filtro.naoConciliado().and(filtroContaDebito);
        return conciliação(filtroCredito, filtroDebito);
    }

    /**
     * Concilia lançamentos pelo número de documento quando o total de crédito
     * for igual ao total de débito
     */
    private static void conciliarDocumentos() {
        //Criar lista de documentos
        List<String> documentos = getListaDocumentosConta();

        //Fazer conciliação do documento
        for (String documento : documentos) {
            Predicate<LctoContabil> filtroCredito = Filtro.naoConciliado().and(Filtro.documento(documento)).and(filtroContaCredito);
            Predicate<LctoContabil> filtroDebito = Filtro.naoConciliado().and(Filtro.documento(documento)).and(filtroContaDebito);
            conciliação(filtroCredito, filtroDebito);
        }
    }

    private static void conciliaçãoValores() {
        //Percorre todos lctos, não percorre por stream de conciliados porque no meio do processo podem haver conciliações

        lctos.stream().filter(Filtro.naoConciliado().and(filtroConta)).forEach(lcto -> {
            //se não estiver conciliado
            if (!lcto.isConciliado()) {
                //Pega conta de crédito
                int contaVerificada = tipoConta == TIPO_PARTICIPANTE ? lcto.getTerceiroCred() : lcto.getCred();

                //Se o lançamento for de crédito (credito == conta atual) --> 1
                // --- CRED: 1
                // --- DEB: -1
                int debOuCred = Objects.equals(contaVerificada, participantFilter) ? 1 : -1;

                //o filtro inverso são as contas de crédito se for débito (-1)
                Predicate<LctoContabil> filtroContaInversa = debOuCred == -1 ? filtroContaCredito : filtroContaDebito;

                //Define lista para conciliar depois
                List<LctoContabil> lctosAConciliar = new ArrayList<>();
                lctosAConciliar.add(lcto);

                //Define valor
                BigDecimal valor = lcto.getValor().getBigDecimal();
                //Define diferença que falta
                BigDecimal diferenca = new BigDecimal(valor.toString());

                //Enquanto a diferença for diferente de zero
                while (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                    LctoContabil lctoEncontrado = null;

                    //Procura um valor igual a diferença
                    Optional<LctoContabil> lctoIgualDiferenca = lctos.stream().filter(
                            Filtro.naoConciliado()
                                    .and(filtroContaInversa)
                                    .and(Filtro.naoEstaNaLista(lctosAConciliar))
                                    .and(Filtro.valorIgual(diferenca))
                    ).findFirst();

                    //Se encontrar valor igual a diferença
                    if (lctoIgualDiferenca.isPresent()) {
                        lctoEncontrado = lctoIgualDiferenca.get();
                    } else {
                        //Procura valor multiplo
                        Optional<LctoContabil> lctoMultiplo = lctos.stream().filter(
                                Filtro.naoConciliado()
                                        .and(filtroContaInversa)
                                        .and(Filtro.naoEstaNaLista(lctosAConciliar))
                                        .and(Filtro.valorMenorQue(diferenca))
                                        .and(Filtro.multiploDe(valor))
                        ).findFirst();

                        if (lctoMultiplo.isPresent()) {
                            lctoEncontrado = lctoMultiplo.get();
                        } else {
                            break;
                        }
                    }

                    //Se tiver encontrado multiplo ou igual
                    if (lctoEncontrado != null) {
                        //Atualiza diferenca
                        diferenca = diferenca.subtract(lctoEncontrado.getValor().getBigDecimal());
                        lctosAConciliar.add(lctoEncontrado);
                    }
                }

                if (diferenca.compareTo(BigDecimal.ZERO) == 0) {
                    conciliarLista(lctosAConciliar);
                }
            }
        });
    }

    /**
     * Concilia se os proximos valores em contra partida fecharem com o valor
     */
    private static void conciliaçãoPróximosValoresContasInversas() {
        List<LctoContabil> lctosContaAtual = lctos.stream().filter(filtroConta.and(Filtro.naoConciliado())).collect(Collectors.toList());

        //Ordena por ordem de data e depois chave
        lctosContaAtual.sort(Comparator.comparing(l -> l.getData().getString() + l.getChave()));

        int deb = -1;
        int cred = 1;

        //Percorre todos lctos
        for (int i = 0; i < lctosContaAtual.size(); i++) {
            LctoContabil lcto = lctosContaAtual.get(i);
            BigDecimal valorLcto = lcto.getValor().getBigDecimal();
            BigDecimal valorContrario = new BigDecimal("0.00");

            List<LctoContabil> lctosConciliar = new ArrayList<>();
            lctosConciliar.add(lcto);

            int debCred = Objects.equals(lcto.getTerceiroCred(), participantFilter) ? cred : deb;

            //percorre proximos valores
            for (int j = i + 1; j < lctosContaAtual.size(); j++) {
                LctoContabil lctoContrario = lctosContaAtual.get(j);
                BigDecimal valorJ = lctoContrario.getValor().getBigDecimal();
                int debCredContrario = Objects.equals(lctoContrario.getTerceiroCred(), participantFilter) ? cred : deb;

                //Se for lcto contrario
                if (debCredContrario != debCred) {
                    BigDecimal valorContrarioComAdição = valorContrario.add(valorJ);

                    //Se valor contarrio com adição do valor for menor ou igual ao valor do lcto, adiciona
                    if (valorContrarioComAdição.compareTo(valorLcto) <= 0) {
                        valorContrario = valorContrarioComAdição;
                        lctosConciliar.add(lctoContrario);

                        if (valorLcto.compareTo(valorContrario) == 0) {
                            break;
                        }
                    }
                } else {
                    //Sai das comparações
                    break;
                }
            }

            //Se tiver achado valor contrario
            if (valorLcto.compareTo(valorContrario) == 0) {
                conciliarLista(lctosConciliar);
            }
        }
    }

    private static void conciliarLista(List<LctoContabil> lctosConciliar) {
        lctosConciliar.stream().forEach(l -> {
            l.conciliar();
        });
    }

    /**
     * Concilia lançamentos conforme filtro de crédito e débito passado
     */
    private static boolean conciliação(Predicate<LctoContabil> filtroCredito, Predicate<LctoContabil> filtroDebito) {
        //Busca total crédito e débito
        double credito = lctos.stream().filter(filtroCredito).mapToDouble(l -> l.getValor().getDouble()).sum();
        double debito = lctos.stream().filter(filtroDebito).mapToDouble(l -> l.getValor().getDouble()).sum();

        if (credito != 0.0 && debito != 0.0) {

            Valor valorCredito = new Valor(Valor.roundDouble(credito, 2));
            Valor valorDebito = new Valor(Valor.roundDouble(debito, 2));

            //Se valores fecharem
            if (valorCredito.getBigDecimal().compareTo(valorDebito.getBigDecimal()) == 0) {
                //Concilia todos lançamentos daquele participante
                lctos.stream().filter(filtroCredito.or(filtroDebito)).forEach(l -> {
                    l.conciliar();
                });

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /*--------------------------------------FUNÇÕES DE EXECUÇÃO-------------------------------------------*/
    private static void definirFiltrosConta() {
        filtroContaCredito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Credito(participantFilter) : Filtro.contaCredito(participantFilter);
        filtroContaDebito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Debito(participantFilter) : Filtro.contaDebito(participantFilter);
        filtroConta = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante(participantFilter) : Filtro.conta(participantFilter);
    }
}
