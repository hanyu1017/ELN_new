package com.systex.jbranch.app.server.fps.pms435;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.systex.jbranch.fubon.commons.FubonWmsBizLogic;
import com.systex.jbranch.platform.common.errHandle.APException;
import com.systex.jbranch.platform.common.errHandle.JBranchException;
import com.systex.jbranch.platform.util.IPrimitiveMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component("pms435")
@Scope("prototype")
public class PMS435 extends FubonWmsBizLogic {

    // =========================================================
    // 【CSV 批次匯入】MERGE + Delete-Insert
    //
    // CSV 欄位索引對應（前端以 TextDecoder('big5') 解碼後傳入）：
    //   0  債券代號   1  交易日      2  UF%     3  IB       4  商品類型
    //   5  天期(月)   6  幣別        7  標的1    8  標的1進場價  9  標的2
    //  10  標的2進場  11 標的3      12 標的3進場  13 標的4   14 標的4進場
    //  15  標的5     16 標的5進場   17 收益率(%)  18 履約條件(KO)  19 KO價格(%)
    //  20  KO類型    21 KI價格(%)   22 KI類型    23 發行日   24 最後評價日
    //  25  到期日    26 理專
    // =========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void importCSV(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            String csvContent = vo.getCsv_content();

            if (StringUtils.isBlank(csvContent)) {
                throw new APException("CSV 內容不可為空");
            }

            // 依換行切割，過濾純空白行
            String[] rawLines = csvContent.split("\r?\n");
            List<String[]> csvList = new ArrayList<>();
            for (String line : rawLines) {
                if (StringUtils.isNotBlank(line)) {
                    csvList.add(parseCsvLine(line));
                }
            }

            if (csvList.isEmpty()) {
                throw new APException("CSV 至少需要標題列與一筆資料");
            }

            Set<String>  idList       = new HashSet<>();
            int          successCount = 0;
            int          failCount    = 0;
            List<String> errors       = new ArrayList<>();

            for (int i = 0; i < csvList.size(); i++) {
                String[] str = csvList.get(i);

                // 第二步：檔頭防呆驗證（i == 0）
                if (i == 0) {
                    if (str.length < 2
                            || !"債券代號".equals(str[0].trim())
                            || !"交易日".equals(str[1].trim())) {
                        throw new APException("上傳格式錯誤，請下載範例檔案");
                    }
                    continue;
                }

                // 第三步：空值跳過 + 檔案內防重
                if (StringUtils.isBlank(str[0])) continue;

                String bondId = str[0].trim();
                if (idList.contains(bondId)) {
                    failCount++;
                    errors.add("第 " + (i + 1) + " 列 (" + bondId + ")：檔案內重複，略過");
                    continue;
                }
                idList.add(bondId);

                // 第四~五步：型態轉換 + MERGE / Delete-Insert
                try {
                    processSingleRow(bondId, str);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("第 " + (i + 1) + " 列 (" + bondId + ")：" + e.getMessage());
                }
            }

            PMS435OutputVO outputVO = new PMS435OutputVO();
            outputVO.setImportSuccessCount(successCount);
            outputVO.setImportFailCount(failCount);
            outputVO.setImportErrors(errors);
            this.sendRtnObject(outputVO);

        } catch (APException ape) {
            throw ape;
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_IMPORT" + "CSV 匯入失敗：" + e.getMessage());
        }
    }

    // 將 CSV 單筆資料列寫入資料庫（MERGE + Delete-Insert）
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processSingleRow(String bondId, String[] cols) throws Exception {
        // ── 解析欄位 ────────────────────────────────────────
        String sTradeDate    = toOraDate(safeGet(cols, 1));
        String sIssueDate    = toOraDate(safeGet(cols, 23));
        String sFinalValDate = toOraDate(safeGet(cols, 24));
        String sMaturityDate = toOraDate(safeGet(cols, 25));

        Map<String, Object> params = new HashMap<>();
        params.put("bondId",      bondId);
        params.put("ib",          safeGet(cols, 3));
        params.put("prodType",    safeGet(cols, 4));
        params.put("tenor",       safeGet(cols, 5));
        params.put("currency",    safeGet(cols, 6));
        params.put("yieldPct",    toDouble(safeGet(cols, 17)));
        params.put("strikePrice", toDouble(safeGet(cols, 18)));
        params.put("koPrice",     toDouble(safeGet(cols, 19)));
        params.put("koType",      safeGet(cols, 20));
        params.put("kiPrice",     toDouble(safeGet(cols, 21)));
        params.put("kiType",      safeGet(cols, 22));
        params.put("ufPct",       toDouble(safeGet(cols, 2)));

        // ── MERGE INTO ELN_PRODUCT ───────────────────────────
        StringBuilder mergeSql = new StringBuilder();
        mergeSql.append("MERGE INTO ELN_PRODUCT tgt ");
        mergeSql.append("USING (SELECT :bondId AS BOND_ID FROM DUAL) src ");
        mergeSql.append("ON (tgt.BOND_ID = src.BOND_ID) ");
        mergeSql.append("WHEN MATCHED THEN UPDATE SET ");
        mergeSql.append("  TRADE_DATE           = ").append(sTradeDate).append(", ");
        mergeSql.append("  UF_PCT               = :ufPct, ");
        mergeSql.append("  IB                   = :ib, ");
        mergeSql.append("  PROD_TYPE            = :prodType, ");
        mergeSql.append("  TENOR                = :tenor, ");
        mergeSql.append("  CURRENCY             = :currency, ");
        mergeSql.append("  YIELD_PCT            = :yieldPct, ");
        mergeSql.append("  STRIKE_PRICE         = :strikePrice, ");
        mergeSql.append("  KO_PRICE             = :koPrice, ");
        mergeSql.append("  KO_TYPE              = :koType, ");
        mergeSql.append("  KI_PRICE             = :kiPrice, ");
        mergeSql.append("  KI_TYPE              = :kiType, ");
        mergeSql.append("  ISSUE_DATE           = ").append(sIssueDate).append(", ");
        mergeSql.append("  FINAL_VALUATION_DATE = ").append(sFinalValDate).append(", ");
        mergeSql.append("  MATURITY_DATE        = ").append(sMaturityDate).append(" ");
        mergeSql.append("WHEN NOT MATCHED THEN INSERT ");
        mergeSql.append("  (BOND_ID, TRADE_DATE, UF_PCT, IB, PROD_TYPE, TENOR, CURRENCY, ");
        mergeSql.append("   YIELD_PCT, STRIKE_PRICE, KO_PRICE, KO_TYPE, KI_PRICE, KI_TYPE, ");
        mergeSql.append("   ISSUE_DATE, FINAL_VALUATION_DATE, MATURITY_DATE) ");
        mergeSql.append("VALUES ");
        mergeSql.append("  (:bondId, ").append(sTradeDate).append(", :ufPct, :ib, :prodType, :tenor, :currency, ");
        mergeSql.append("   :yieldPct, :strikePrice, :koPrice, :koType, :kiPrice, :kiType, ");
        mergeSql.append("   ").append(sIssueDate).append(", ").append(sFinalValDate).append(", ").append(sMaturityDate).append(")");

        this.exeUpdateForMap(mergeSql.toString(), params);

        // ── Delete-Insert：ELN_UNDERLYING & ELN_RM_MAPPING ──
        Map<String, Object> bondParam = new HashMap<>();
        bondParam.put("bondId", bondId);
        this.exeUpdateForMap("DELETE FROM ELN_UNDERLYING  WHERE BOND_ID = :bondId", bondParam);
        this.exeUpdateForMap("DELETE FROM ELN_RM_MAPPING  WHERE BOND_ID = :bondId", bondParam);

        // 標的：索引配對 [ticker列, 進場價列]（最多 5 組）
        int[][] undCols = {{7, 8}, {9, 10}, {11, 12}, {13, 14}, {15, 16}};
        String undSql = "INSERT INTO ELN_UNDERLYING (BOND_ID, TICKER, ENTRY_PRICE) VALUES (:bondId, :ticker, :price)";
        for (int[] uc : undCols) {
            String ticker = safeGet(cols, uc[0]);
            Double  price  = toDouble(safeGet(cols, uc[1]));
            if (StringUtils.isNotBlank(ticker) && price != null) {
                Map<String, Object> up = new HashMap<>();
                up.put("bondId", bondId);
                up.put("ticker", ticker);
                up.put("price",  price);
                this.exeUpdateForMap(undSql, up);
            }
        }

        // 理專：允許逗號分隔多位
        String rmRaw = safeGet(cols, 26);
        if (StringUtils.isNotBlank(rmRaw)) {
            String rmSql = "INSERT INTO ELN_RM_MAPPING (BOND_ID, RM_ID) VALUES (:bondId, :rmId)";
            for (String rmId : rmRaw.split(",")) {
                String rm = rmId.trim();
                if (StringUtils.isNotBlank(rm)) {
                    Map<String, Object> rp = new HashMap<>();
                    rp.put("bondId", bondId);
                    rp.put("rmId",   rm);
                    this.exeUpdateForMap(rmSql, rp);
                }
            }
        }
    }

    // =========================================================
    // 【單筆儲存/更新】保留供直接 API 呼叫，UI 已不再使用
    // =========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void save(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            String bondId = vo.getBond_id();

            if (StringUtils.isBlank(bondId)) {
                throw new Exception("債券代號不可為空");
            }

            String sTradeDate    = StringUtils.isBlank(vo.getTrade_date())            ? "NULL" : "TO_DATE('" + vo.getTrade_date().substring(0, 10)            + "','YYYY-MM-DD')";
            String sIssueDate    = StringUtils.isBlank(vo.getIssue_date())            ? "NULL" : "TO_DATE('" + vo.getIssue_date().substring(0, 10)            + "','YYYY-MM-DD')";
            String sFinalValDate = StringUtils.isBlank(vo.getFinal_valuation_date())  ? "NULL" : "TO_DATE('" + vo.getFinal_valuation_date().substring(0, 10)  + "','YYYY-MM-DD')";
            String sMaturityDate = StringUtils.isBlank(vo.getMaturity_date())         ? "NULL" : "TO_DATE('" + vo.getMaturity_date().substring(0, 10)         + "','YYYY-MM-DD')";

            Map<String, Object> params = new HashMap<>();
            params.put("bondId",      bondId);
            params.put("prodType",    vo.getProd_type()   == null ? "" : vo.getProd_type());
            params.put("ib",          vo.getIb()          == null ? "" : vo.getIb());
            params.put("currency",    vo.getCurrency()    == null ? "" : vo.getCurrency());
            params.put("tenor",       vo.getTenor()       == null ? "" : vo.getTenor());
            params.put("koType",      vo.getKo_type()     == null ? "" : vo.getKo_type());
            params.put("kiType",      vo.getKi_type()     == null ? "" : vo.getKi_type());
            params.put("ufPct",       vo.getUf_pct());
            params.put("strikePrice", vo.getStrike_price());
            params.put("koPrice",     vo.getKo_price());
            params.put("yieldPct",    vo.getYield_pct());
            params.put("kiPrice",     vo.getKi_price());

            StringBuilder mergeSql = new StringBuilder();
            mergeSql.append("MERGE INTO ELN_PRODUCT tgt ");
            mergeSql.append("USING (SELECT :bondId AS BOND_ID FROM DUAL) src ");
            mergeSql.append("ON (tgt.BOND_ID = src.BOND_ID) ");
            mergeSql.append("WHEN MATCHED THEN UPDATE SET ");
            mergeSql.append("  TRADE_DATE=").append(sTradeDate).append(", UF_PCT=:ufPct, IB=:ib, PROD_TYPE=:prodType, ");
            mergeSql.append("  TENOR=:tenor, CURRENCY=:currency, YIELD_PCT=:yieldPct, STRIKE_PRICE=:strikePrice, ");
            mergeSql.append("  KO_PRICE=:koPrice, KO_TYPE=:koType, KI_PRICE=:kiPrice, KI_TYPE=:kiType, ");
            mergeSql.append("  ISSUE_DATE=").append(sIssueDate).append(", FINAL_VALUATION_DATE=").append(sFinalValDate).append(", MATURITY_DATE=").append(sMaturityDate).append(" ");
            mergeSql.append("WHEN NOT MATCHED THEN INSERT ");
            mergeSql.append("  (BOND_ID,TRADE_DATE,UF_PCT,IB,PROD_TYPE,TENOR,CURRENCY,YIELD_PCT,STRIKE_PRICE,KO_PRICE,KO_TYPE,KI_PRICE,KI_TYPE,ISSUE_DATE,FINAL_VALUATION_DATE,MATURITY_DATE) ");
            mergeSql.append("VALUES (:bondId,").append(sTradeDate).append(",:ufPct,:ib,:prodType,:tenor,:currency,:yieldPct,:strikePrice,:koPrice,:koType,:kiPrice,:kiType,");
            mergeSql.append(sIssueDate).append(",").append(sFinalValDate).append(",").append(sMaturityDate).append(")");

            this.exeUpdateForMap(mergeSql.toString(), params);

            // Delete-Insert 子表
            Map<String, Object> bondParam = new HashMap<>();
            bondParam.put("bondId", bondId);
            this.exeUpdateForMap("DELETE FROM ELN_RM_MAPPING WHERE BOND_ID = :bondId", bondParam);
            this.exeUpdateForMap("DELETE FROM ELN_UNDERLYING  WHERE BOND_ID = :bondId", bondParam);

            String rmIdsStr = vo.getRm_id();
            if (StringUtils.isNotBlank(rmIdsStr)) {
                for (String rmId : rmIdsStr.split(",")) {
                    if (StringUtils.isNotBlank(rmId)) {
                        Map<String, Object> rp = new HashMap<>();
                        rp.put("bondId", bondId); rp.put("rmId", rmId.trim());
                        this.exeUpdateForMap("INSERT INTO ELN_RM_MAPPING (BOND_ID, RM_ID) VALUES (:bondId, :rmId)", rp);
                    }
                }
            }

            String undSql = "INSERT INTO ELN_UNDERLYING (BOND_ID, TICKER, ENTRY_PRICE) VALUES (:bondId, :ticker, :price)";
            Object[][] unds = {
                {vo.getUnd1_ticker(), vo.getUnd1_price()}, {vo.getUnd2_ticker(), vo.getUnd2_price()},
                {vo.getUnd3_ticker(), vo.getUnd3_price()}, {vo.getUnd4_ticker(), vo.getUnd4_price()},
                {vo.getUnd5_ticker(), vo.getUnd5_price()}
            };
            for (Object[] u : unds) {
                if (StringUtils.isNotBlank((String) u[0]) && u[1] != null) {
                    Map<String, Object> up = new HashMap<>();
                    up.put("bondId", bondId); up.put("ticker", ((String) u[0]).trim()); up.put("price", u[1]);
                    this.exeUpdateForMap(undSql, up);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_SAVE" + "商品儲存失敗：" + e.getMessage());
        }
    }

    // =========================================================
    // 【刪除】
    // =========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void delete(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            String bondId = vo.getBond_id();

            if (StringUtils.isNotBlank(bondId)) {
                Map<String, Object> param = new HashMap<>();
                param.put("bondId", bondId);
                this.exeUpdateForMap("DELETE FROM ELN_RM_MAPPING WHERE BOND_ID = :bondId", param);
                this.exeUpdateForMap("DELETE FROM ELN_UNDERLYING  WHERE BOND_ID = :bondId", param);
                this.exeUpdateForMap("DELETE FROM ELN_PRODUCT      WHERE BOND_ID = :bondId", param);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_DEL" + "商品刪除失敗：" + e.getMessage());
        }
    }

    // =========================================================
    // 【查詢】
    //
    // 燈號邏輯（KI 優先於 KO）：
    //   🔴 KI 觸發：ELN_PRICE_HISTORY 中首次 CLOSE_PRICE <= ENTRY_PRICE * KI_PRICE / 100
    //   🟢 KO 觸發：ELN_PRICE_HISTORY 中首次 CLOSE_PRICE >= ENTRY_PRICE * KO_PRICE / 100
    //   ⚪ 觀察中 ：兩者皆未觸及
    //
    // 閉鎖期邏輯（KO_TYPE 含 NC\d+M，如 NC2M、NC3M）：
    //   REGEXP_LIKE(KO_TYPE, '^NC[0-9]+M$') AND SYSDATE < ADD_MONTHS(ISSUE_DATE, N)
    //   → 閉鎖中；否則 → 觀察中；MATURITY_DATE < SYSDATE → 已到期
    // =========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void query(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435InputVO vo = (PMS435InputVO) body;
            PMS435OutputVO outputVO = new PMS435OutputVO();
            Map<String, Object> param = new HashMap<>();

            StringBuilder sb = new StringBuilder();

            // CTE 1：計算每標的的 KI/KO 首次觸發日（動態比對 ELN_PRICE_HISTORY）
            sb.append("WITH UnderlyingDetails AS ( ");
            sb.append("  SELECT ");
            sb.append("    u.BOND_ID, u.TICKER, u.ENTRY_PRICE, pm.CURRENT_PRICE, ");
            sb.append("    ROW_NUMBER() OVER (PARTITION BY u.BOND_ID ORDER BY u.TICKER) AS RN, ");
            // KI 觸發日：發行日起，首次收盤 <= 進場價 × KI%
            sb.append("    (SELECT MIN(ph.PRICE_DATE) FROM ELN_PRICE_HISTORY ph ");
            sb.append("     WHERE ph.TICKER = u.TICKER AND ph.PRICE_DATE >= p.ISSUE_DATE ");
            sb.append("       AND ph.CLOSE_PRICE <= u.ENTRY_PRICE * p.KI_PRICE / 100) AS KI_DATE, ");
            // KO 觸發日：發行日起，首次收盤 >= 進場價 × KO%
            sb.append("    (SELECT MIN(ph.PRICE_DATE) FROM ELN_PRICE_HISTORY ph ");
            sb.append("     WHERE ph.TICKER = u.TICKER AND ph.PRICE_DATE >= p.ISSUE_DATE ");
            sb.append("       AND ph.CLOSE_PRICE >= u.ENTRY_PRICE * p.KO_PRICE / 100) AS KO_DATE ");
            sb.append("  FROM ELN_UNDERLYING u ");
            sb.append("  JOIN ELN_PRODUCT p ON u.BOND_ID = p.BOND_ID ");
            sb.append("  LEFT JOIN ELN_PRICE_MONITOR pm ON u.TICKER = pm.TICKER ");
            sb.append("), ");

            // CTE 2：將最多 5 個標的橫轉為欄位（Pivot）
            sb.append("PivotUnderlyings AS ( ");
            sb.append("  SELECT BOND_ID, ");
            for (int n = 1; n <= 5; n++) {
                sb.append("    MAX(CASE WHEN RN=").append(n).append(" THEN TICKER END)        AS UND").append(n).append("_TICKER, ");
                sb.append("    MAX(CASE WHEN RN=").append(n).append(" THEN ENTRY_PRICE END)   AS UND").append(n).append("_ENTRY_PRICE, ");
                sb.append("    MAX(CASE WHEN RN=").append(n).append(" THEN CURRENT_PRICE END) AS UND").append(n).append("_CURRENT_PRICE, ");
                sb.append("    MAX(CASE WHEN RN=").append(n).append(" THEN KI_DATE END)       AS UND").append(n).append("_KI_DATE, ");
                String sep = (n < 5) ? ", " : " ";
                sb.append("    MAX(CASE WHEN RN=").append(n).append(" THEN KO_DATE END)       AS UND").append(n).append("_KO_DATE").append(sep);
            }
            sb.append("  FROM UnderlyingDetails GROUP BY BOND_ID ");
            sb.append(") ");

            // 主查詢
            sb.append("SELECT ");
            sb.append("  p.BOND_ID, ");
            sb.append("  TO_CHAR(p.TRADE_DATE,'yyyy-MM-dd')            AS TRADE_DATE, ");
            sb.append("  p.PROD_TYPE, p.IB, p.CURRENCY, p.TENOR, p.UF_PCT, ");
            sb.append("  p.STRIKE_PRICE, p.KO_PRICE, p.KO_TYPE, ");
            sb.append("  p.YIELD_PCT, p.KI_PRICE, p.KI_TYPE, ");
            sb.append("  TO_CHAR(p.ISSUE_DATE,'yyyy-MM-dd')            AS ISSUE_DATE, ");
            sb.append("  TO_CHAR(p.FINAL_VALUATION_DATE,'yyyy-MM-dd')  AS FINAL_VALUATION_DATE, ");
            sb.append("  TO_CHAR(p.MATURITY_DATE,'yyyy-MM-dd')         AS MATURITY_DATE, ");
            // 閉鎖期狀態：已到期 > 閉鎖中 > 觀察中
            sb.append("  CASE ");
            sb.append("    WHEN p.MATURITY_DATE IS NOT NULL AND SYSDATE > p.MATURITY_DATE THEN '已到期' ");
            sb.append("    WHEN REGEXP_LIKE(TRIM(p.KO_TYPE),'^NC[0-9]+M$') ");
            sb.append("         AND p.ISSUE_DATE IS NOT NULL ");
            sb.append("         AND SYSDATE < ADD_MONTHS(p.ISSUE_DATE, TO_NUMBER(REGEXP_SUBSTR(TRIM(p.KO_TYPE),'[0-9]+'))) ");
            sb.append("    THEN '閉鎖中' ");
            sb.append("    ELSE '觀察中' ");
            sb.append("  END AS LOCKOUT_STATUS, ");
            // 理專（scalar subquery 避免 LISTAGG GROUP BY 膨脹）
            sb.append("  (SELECT LISTAGG(rm.RM_ID,', ') WITHIN GROUP (ORDER BY rm.RM_ID) ");
            sb.append("   FROM ELN_RM_MAPPING rm WHERE rm.BOND_ID = p.BOND_ID) AS RM_ID, ");
            // 5 個標的欄位（含 KI/KO 日期）
            for (int n = 1; n <= 5; n++) {
                sb.append("  ud.UND").append(n).append("_TICKER, ");
                sb.append("  ud.UND").append(n).append("_ENTRY_PRICE, ");
                sb.append("  ud.UND").append(n).append("_CURRENT_PRICE, ");
                sb.append("  TO_CHAR(ud.UND").append(n).append("_KI_DATE,'yyyy-MM-dd') AS UND").append(n).append("_KI_DATE, ");
                String sep = (n < 5) ? ", " : " ";
                sb.append("  TO_CHAR(ud.UND").append(n).append("_KO_DATE,'yyyy-MM-dd') AS UND").append(n).append("_KO_DATE").append(sep);
            }
            sb.append("FROM ELN_PRODUCT p ");
            sb.append("LEFT JOIN PivotUnderlyings ud ON p.BOND_ID = ud.BOND_ID ");
            sb.append("WHERE 1=1 ");

            if (vo != null && StringUtils.isNotBlank(vo.getBond_id())) {
                sb.append("AND p.BOND_ID LIKE :bondId ");
                param.put("bondId", "%" + vo.getBond_id().trim() + "%");
            }
            if (vo != null && StringUtils.isNotBlank(vo.getRm_id())) {
                sb.append("AND EXISTS (SELECT 1 FROM ELN_RM_MAPPING rm WHERE rm.BOND_ID = p.BOND_ID AND rm.RM_ID = :rmId) ");
                param.put("rmId", vo.getRm_id().trim());
            }
            if (vo != null && StringUtils.isNotBlank(vo.getTicker())) {
                sb.append("AND EXISTS (SELECT 1 FROM ELN_UNDERLYING u WHERE u.BOND_ID = p.BOND_ID AND u.TICKER = :ticker) ");
                param.put("ticker", vo.getTicker().trim().toUpperCase());
            }
            sb.append("ORDER BY p.TRADE_DATE DESC");

            List<Map<String, Object>> resultList = this.exeQueryForMap(sb.toString(), param);
            outputVO.setResultList(resultList);
            this.sendRtnObject(outputVO);

        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_QUERY" + "查詢商品失敗：" + e.getMessage());
        }
    }

    // =========================================================
    // 【查詢標的清單】供前端下拉建議
    // =========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void queryTickers(Object body, IPrimitiveMap header) throws JBranchException {
        try {
            PMS435OutputVO outputVO = new PMS435OutputVO();
            List<Map<String, Object>> rows = this.exeQueryForMap(
                "SELECT DISTINCT TICKER FROM ELN_UNDERLYING WHERE TICKER IS NOT NULL ORDER BY TICKER",
                new HashMap<>());
            List<String> tickerList = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                tickerList.add((String) row.get("TICKER"));
            }
            outputVO.setTickerList(tickerList);
            this.sendRtnObject(outputVO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JBranchException("ERR_PMS435_TICKER" + "載入標的清單失敗：" + e.getMessage());
        }
    }

    // =========================================================
    // 輔助方法
    // =========================================================

    // 安全取值：索引超出範圍時回傳空字串
    private String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return "";
        return cols[idx] == null ? "" : cols[idx].trim();
    }

    // 字串轉 Double；空值或非數字回傳 null
    private Double toDouble(String s) {
        if (StringUtils.isBlank(s)) return null;
        try { return Double.parseDouble(s.replace(",", "")); }
        catch (NumberFormatException e) { return null; }
    }

    // 日期字串轉 Oracle TO_DATE 字面值（格式 YYYY/MM/DD 或 YYYY-MM-DD）
    private String toOraDate(String raw) {
        if (StringUtils.isBlank(raw)) return "NULL";
        String s = raw.trim().replace("-", "/");
        if (s.length() >= 10) {
            return "TO_DATE('" + s.substring(0, 10) + "','YYYY/MM/DD')";
        }
        return "NULL";
    }

    // CSV 單行解析（支援雙引號包覆欄位，符合 RFC 4180）
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur   = new StringBuilder();
        boolean inQuote     = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

}
