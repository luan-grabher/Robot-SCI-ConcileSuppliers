package conciliarunico.Model;

import Auxiliar.Valor;
import Executor.View.Carregamento;
import conciliarunico.Control.ExecucaoControl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lctocontabil.Entity.LctoContabil;
import lctocontabil.Model.Filtro;

public class ConciliarLancamentos {

    private static final int TIPO_PARTICIPANTE = 1;
    private static final int TIPO_CONTACTB = 2;

    private static Predicate<LctoContabil> filtroContaCredito;
    private static Predicate<LctoContabil> filtroContaDebito;
    private static Predicate<LctoContabil> filtroConta;

    private static Integer codigoEmpresa;
    private static Integer contaCtb;
    private static Integer contaAtual;
    private static Integer tipoConta;
    private static List<LctoContabil> lctos;

    private static Integer dataInicial;
    private static Integer dataFinal;

    private static String tipoContaContabil;

    private static final List<Integer> contasAtuais = new ArrayList<>();

    public static void conciliar(Integer codigoEmpresa, Integer contaCtb, String tipoContaContabil, List<LctoContabil> lctos, Integer dataInicial, Integer dataFinal) {
        //Define Lançamentos
        ConciliarLancamentos.codigoEmpresa = codigoEmpresa;
        ConciliarLancamentos.contaCtb = contaCtb;
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

        double creditoConciliados = lctos.stream().filter(Filtro.conciliado().and(Filtro.contaCredito(contaCtb))).mapToDouble(l -> l.getValor().getDouble()).sum();
        double debitoConciliados = lctos.stream().filter(Filtro.conciliado().and(Filtro.contaDebito(contaCtb))).mapToDouble(l -> l.getValor().getDouble()).sum();

        StringBuilder resultado = new StringBuilder();

        resultado.append("Lctos totais: ").append(lctos.size()).append("<br>");
        resultado.append("Lctos conciliados: ").append(conciliados).append("<br>");
        resultado.append("Lctos NÃO conciliados: ").append(naoConciliados).append("<br>");
        resultado.append("TOTAIS:<br>");
        resultado.append("Débito Conciliados: ").append(creditoConciliados).append("<br>");
        resultado.append("Crédito Conciliados: ").append(debitoConciliados).append("<br>");

        ExecucaoControl.setResultado(resultado.toString());
        System.out.println(resultado.toString().replaceAll("<br>", "\n"));
    }

    private static void tiraTeima(String titulo, Integer contaContabil, int tipoConta) {
        Predicate<LctoContabil> filtroContaCredito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Credito(contaContabil) : Filtro.contaCredito(contaContabil);
        Predicate<LctoContabil> filtroContaDebito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Debito(contaContabil) : Filtro.contaDebito(contaContabil);
        Predicate<LctoContabil> filtroConta = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante(contaContabil) : Filtro.conta(contaContabil);

        List<LctoContabil> lctosConciliadosContaAtual = lctos.stream().filter(filtroConta.and(Filtro.conciliado())).collect(Collectors.toList());

        double credito = lctosConciliadosContaAtual.stream().filter(filtroContaCredito).mapToDouble(l -> l.getValor().getDouble()).sum();
        double debito = lctosConciliadosContaAtual.stream().filter(filtroContaDebito).mapToDouble(l -> l.getValor().getDouble()).sum();

        System.out.println("----------------------------");
        System.out.println(titulo);
        System.out.println("----------------------------");
        System.out.println("Conta / Participante : " + contaContabil);
        System.out.println("Débito: " + debito);
        System.out.println("Crédito: " + credito);
        System.out.println("----------------------------");
        System.out.println("----------------------------");
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

            contaAtual = conta;
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

    private static void conciliaçãoSaldo() {
        //Pega ultima data com saldo zerado

        //Define Saldo para a conta
        SaldoConta.set(codigoEmpresa, contaAtual, tipoConta, dataInicial, dataFinal);

        List<String> datasLctosConta = new ArrayList<>();

        //monta lista de datas
        lctos.stream().filter(filtroConta).forEach(l -> {
            if (!datasLctosConta.stream().anyMatch(data -> data.equals(l.getData().getString()))) {
                datasLctosConta.add(l.getData().getString());
            }
        });

        //ordena datas
        Collections.sort(datasLctosConta, Collections.reverseOrder());

        //percorre datas        
        for (String d : datasLctosConta) {

            int statusData = SaldoConta.saldoNaData(d);

            if (statusData == SaldoConta.SALDO_NULL) {
                break;
            } else if (statusData == SaldoConta.SALDO_ZERADO) {
                Integer ultimaDataInteger = Integer.valueOf(d.replaceAll("-", ""));
                //filtra todos os lançamentos da conta na data ou antes da data e concilia

                Predicate<LctoContabil> anteriorAData = l
                        -> Integer.valueOf(l.getData().getString().replaceAll("[^0-9]", "")) <= ultimaDataInteger;
                List<LctoContabil> lctosAntesDoZeramento = lctos.stream().filter(
                        filtroConta.and(anteriorAData)
                ).collect(Collectors.toList());

                conciliarLista(lctosAntesDoZeramento);

                break;
            }
        }
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
                int debOuCred = Objects.equals(contaVerificada, contaAtual) ? 1 : -1;

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

            int debCred = Objects.equals(lcto.getTerceiroCred(), contaAtual) ? cred : deb;

            //percorre proximos valores
            for (int j = i + 1; j < lctosContaAtual.size(); j++) {
                LctoContabil lctoContrario = lctosContaAtual.get(j);
                BigDecimal valorJ = lctoContrario.getValor().getBigDecimal();
                int debCredContrario = Objects.equals(lctoContrario.getTerceiroCred(), contaAtual) ? cred : deb;

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
        filtroContaCredito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Credito(contaAtual) : Filtro.contaCredito(contaAtual);
        filtroContaDebito = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante_Debito(contaAtual) : Filtro.contaDebito(contaAtual);
        filtroConta = tipoConta == TIPO_PARTICIPANTE ? Filtro.participante(contaAtual) : Filtro.conta(contaAtual);
    }
}
