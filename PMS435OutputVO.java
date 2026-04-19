package com.systex.jbranch.app.server.fps.pms435;

import com.systex.jbranch.platform.common.dataaccess.vo.BaseVO;
import java.util.List;
import java.util.Map;

public class PMS435OutputVO extends BaseVO {
    private static final long serialVersionUID = 1L;

    private List<Map<String, Object>> resultList;
    private List<String> tickerList;

    public List<Map<String, Object>> getResultList() { return resultList; }
    public void setResultList(List<Map<String, Object>> resultList) { this.resultList = resultList; }
    public List<String> getTickerList() { return tickerList; }
    public void setTickerList(List<String> tickerList) { this.tickerList = tickerList; }
}
