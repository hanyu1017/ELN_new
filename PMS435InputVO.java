package com.systex.jbranch.app.server.fps.pms435;

import com.systex.jbranch.platform.common.dataaccess.vo.PagingInputVO;

public class PMS435InputVO extends PagingInputVO {
    private static final long serialVersionUID = 1L;

    private String bond_id;
    private String rm_id;
    private String trade_date;
    private String prod_type;
    private String ib;
    private String currency;
    private String tenor;
    private String issue_date;
    private Double uf_pct;
    private Double strike_price;
    private Double ko_price;
    private String ko_type;
    private Double yield_pct;
    private Double ki_price;
    private String ki_type;
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
    private String final_valuation_date;

    public String getBond_id()       { return bond_id; }
    public void   setBond_id(String bond_id) { this.bond_id = bond_id; }
    public String getRm_id()         { return rm_id; }
    public void   setRm_id(String rm_id)     { this.rm_id = rm_id; }
    public String getTrade_date()    { return trade_date; }
    public void   setTrade_date(String trade_date) { this.trade_date = trade_date; }
    public String getProd_type()     { return prod_type; }
    public void   setProd_type(String prod_type) { this.prod_type = prod_type; }
    public String getIb()            { return ib; }
    public void   setIb(String ib)            { this.ib = ib; }
    public String getCurrency()      { return currency; }
    public void   setCurrency(String currency) { this.currency = currency; }
    public String getTenor()         { return tenor; }
    public void   setTenor(String tenor)     { this.tenor = tenor; }
    public String getIssue_date()    { return issue_date; }
    public void   setIssue_date(String issue_date) { this.issue_date = issue_date; }
    public Double getUf_pct()        { return uf_pct; }
    public void   setUf_pct(Double uf_pct)   { this.uf_pct = uf_pct; }
    public Double getStrike_price()  { return strike_price; }
    public void   setStrike_price(Double strike_price) { this.strike_price = strike_price; }
    public Double getKo_price()      { return ko_price; }
    public void   setKo_price(Double ko_price) { this.ko_price = ko_price; }
    public String getKo_type()       { return ko_type; }
    public void   setKo_type(String ko_type) { this.ko_type = ko_type; }
    public Double getYield_pct()     { return yield_pct; }
    public void   setYield_pct(Double yield_pct) { this.yield_pct = yield_pct; }
    public Double getKi_price()      { return ki_price; }
    public void   setKi_price(Double ki_price) { this.ki_price = ki_price; }
    public String getKi_type()       { return ki_type; }
    public void   setKi_type(String ki_type) { this.ki_type = ki_type; }
    public String getUnd1_ticker()   { return und1_ticker; }
    public void   setUnd1_ticker(String und1_ticker) { this.und1_ticker = und1_ticker; }
    public Double getUnd1_price()    { return und1_price; }
    public void   setUnd1_price(Double und1_price) { this.und1_price = und1_price; }
    public String getUnd2_ticker()   { return und2_ticker; }
    public void   setUnd2_ticker(String und2_ticker) { this.und2_ticker = und2_ticker; }
    public Double getUnd2_price()    { return und2_price; }
    public void   setUnd2_price(Double und2_price) { this.und2_price = und2_price; }
    public String getUnd3_ticker()   { return und3_ticker; }
    public void   setUnd3_ticker(String und3_ticker) { this.und3_ticker = und3_ticker; }
    public Double getUnd3_price()    { return und3_price; }
    public void   setUnd3_price(Double und3_price) { this.und3_price = und3_price; }
    public String getUnd4_ticker()   { return und4_ticker; }
    public void   setUnd4_ticker(String und4_ticker) { this.und4_ticker = und4_ticker; }
    public Double getUnd4_price()    { return und4_price; }
    public void   setUnd4_price(Double und4_price) { this.und4_price = und4_price; }
    public String getUnd5_ticker()   { return und5_ticker; }
    public void   setUnd5_ticker(String und5_ticker) { this.und5_ticker = und5_ticker; }
    public Double getUnd5_price()    { return und5_price; }
    public void   setUnd5_price(Double und5_price) { this.und5_price = und5_price; }
    public String getFinal_valuation_date()              { return final_valuation_date; }
    public void   setFinal_valuation_date(String final_valuation_date) { this.final_valuation_date = final_valuation_date; }
}
