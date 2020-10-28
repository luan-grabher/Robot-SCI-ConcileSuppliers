package robot_conciliate.Control;

import Auxiliar.Valor;
import Dates.Dates;
import Entity.Executavel;
import SimpleDotEnv.Env;
import fileManager.FileManager;
import robot_conciliate.Model.AlterarLancamentos;
import robot_conciliate.Model.ConciliarLancamentos;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import lctocontabil.Entity.ComandosSql;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Model.ContabilityEntries_Model;
import lctocontabil.Model.LctoContabil_Model;
import sql.Database;

public class Controller {

    public static final String pathBancoSql = Env.get("databaseCfgFilePath");
    private final LctoContabil_Model modeloLctos;

    private final Integer codigoEmpresa;
    private final Integer contaCtb;
    private final Integer dataInicial;
    private final Integer dataFinal;
    private final String tipoContaContabil;
    
    private static String resultado = "Execução finalizada, verifique a conciliação dentro do único.";

    public Controller(ComandosSql comandosSqlLctos, Integer codEmpresa, Integer contaCTB, String tipoContaContabil, Calendar dataInicial, Calendar dataFinal) {
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
        Controller.resultado = resultado;
    }
    
    
    //---------------------------------------------------------------------------------------------------------------------
    
    private final Integer enterprise;
    private final Integer account;
    private final Integer participant;
    private final Calendar startDate;
    private final Calendar endDate;
    private final String databaseCfgFilePath = Env.get("databaseCfgFilePath");
    
    private final Map<Integer,ContabilityEntry> entries = new HashMap<>();

    public Controller(Integer enterprise, Integer account, Integer participant, Calendar startDate, Calendar finalDate) {
        this.enterprise = enterprise;
        this.account = account;
        this.participant = participant;
        this.startDate = startDate;
        this.endDate = finalDate;
    }
    
    public void setDatabase(){
        Database.setStaticObject(new Database(FileManager.getFile(databaseCfgFilePath)));
        
        if(!Database.getDatabase().testConnection()){
            throw new Error("Erro ao conectar ao banco de dados!");
        }        
    }
    
    public Map<Integer,ContabilityEntry> getDatabaseEntries(){
        entries.clear();
                
        String sql = FileManager.getText(FileManager.getFile("sql\\selectContabilityEntries.sql"));
        
        Map<String, String> swaps = new HashMap<>();
        swaps.put("enterprise", enterprise.toString());
        swaps.put("account", account.toString());
        swaps.put("participant", participant == null ? "NULL" : participant.toString());        
        swaps.put("dateStart", Dates.getCalendarInThisStringFormat(startDate, "YYYY-MM-dd"));
        swaps.put("dateEnd", Dates.getCalendarInThisStringFormat(endDate, "YYYY-MM-dd"));
        
        
        entries.putAll(ContabilityEntries_Model.getEntries(sql, swaps));
        
        return entries;
    }
    
}
