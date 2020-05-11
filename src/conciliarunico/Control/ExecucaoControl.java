package conciliarunico.Control;

import Auxiliar.Valor;
import Entity.Executavel;
import conciliarunico.Model.AlterarLancamentos;
import conciliarunico.Model.ConciliarLancamentos;
import java.util.Calendar;
import lctocontabil.Entity.ComandosSql;
import lctocontabil.Model.LctoContabil_Model;

public class ExecucaoControl {

    public static final String pathBancoSql = "\\\\zac\\robos\\Tarefas\\Arquivos\\sci.cfg";
    private final LctoContabil_Model modeloLctos;

    private final Integer codigoEmpresa;
    private final Integer contaCtb;
    private final Integer dataInicial;
    private final Integer dataFinal;
    private final String tipoContaContabil;
    
    private static String resultado = "Execução finalizada, verifique a conciliação dentro do único.";

    public ExecucaoControl(ComandosSql comandosSqlLctos, Integer codEmpresa, Integer contaCTB, String tipoContaContabil, Calendar dataInicial, Calendar dataFinal) {
        this.codigoEmpresa = codEmpresa;
        this.contaCtb = contaCTB;
        this.dataInicial = new Valor(dataInicial).getNumberFromDateDMY();
        this.dataFinal = new Valor(dataFinal).getNumberFromDateDMY();
        this.tipoContaContabil = tipoContaContabil;

        modeloLctos = new LctoContabil_Model(comandosSqlLctos, pathBancoSql);
        modeloLctos.definirLancamentos("", codEmpresa, new Valor(dataInicial).getSQLDate(), new Valor(dataFinal).getSQLDate(), contaCTB);
    }

    public class zerarConciliacaoNoBanco extends Executavel {

        @Override
        public void run() {
            //AlterarLancamentos.zerarConciliacao(modeloLctos);
            modeloLctos.zerarConciliacaoLctosAtuais();
        }
    }

    public class definirNroDocumentoNosLancamentos extends Executavel {

        @Override
        public void run() {
            AlterarLancamentos.definirNroDocto(modeloLctos.getLctos());
        }
    }

    public class conciliarLancamentos extends Executavel {

        @Override
        public void run() {
            ConciliarLancamentos.conciliar(codigoEmpresa, contaCtb, tipoContaContabil, modeloLctos.getLctos(), dataInicial, dataFinal);
        }
    }

    public class atualizarBancoDeDados extends Executavel {

        @Override
        public void run() {
            //modeloLctos.fazerUpdateNoBanco();
            
            //Se tiver pelo menos 1 lançamento
            if (modeloLctos.getLctos().size() > 0) {
                //Pega a empresa do primeiro lançamento que deve ser igual para todos
                Integer empresa = modeloLctos.getLctos().get(0).getCodigoEmpresa();

                StringBuilder chavesSeparadasConciliar = new StringBuilder();
                StringBuilder chavesSeparadasDesconciliar = new StringBuilder();

                //Percorre todos lançamentos
                modeloLctos.getLctos().stream().forEach(l -> {
                    
                    if (l.isConciliado()) {
                        //Cria lista dos conciliados
                        chavesSeparadasConciliar.append(",");
                        chavesSeparadasConciliar.append(l.getChave().toString());
                    } else {
                        //Cria Lista dos não conciliados
                        chavesSeparadasDesconciliar.append(",");
                        chavesSeparadasDesconciliar.append(l.getChave().toString());
                    }
                });

                //Remove primeira virgula e converte para string
                String chavesParaConciliar = chavesSeparadasConciliar.toString().replaceFirst(",", "");
                String chavesParaDesconciliar = chavesSeparadasDesconciliar.toString().replaceFirst(",", "");

                modeloLctos.conciliarLctosDaLista(empresa, chavesParaConciliar, "TRUE");
                modeloLctos.conciliarLctosDaLista(empresa, chavesParaDesconciliar, "FALSE");
            }
        }
    }

    public static String getResultado() {
        return resultado;
    }

    public static void setResultado(String resultado) {
        ExecucaoControl.resultado = resultado;
    }
    
    
}
