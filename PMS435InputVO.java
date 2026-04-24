package com.systex.jbranch.app.server.fps.pms435;

import com.systex.jbranch.platform.common.dataaccess.vo.PagingInputVO;

public class PMS435InputVO extends PagingInputVO {
    private static final long serialVersionUID = 1L;

    // ── 查詢篩選欄位 ──────────────────────────────
    private String bond_id;
    private String rm_id;
    private String ticker;           // 新增：依標的代號篩選

    // ── 商品主檔欄位 ──────────────────────────────
    private String trade_date;
    private String prod_type;
    private String ib;
    private String currency;
    private String tenor;
    private String issue_date;
    private String final_valuation_date;
    private String maturity_date;    // 新增：到期日

    // ── 數值欄位 ──────────────────────────────────
    private Double uf_pct;
    private Double strike_price;
    private Double ko_price;
    private String ko_type;
    private Double yield_pct;
    private Double ki_price;
    private String ki_type;

    // ── 標的明細 (最多 5 筆) ─────────────────────
    private String und1_ticker;
    private Double und1_price;
    private String und2_ticker;
    private Double und2_price;
    private String und3_ticker;
    private Double und3_price;
    private String und4_ticker;
    private Double und4_price;
    private String und5_ticker;
    private Double und5_price;

    // ── CSV 批次匯入 ──────────────────────────────
    private String csv_content;      // 前端以 BIG5 解碼後的 CSV 文字內容

    // ── Getters / Setters ─────────────────────────
    public String getBond_id()                             { return bond_id; }
    public void   setBond_id(String bond_id)               { this.bond_id = bond_id; }
    public String getRm_id()                               { return rm_id; }
    public void   setRm_id(String rm_id)                   { this.rm_id = rm_id; }
    public String getTicker()                              { return ticker; }
    public void   setTicker(String ticker)                 { this.ticker = ticker; }
    public String getTrade_date()                          { return trade_date; }
    public void   setTrade_date(String trade_date)         { this.trade_date = trade_date; }
    public String getProd_type()                           { return prod_type; }
    public void   setProd_type(String prod_type)           { this.prod_type = prod_type; }
    public String getIb()                                  { return ib; }
    public void   setIb(String ib)                         { this.ib = ib; }
    public String getCurrency()                            { return currency; }
    public void   setCurrency(String currency)             { this.currency = currency; }
    public String getTenor()                               { return tenor; }
    public void   setTenor(String tenor)                   { this.tenor = tenor; }
    public String getIssue_date()                          { return issue_date; }
    public void   setIssue_date(String issue_date)         { this.issue_date = issue_date; }
    public String getFinal_valuation_date()                { return final_valuation_date; }
    public void   setFinal_valuation_date(String v)        { this.final_valuation_date = v; }
    public String getMaturity_date()                       { return maturity_date; }
    public void   setMaturity_date(String maturity_date)   { this.maturity_date = maturity_date; }
    public Double getUf_pct()                              { return uf_pct; }
    public void   setUf_pct(Double uf_pct)                 { this.uf_pct = uf_pct; }
    public Double getStrike_price()                        { return strike_price; }
    public void   setStrike_price(Double strike_price)     { this.strike_price = strike_price; }
    public Double getKo_price()                            { return ko_price; }
    public void   setKo_price(Double ko_price)             { this.ko_price = ko_price; }
    public String getKo_type()                             { return ko_type; }
    public void   setKo_type(String ko_type)               { this.ko_type = ko_type; }
    public Double getYield_pct()                           { return yield_pct; }
    public void   setYield_pct(Double yield_pct)           { this.yield_pct = yield_pct; }
    public Double getKi_price()                            { return ki_price; }
    public void   setKi_price(Double ki_price)             { this.ki_price = ki_price; }
    public String getKi_type()                             { return ki_type; }
    public void   setKi_type(String ki_type)               { this.ki_type = ki_type; }
    public String getUnd1_ticker()                         { return und1_ticker; }
    public void   setUnd1_ticker(String v)                 { this.und1_ticker = v; }
    public Double getUnd1_price()                          { return und1_price; }
    public void   setUnd1_price(Double v)                  { this.und1_price = v; }
    public String getUnd2_ticker()                         { return und2_ticker; }
    public void   setUnd2_ticker(String v)                 { this.und2_ticker = v; }
    public Double getUnd2_price()                          { return und2_price; }
    public void   setUnd2_price(Double v)                  { this.und2_price = v; }
    public String getUnd3_ticker()                         { return und3_ticker; }
    public void   setUnd3_ticker(String v)                 { this.und3_ticker = v; }
    public Double getUnd3_price()                          { return und3_price; }
    public void   setUnd3_price(Double v)                  { this.und3_price = v; }
    public String getUnd4_ticker()                         { return und4_ticker; }
    public void   setUnd4_ticker(String v)                 { this.und4_ticker = v; }
    public Double getUnd4_price()                          { return und4_price; }
    public void   setUnd4_price(Double v)                  { this.und4_price = v; }
    public String getUnd5_ticker()                         { return und5_ticker; }
    public void   setUnd5_ticker(String v)                 { this.und5_ticker = v; }
    public Double getUnd5_price()                          { return und5_price; }
    public void   setUnd5_price(Double v)                  { this.und5_price = v; }
    public String getCsv_content()                         { return csv_content; }
    public void   setCsv_content(String csv_content)       { this.csv_content = csv_content; }
}
