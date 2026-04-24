package com.systex.jbranch.app.server.fps.pms435;

import com.systex.jbranch.app.server.fps.sot701.basicVO;
import java.util.List;
import java.util.Map;

public class PMS435OutputVO extends basicVO {
    private static final long serialVersionUID = 1L;

    private List<Map<String, Object>> resultList;
    private List<String> tickerList;

    // CSV 批次匯入結果
    private int importSuccessCount;
    private int importFailCount;
    private List<String> importErrors;

    public List<Map<String, Object>> getResultList()              { return resultList; }
    public void setResultList(List<Map<String, Object>> v)        { this.resultList = v; }
    public List<String> getTickerList()                           { return tickerList; }
    public void setTickerList(List<String> v)                     { this.tickerList = v; }
    public int getImportSuccessCount()                            { return importSuccessCount; }
    public void setImportSuccessCount(int v)                      { this.importSuccessCount = v; }
    public int getImportFailCount()                               { return importFailCount; }
    public void setImportFailCount(int v)                         { this.importFailCount = v; }
    public List<String> getImportErrors()                         { return importErrors; }
    public void setImportErrors(List<String> v)                   { this.importErrors = v; }
}
