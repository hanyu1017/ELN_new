import yfinance as yf
import pandas as pd
import csv
from datetime import datetime
import logging
import os
import urllib3
from curl_cffi import requests as cffi_requests

# ================= 配置設定 =================
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# 1. 監控的標的代號清單
TICKERS_TO_MONITOR = [
    "AAPL", "AVGO", "GOOG", "INTC", "META", 
    "MSFT", "MU", "NFLX", "NVDA", "PLTR", 
    "TSLA", "TSM"
]

# 🌟 2. 在這裡設定你要抓取的日期區間 (格式: YYYY-MM-DD)
START_DATE = "2025-10-01"  
END_DATE   = "2026-04-10"  # 注意：yfinance 的 end_date 是不包含當天的 (Exclusive)，若要包含 4/9，請設為 4/10

# 設定產出的 CSV 檔名 (包含區間以便識別)
CSV_FILENAME = f"eln_historical_{START_DATE}_to_{END_DATE}.csv"
# ============================================

def fetch_historical_prices():
    if not TICKERS_TO_MONITOR:
        logging.warning("標的清單為空。")
        return

    # 忽略 SSL 警告
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    # 建立偽裝 Session，繞過防火牆與 Yahoo 防爬蟲機制
    custom_session = cffi_requests.Session(impersonate="chrome", verify=False)

    logging.info(f"準備批次抓取 {len(TICKERS_TO_MONITOR)} 檔標的歷史價格...")
    logging.info(f"設定區間：{START_DATE} 至 {END_DATE}")

    try:
        # 🚀 透過 start 與 end 參數抓取區間資料
        data = yf.download(
            TICKERS_TO_MONITOR, 
            start=START_DATE, 
            end=END_DATE, 
            session=custom_session, 
            progress=False
        )

        if data.empty:
            logging.error("抓取失敗，完全沒有資料回傳 (請確認日期區間是否為未來，或是否為假日)。")
            return

        scraped_data = []
        
        # yf.download 多檔標的時，回傳的 DataFrame 會有 MultiIndex 欄位結構
        # 我們只取 'Close' (收盤價) 這個層級
        close_data = data['Close'] if 'Close' in data else data
        
        # 解析每一檔股票的每一天資料
        for ticker in TICKERS_TO_MONITOR:
            try:
                # 處理單一檔或多檔股票回傳結構不同的問題
                if isinstance(close_data, pd.DataFrame):
                    if ticker not in close_data.columns:
                        logging.warning(f"⚠️ 找不到 {ticker} 的資料。")
                        continue
                    clean_series = close_data[ticker].dropna()
                else:
                    clean_series = close_data.dropna()

                if not clean_series.empty:
                    # 🌟 迴圈取出該標的「每一天」的交易日期與價格
                    for date_obj, price in clean_series.items():
                        date_str = date_obj.strftime('%Y-%m-%d')
                        scraped_data.append({
                            'Ticker': ticker,
                            'Date': date_str,
                            'Close_Price': round(float(price), 4)
                        })
                    logging.info(f"✅ 成功獲取 {ticker} : 共 {len(clean_series)} 筆歷史交易日資料。")
                else:
                    logging.warning(f"⚠️ {ticker} 在此區間內沒有有效報價。")
            except Exception as e:
                logging.error(f"❌ 解析 {ticker} 時發生錯誤: {str(e)}")

        # 將收集到的資料排序 (依據 Ticker, 然後 Date)
        scraped_data = sorted(scraped_data, key=lambda x: (x['Ticker'], x['Date']))

        if scraped_data:
            write_to_csv(scraped_data)
        else:
            logging.warning("沒有成功解析到任何資料，不產生 CSV 檔案。")

    except Exception as e:
        logging.error(f"批次抓取發生嚴重錯誤: {str(e)}")


def write_to_csv(data_list):
    try:
        with open(CSV_FILENAME, mode='w', newline='', encoding='utf-8-sig') as csv_file:
            fieldnames = ['Ticker', 'Date', 'Close_Price']
            writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
            writer.writeheader()
            for data in data_list:
                writer.writerow(data)
                
        logging.info(f"🎉 歷史報價抓取完成！共 {len(data_list)} 筆資料，已儲存至 [{os.path.abspath(CSV_FILENAME)}]")
        
    except Exception as e:
        logging.error(f"寫入 CSV 檔案時發生錯誤: {str(e)}")


if __name__ == "__main__":
    fetch_historical_prices()