package br.com.sankhya.truss;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;

public class BotaoDevolucao implements AcaoRotinaJava {

    BigDecimal nunota;
    BigDecimal codprod;
    BigDecimal codemp;
    String controle;
    String observacao;
   
    

    public void doAction(ContextoAcao contexto) throws Exception {

        JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
        NativeSql sql = new NativeSql(jdbc);

        if (!contexto.confirmarSimNao("Confirma", "Deseja realizar a geração das etiquetas de devolução?", 1))
            return;

        int countDev = -1; 
        int countPrd = -1;
        int countEtiq = -1;
        int qtdEtiq = -1;
     
            this.nunota = new BigDecimal(contexto.getParam("NUNOTA").toString());     
            this.codprod = new BigDecimal(contexto.getParam("CODPROD").toString());
            this.codemp = new BigDecimal(contexto.getParam("CODEMP").toString());
            this.controle = new String (contexto.getParam("CONTROLE").toString());
            this.observacao = new String (contexto.getParam("OBSERVACAO").toString());
                

            
            String validaDevol = "SELECT COUNT(1) VALIDA FROM TGFTOP WHERE CODTIPOPER = (SELECT CODTIPOPER FROM TGFCAB WHERE NUNOTA = "
                    + this.nunota
                    + ") AND DHALTER = (SELECT DHTIPOPER FROM TGFCAB WHERE NUNOTA = "
                    + this.nunota + ") AND TIPMOV = 'D'";

            ResultSet rsValidaDevol = sql.executeQuery(validaDevol);

            if (rsValidaDevol.next()) {
              
            	countDev = rsValidaDevol.getInt("VALIDA");

                if (countDev >= 1) {
                  
                	String valprodNota = 
                            "SELECT count(1)  VALIDA"+
                            "  FROM TGFITE " +
                            " WHERE NUNOTA = " + this.nunota + 
                            "   AND CODPROD = " + this.codprod + 
                            "   AND CONTROLE = '" + this.controle+
                            "'   AND CODEMP = " +this.codemp;
                	
                	   ResultSet rsValprodNota = sql.executeQuery(valprodNota);
                	   
                	   if (rsValprodNota.next()) {
                		   
                		   countPrd = rsValprodNota.getInt("VALIDA");
                		   
                		   if (countPrd>=1) {
                			   
                			   String valEtiq = 
                                       "SELECT count(1)  VALIDA"+
                                       "  FROM AD_TRASETIQUETA " +
                                       " WHERE NUNOTA = " + this.nunota + 
                                       "   AND CODPROD = " + this.codprod + 
                                       "   AND CONTROLE = '" + this.controle+
                                       "'   AND CODEMP = " +this.codemp+
                                       "    AND SITUACAO != 'CAN'";
                			   
                			   ResultSet rsValEtiq = sql.executeQuery(valEtiq);
                			   
                			   if (rsValEtiq.next()) {
                				   
                				   countEtiq = rsValEtiq.getInt("VALIDA"); 
                				 
                				   if(countEtiq==0) {
                					   
                					   String valQtdEtiq = 
                	                            "SELECT AD_QTDETIQUETAS VALIDA"+
                	                            "  FROM TGFITE " +
                	                            " WHERE NUNOTA = " + this.nunota + 
                	                            "   AND CODPROD = " + this.codprod + 
                	                            "   AND CONTROLE = '" + this.controle+
                	                            "'   AND CODEMP = " +this.codemp;
                					   ResultSet rsValQtdEtiq = sql.executeQuery(valQtdEtiq);
                					   
                					   if (rsValQtdEtiq.next()) {
                						   
                						   qtdEtiq = rsValQtdEtiq.getInt("VALIDA"); 
                						   
                						   if(qtdEtiq>=1) {
                							   
                							   int nrocaixa = 0; 
                							   
                							   for (int c1 = 1; c1 <= qtdEtiq; c1++) {
                								   
                								   nrocaixa++;  
                								   String insertEtiqueta = "INSERT INTO AD_TRASETIQUETA (CODETIQ,CODPROD,TIPOPROD,CONTROLE,SITUACAO,CODUSUINCLUSAO,DHINCLUSAO,NUNOTA,NROCAIXA,GERARMAIOR,CODLOCAL,CODEMP,OBSERVACAO) SELECT (SELECT n.ultcod FROM tgfnum n WHERE n.arquivo = 'AD_TRASETIQUETA')+" + 
                							                nrocaixa + ", " + 
                							                " CODPROD, 'PA' , CONTROLE, 'NAO', STP_GET_CODUSULOGADO, SYSDATE, " + this.nunota + "," + nrocaixa + ", 'S', CODLOCALORIG, CODEMP, '" + this.observacao + "' FROM TGFITE WHERE NUNOTA = " + this.nunota + " AND " + 
                							                " CODPROD =" + this.codprod + " AND CONTROLE = '" + this.controle + "' AND CODEMP = " + this.codemp;
                							              String updateTgfnum = "UPDATE TGFNUM SET ULTCOD = ULTCOD+" + nrocaixa + " WHERE ARQUIVO = 'AD_TRASETIQUETA'";
                							              sql.executeUpdate(insertEtiqueta);
                							              sql.executeUpdate(updateTgfnum);
                							   }
                							   
                						   }else {throw new Exception ("O campo Quantidade de etiquetas do lançamento da nota não está preenchido, por gentileza, realizar o preenchimento e tentar novamente.");
                							   
                						   }
                						   
                						   
                						   
                					   }
                					   

                					   
                				   }else {
                					   throw new Exception ("Não é possível realizar a geração da etiqueta, já existem etiquetas consumidas ou não canceladas para essa nota e produto.");
                				   }
                				   
                			   	}
                			   
                			   
                			   
                			   
                		   }else {
                			   throw new Exception("Os parâmetros informados para produto, lote ou empresa não foram encontrados. Por favor, revise as informações preenchidas e tente novamente.");

                			   
                			   
                		   }
                		   
                		   
                	   }
                          
                  
                  
                } else {
                    throw new Exception("O NU indicado: " + this.nunota
                            + " não representa um documento do tipo de Devolução, verifique e tente novamente.");
                }
            }

        
    }
}
