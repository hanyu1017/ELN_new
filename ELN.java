package com.systex.jbranch.app.server.fps.pms435;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.systex.jbranch.fubon.commons.FubonWmsBizLogic;
import com.systex.jbranch.platform.common.errHandle.JBranchException;
import com.systex.jbranch.platform.util.IPrimitiveMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("pms435")
@Scope("prototype")
public class PMS435 extends FubonWmsBizLogic {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void save(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            String bondId = vo.getBond_id();

            if (StringUtils.isBlank(bondId)) {
                throw new Exception("債券代號不可為空");
            }

            Map paramProd = new HashMap();
            paramProd.put("bondId", bondId); 

            // 🔍 檢查是否存在 (判斷要 UPDATE 還是 INSERT)
            List<Map<String, Object>> existCheck = this.exeQueryForMap("SELECT COUNT(*) AS CNT FROM ELN_PRODUCT WHERE BOND_ID = :bondId", paramProd);
            int count = Integer.parseInt(existCheck.get(0).get("CNT").toString());

            // ================== 【究極防彈 SQL 組裝法】 ==================
            String sTradeDate = StringUtils.isBlank(vo.getTrade_date()) ? "NULL" : "TO_DATE('" + vo.getTrade_date().substring(0, 10) + "', 'YYYY-MM-DD')";
            String sIssueDate = StringUtils.isBlank(vo.getIssue_date()) ? "NULL" : "TO_DATE('" + vo.getIssue_date().substring(0, 10) + "', 'YYYY-MM-DD')";
            String sFinalValDate = StringUtils.isBlank(vo.getFinal_valuation_date()) ? "NULL" : "TO_DATE('" + vo.getFinal_valuation_date().substring(0, 10) + "', 'YYYY-MM-DD')";
            
            String sProdType = vo.getProd_type() == null ? "NULL" : "'" + vo.getProd_type().replace("'", "''") + "'";
            String sIb = vo.getIb() == null ? "NULL" : "'" + vo.getIb().replace("'", "''") + "'";
            String sCurrency = vo.getCurrency() == null ? "NULL" : "'" + vo.getCurrency().replace("'", "''") + "'";
            String sTenor = vo.getTenor() == null ? "NULL" : "'" + vo.getTenor().replace("'", "''") + "'";
            String sKoType = vo.getKo_type() == null ? "NULL" : "'" + vo.getKo_type().replace("'", "''") + "'";
            String sKiType = vo.getKi_type() == null ? "NULL" : "'" + vo.getKi_type().replace("'", "''") + "'";

            String nUfPct = vo.getUf_pct() == null ? "NULL" : vo.getUf_pct().toString();
            String nStrikePrice = vo.getStrike_price() == null ? "NULL" : vo.getStrike_price().toString();
            String nKoPrice = vo.getKo_price() == null ? "NULL" : vo.getKo_price().toString();
            String nYieldPct = vo.getYield_pct() == null ? "NULL" : vo.getYield_pct().toString();
            String nKiPrice = vo.getKi_price() == null ? "NULL" : vo.getKi_price().toString();

            if (count > 0) {
                StringBuffer sqlUpdate = new StringBuffer();
                sqlUpdate.append("UPDATE ELN_PRODUCT SET ");
                sqlUpdate.append("TRADE_DATE = ").append(sTradeDate).append(", ");
                sqlUpdate.append("PROD_TYPE = ").append(sProdType).append(", ");
                sqlUpdate.append("IB = ").append(sIb).append(", ");
                sqlUpdate.append("CURRENCY = ").append(sCurrency).append(", ");
                sqlUpdate.append("TENOR = ").append(sTenor).append(", ");
                sqlUpdate.append("ISSUE_DATE = ").append(sIssueDate).append(", ");
                sqlUpdate.append("UF_PCT = ").append(nUfPct).append(", ");
                sqlUpdate.append("STRIKE_PRICE = ").append(nStrikePrice).append(", ");
                sqlUpdate.append("KO_PRICE = ").append(nKoPrice).append(", ");
                sqlUpdate.append("KO_TYPE = ").append(sKoType).append(", ");
                sqlUpdate.append("YIELD_PCT = ").append(nYieldPct).append(", ");
                sqlUpdate.append("KI_PRICE = ").append(nKiPrice).append(", ");
                sqlUpdate.append("KI_TYPE = ").append(sKiType).append(", ");
                sqlUpdate.append("FINAL_VALUATION_DATE = ").append(sFinalValDate).append(" ");
                sqlUpdate.append("WHERE BOND_ID = :bondId");
                
                this.exeUpdateForMap(sqlUpdate.toString(), paramProd);

                this.exeUpdateForMap("DELETE FROM ELN_RM_MAPPING WHERE BOND_ID = :bondId", paramProd);
                this.exeUpdateForMap("DELETE FROM ELN_UNDERLYING WHERE BOND_ID = :bondId", paramProd);
            } else {
                StringBuffer sqlInsert = new StringBuffer();
                sqlInsert.append("INSERT INTO ELN_PRODUCT (BOND_ID, TRADE_DATE, PROD_TYPE, IB, CURRENCY, TENOR, ISSUE_DATE, ");
                sqlInsert.append("UF_PCT, STRIKE_PRICE, KO_PRICE, KO_TYPE, YIELD_PCT, KI_PRICE, KI_TYPE, FINAL_VALUATION_DATE) ");
                sqlInsert.append("VALUES (:bondId, ").append(sTradeDate).append(", ").append(sProdType).append(", ")
                         .append(sIb).append(", ").append(sCurrency).append(", ").append(sTenor).append(", ")
                         .append(sIssueDate).append(", ").append(nUfPct).append(", ").append(nStrikePrice).append(", ")
                         .append(nKoPrice).append(", ").append(sKoType).append(", ").append(nYieldPct).append(", ")
                         .append(nKiPrice).append(", ").append(sKiType).append(", ").append(sFinalValDate).append(")");
                
                this.exeUpdateForMap(sqlInsert.toString(), paramProd);
            }

            // ======= 【寫入理專與標的】 =======
            String rmIdsStr = vo.getRm_id();
            if (StringUtils.isNotBlank(rmIdsStr)) {
                StringBuffer sqlRm = new StringBuffer("INSERT INTO ELN_RM_MAPPING (BOND_ID, RM_ID) VALUES (:bondId, :rmId)");
                for (String rmId : rmIdsStr.split(",")) {
                    if (StringUtils.isNotBlank(rmId)) {
                        Map paramRm = new HashMap(); paramRm.put("bondId", bondId); paramRm.put("rmId", rmId.trim()); this.exeUpdateForMap(sqlRm.toString(), paramRm);
                    }
                }
            }

            StringBuffer sqlUnd = new StringBuffer("INSERT INTO ELN_UNDERLYING (BOND_ID, TICKER, ENTRY_PRICE) VALUES (:bondId, :ticker, :price)");
            if(StringUtils.isNotBlank(vo.getUnd1_ticker()) && vo.getUnd1_price() != null) { Map p = new HashMap(); p.put("bondId", bondId); p.put("ticker", vo.getUnd1_ticker().trim()); p.put("price", vo.getUnd1_price()); this.exeUpdateForMap(sqlUnd.toString(), p); }
            if(StringUtils.isNotBlank(vo.getUnd2_ticker()) && vo.getUnd2_price() != null) { Map p = new HashMap(); p.put("bondId", bondId); p.put("ticker", vo.getUnd2_ticker().trim()); p.put("price", vo.getUnd2_price()); this.exeUpdateForMap(sqlUnd.toString(), p); }
            if(StringUtils.isNotBlank(vo.getUnd3_ticker()) && vo.getUnd3_price() != null) { Map p = new HashMap(); p.put("bondId", bondId); p.put("ticker", vo.getUnd3_ticker().trim()); p.put("price", vo.getUnd3_price()); this.exeUpdateForMap(sqlUnd.toString(), p); }
            if(StringUtils.isNotBlank(vo.getUnd4_ticker()) && vo.getUnd4_price() != null) { Map p = new HashMap(); p.put("bondId", bondId); p.put("ticker", vo.getUnd4_ticker().trim()); p.put("price", vo.getUnd4_price()); this.exeUpdateForMap(sqlUnd.toString(), p); }
            if(StringUtils.isNotBlank(vo.getUnd5_ticker()) && vo.getUnd5_price() != null) { Map p = new HashMap(); p.put("bondId", bondId); p.put("ticker", vo.getUnd5_ticker().trim()); p.put("price", vo.getUnd5_price()); this.exeUpdateForMap(sqlUnd.toString(), p); }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_SAVE", "商品儲存失敗：" + e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void delete(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            String bondId = vo.getBond_id();

            if (StringUtils.isNotBlank(bondId)) {
                Map param = new HashMap();
                param.put("bondId", bondId);
                this.exeUpdateForMap("DELETE FROM ELN_RM_MAPPING WHERE BOND_ID = :bondId", param);
                this.exeUpdateForMap("DELETE FROM ELN_UNDERLYING WHERE BOND_ID = :bondId", param);
                this.exeUpdateForMap("DELETE FROM ELN_PRODUCT WHERE BOND_ID = :bondId", param);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_DEL", "商品刪除失敗：" + e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void query(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            PMS435OutputVO outputVO = new PMS435OutputVO();
            StringBuffer sb = new StringBuffer();
            Map param = new HashMap();

            sb.append("WITH RankedUnderlyings AS ( ");
            sb.append("    SELECT u.BOND_ID, u.TICKER, u.ENTRY_PRICE, pm.CURRENT_PRICE, ROW_NUMBER() OVER(PARTITION BY u.BOND_ID ORDER BY u.TICKER) as RN ");
            sb.append("    FROM ELN_UNDERLYING u LEFT JOIN ELN_PRICE_MONITOR pm ON u.TICKER = pm.TICKER "); 
            sb.append("), PivotUnderlyings AS ( ");
            sb.append("    SELECT BOND_ID, ");
            sb.append("    MAX(CASE WHEN RN = 1 THEN TICKER END) AS UND1_TICKER, MAX(CASE WHEN RN = 1 THEN ENTRY_PRICE END) AS UND1_PRICE, MAX(CASE WHEN RN = 1 THEN CURRENT_PRICE END) AS UND1_CURRENT, ");
            sb.append("    MAX(CASE WHEN RN = 2 THEN TICKER END) AS UND2_TICKER, MAX(CASE WHEN RN = 2 THEN ENTRY_PRICE END) AS UND2_PRICE, MAX(CASE WHEN RN = 2 THEN CURRENT_PRICE END) AS UND2_CURRENT, ");
            sb.append("    MAX(CASE WHEN RN = 3 THEN TICKER END) AS UND3_TICKER, MAX(CASE WHEN RN = 3 THEN ENTRY_PRICE END) AS UND3_PRICE, MAX(CASE WHEN RN = 3 THEN CURRENT_PRICE END) AS UND3_CURRENT, ");
            sb.append("    MAX(CASE WHEN RN = 4 THEN TICKER END) AS UND4_TICKER, MAX(CASE WHEN RN = 4 THEN ENTRY_PRICE END) AS UND4_PRICE, MAX(CASE WHEN RN = 4 THEN CURRENT_PRICE END) AS UND4_CURRENT, ");
            sb.append("    MAX(CASE WHEN RN = 5 THEN TICKER END) AS UND5_TICKER, MAX(CASE WHEN RN = 5 THEN ENTRY_PRICE END) AS UND5_PRICE, MAX(CASE WHEN RN = 5 THEN CURRENT_PRICE END) AS UND5_CURRENT ");
            sb.append("    FROM RankedUnderlyings GROUP BY BOND_ID ");
            sb.append(") ");
            sb.append("SELECT p.BOND_ID, TO_CHAR(p.TRADE_DATE, 'yyyy-MM-dd') AS TRADE_DATE, p.PROD_TYPE, p.IB, p.CURRENCY, p.TENOR, TO_CHAR(p.ISSUE_DATE, 'yyyy-MM-dd') AS ISSUE_DATE, ");
            sb.append("p.UF_PCT, p.STRIKE_PRICE, p.KO_PRICE, p.KO_TYPE, p.YIELD_PCT, p.KI_PRICE, p.KI_TYPE, TO_CHAR(p.FINAL_VALUATION_DATE, 'yyyy-MM-dd') AS FINAL_VALUATION_DATE, ");
            sb.append("(SELECT LISTAGG(RM_ID, ', ') WITHIN GROUP (ORDER BY RM_ID) FROM ELN_RM_MAPPING rm WHERE rm.BOND_ID = p.BOND_ID) AS RM_ID, ");
            sb.append("COALESCE((SELECT CASE WHEN MIN(ph.CLOSE_PRICE / NULLIF(u2.ENTRY_PRICE, 0) * 100) <= p.KI_PRICE THEN 'Y' ELSE 'N' END ");
            sb.append(" FROM ELN_PRICE_HISTORY ph JOIN ELN_UNDERLYING u2 ON ph.TICKER = u2.TICKER WHERE u2.BOND_ID = p.BOND_ID AND ph.PRICE_DATE >= p.ISSUE_DATE AND p.KI_TYPE = 'AKI'), 'N') AS HAS_TOUCHED_AKI, ");
            sb.append("u.UND1_TICKER, u.UND1_PRICE, u.UND1_CURRENT, u.UND2_TICKER, u.UND2_PRICE, u.UND2_CURRENT, ");
            sb.append("u.UND3_TICKER, u.UND3_PRICE, u.UND3_CURRENT, u.UND4_TICKER, u.UND4_PRICE, u.UND4_CURRENT, u.UND5_TICKER, u.UND5_PRICE, u.UND5_CURRENT ");
            sb.append("FROM ELN_PRODUCT p LEFT JOIN PivotUnderlyings u ON p.BOND_ID = u.BOND_ID WHERE 1=1 ");

            if (vo != null && StringUtils.isNotBlank(vo.getBond_id())) {
                sb.append("AND p.BOND_ID LIKE :bondId "); param.put("bondId", "%" + vo.getBond_id().trim() + "%");
            }
            if (vo != null && StringUtils.isNotBlank(vo.getRm_id())) {
                sb.append("AND EXISTS (SELECT 1 FROM ELN_RM_MAPPING rm WHERE rm.BOND_ID = p.BOND_ID AND rm.RM_ID = :rmId) "); param.put("rmId", vo.getRm_id().trim());
            }
            sb.append("ORDER BY p.TRADE_DATE DESC ");

            List<Map<String, Object>> resultList = this.exeQueryForMap(sb.toString(), param);
            outputVO.setResultList(resultList);
            this.sendRtnObject(outputVO);

        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_QUERY", "查詢商品失敗：" + e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void queryTickers(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435OutputVO outputVO = new PMS435OutputVO();
            StringBuffer sb = new StringBuffer("SELECT DISTINCT TICKER FROM ELN_UNDERLYING WHERE TICKER IS NOT NULL ORDER BY TICKER");
            List<Map<String, Object>> rows = this.exeQueryForMap(sb.toString(), new HashMap());
            List<String> tickerList = new ArrayList<>();
            for (Map<String, Object> row : rows) { tickerList.add((String) row.get("TICKER")); }
            outputVO.setTickerList(tickerList);
            this.sendRtnObject(outputVO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_TICKER", "載入標的清單失敗：" + e.getMessage());
        }
    }
}
