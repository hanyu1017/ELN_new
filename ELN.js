'use strict';
eSoafApp.controller('PMS435Controller', function($rootScope, $scope, $controller, $timeout) {
    $controller('BaseController', { $scope: $scope });
    
    $scope.controllerName = "PMS435"; 

    // ================== 【初始化】 ==================
    $scope.pageSize = 10;

    $scope.argInit = function() {
        $scope.inputVO = {};
        $scope.resultList = [];
        $scope.paramList = [];
        $scope.riskAlerts = [];
        $scope.currentPage = 1;
    };

    if (!$scope.inputVO) { $scope.argInit(); }
    if (!$scope.currentPage) { $scope.currentPage = 1; }
    $scope.kiThreshold = "5";
    $scope.dbTickers = [];

    // ================== 【安全日期轉換】 ==================
    $scope.formatDateSafe = function(dateVal) {
        if (!dateVal) return "";
        if (typeof dateVal.toISOString === 'function') {
            return dateVal.toISOString().split('T')[0];
        }
        if (typeof dateVal === 'string') {
            return dateVal.substring(0, 10).replace(/\//g, '-');
        }
        return dateVal;
    };

    // ================== 【載入標的清單】 ==================
    $scope.loadTickers = function() {
        $scope.sendRecv($scope.controllerName, 'queryTickers', 'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', {},
            function(totas, isError) {
                if (!isError) {
                    var body = (totas[0].body) ? totas[0].body : totas[0];
                    $scope.dbTickers = body.tickerList || [];
                }
            }, 0, 0);
    };

    // ================== 【分析 KI 警示面板】 ==================
    $scope.analyzeRiskAlerts = function() {
        $scope.riskAlerts = [];
        var threshold = parseFloat($scope.kiThreshold);
        if (!$scope.resultList) return;

        angular.forEach($scope.resultList, function(row) {
            if (!row.KI_PRICE) return;
            var minRatio = 9999;
            var worstTicker = "";

            for (var i = 1; i <= 5; i++) {
                var entry = row['UND' + i + '_PRICE'];
                var current = row['UND' + i + '_CURRENT'];
                var ticker = row['UND' + i + '_TICKER'];

                if (entry && current && ticker) {
                    var ratio = (current / entry) * 100;
                    if (ratio < minRatio) {
                        minRatio = ratio;
                        worstTicker = ticker;
                    }
                }
            }

            if (minRatio === 9999) return;
            var koPrice = row.KO_PRICE || 120;
            var range = koPrice - row.KI_PRICE;
            var dist = minRatio - row.KI_PRICE;

            // 已觸發 AKI：一律顯示，不受 threshold 篩選
            if (row.HAS_TOUCHED_AKI === 'Y') {
                var akiPct = range > 0 ? ((minRatio - row.KI_PRICE) / range * 100) : 0;
                akiPct = Math.max(0, Math.min(98, akiPct));
                $scope.riskAlerts.push({
                    BOND_ID: row.BOND_ID,
                    PROD_TYPE: row.PROD_TYPE,
                    KI_TYPE: row.KI_TYPE,
                    KI_PRICE: row.KI_PRICE,
                    KO_PRICE: row.KO_PRICE,
                    WORST_TICKER: worstTicker,
                    DIST_TO_KI: dist,
                    CURRENT_RATIO: minRatio,
                    DIST_TO_KO: koPrice - minRatio,
                    POSITION_PCT: akiPct,
                    IS_AKI: true
                });
                return;
            }

            // 接近 KI：受 threshold 篩選
            if (dist >= 0 && dist <= threshold) {
                var nearPct = range > 0 ? ((minRatio - row.KI_PRICE) / range * 100) : 0;
                nearPct = Math.max(2, Math.min(98, nearPct));
                $scope.riskAlerts.push({
                    BOND_ID: row.BOND_ID,
                    PROD_TYPE: row.PROD_TYPE,
                    KI_TYPE: row.KI_TYPE,
                    KI_PRICE: row.KI_PRICE,
                    KO_PRICE: row.KO_PRICE,
                    WORST_TICKER: worstTicker,
                    DIST_TO_KI: dist,
                    CURRENT_RATIO: minRatio,
                    DIST_TO_KO: koPrice - minRatio,
                    POSITION_PCT: nearPct,
                    IS_AKI: false
                });
            }
        });
    };

    // ================== 【表格燈號邏輯】 ==================
    $scope.getWorstPerformance = function(row) {
        var minPerf = 9999;
        for (var i = 1; i <= 5; i++) {
            var entry = row['UND' + i + '_PRICE'];
            var current = row['UND' + i + '_CURRENT'];
            if (entry && current) {
                var perf = ((current - entry) / entry) * 100;
                if (perf < minPerf) minPerf = perf;
            }
        }
        return minPerf === 9999 ? 0 : minPerf;
    };

    $scope.getPriceStyle = function(entry, current) {
        if (!entry || !current) return {};
        if (current < entry) return { color: '#dc3545' };
        if (current > entry) return { color: '#28a745' };
        return {};
    };

    $scope.getPctChange = function(entry, current) {
        if (!entry || !current) return null;
        return ((current - entry) / entry * 100);
    };

    // ================== 【編輯按鈕】 ==================
    $scope.editRow = function(row) {
        $scope.inputVO = {
            bond_id: row.BOND_ID, rm_id: row.RM_ID, prod_type: row.PROD_TYPE, ib: row.IB, currency: row.CURRENCY, tenor: row.TENOR,
            uf_pct: row.UF_PCT, strike_price: row.STRIKE_PRICE, ko_price: row.KO_PRICE, ko_type: row.KO_TYPE, yield_pct: row.YIELD_PCT,
            ki_price: row.KI_PRICE, ki_type: row.KI_TYPE,
            und1_ticker: row.UND1_TICKER, und1_price: row.UND1_PRICE, und2_ticker: row.UND2_TICKER, und2_price: row.UND2_PRICE,
            und3_ticker: row.UND3_TICKER, und3_price: row.UND3_PRICE, und4_ticker: row.UND4_TICKER, und4_price: row.UND4_PRICE,
            und5_ticker: row.UND5_TICKER, und5_price: row.UND5_PRICE
        };

        if (row.TRADE_DATE) $scope.inputVO.trade_date = new Date(row.TRADE_DATE);
        if (row.ISSUE_DATE) $scope.inputVO.issue_date = new Date(row.ISSUE_DATE);
        if (row.FINAL_VALUATION_DATE) $scope.inputVO.final_valuation_date = new Date(row.FINAL_VALUATION_DATE);
        
        window.scrollTo(0, 0); 
    };

    // ================== 【刪除按鈕】 ==================
    $scope.deleteRow = function(bondId) {
        if (confirm("⚠️ 確定要刪除商品 " + bondId + " 嗎？這項操作無法復原！")) {
            var delVO = { bond_id: bondId };
            $scope.sendRecv($scope.controllerName, 'delete', 'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', delVO,
                function(totas, isError) {
                    if (!isError) {
                        alert("商品刪除成功！");
                        $scope.inputVO = {}; // 清空表單
                        $scope.inquireInit();
                        $scope.inquire();
                    } else {
                        var msg = (totas[0] && totas[0].head) ? totas[0].head.msgId : "未知錯誤";
                        alert("刪除失敗，錯誤代碼: " + msg);
                    }
                }, 0, 0);
        }
    };

    // ================== 【分頁邏輯】 ==================
    $scope.inquireInit = function() {
        $scope.currentPage = 1;
    };

    $scope.totalPages = function() {
        if (!$scope.resultList || $scope.resultList.length === 0) return 0;
        return Math.ceil($scope.resultList.length / $scope.pageSize);
    };

    $scope.updateParamList = function() {
        if (!$scope.resultList) { $scope.paramList = []; return; }
        var start = ($scope.currentPage - 1) * $scope.pageSize;
        $scope.paramList = $scope.resultList.slice(start, start + $scope.pageSize);
    };

    $scope.goToPage = function(page) {
        var total = $scope.totalPages();
        if (page < 1 || (total > 0 && page > total)) return;
        $scope.currentPage = page;
        $scope.updateParamList();
    };

    // ================== 【查詢動作】 ==================
    $scope.inquire = function() {
        var queryVO = angular.copy($scope.inputVO);

        queryVO.trade_date = $scope.formatDateSafe(queryVO.trade_date);
        queryVO.issue_date = $scope.formatDateSafe(queryVO.issue_date);

        $scope.sendRecv($scope.controllerName, 'query', 'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', queryVO,
            function(totas, isError) {
                if (!isError) {
                    var body = (totas[0].body) ? totas[0].body : totas[0];
                    $scope.resultList = body.resultList || [];
                    $scope.updateParamList();
                    $scope.analyzeRiskAlerts();
                } else {
                    var msg = (totas[0] && totas[0].head) ? totas[0].head.msgId : "未知錯誤";
                    alert("查詢失敗，錯誤代碼: " + msg);
                }
            }, 0, 0);
    };

    // ================== 【儲存動作】 ==================
    $scope.saveData = function() {
        var saveVO = angular.copy($scope.inputVO);

        saveVO.trade_date = $scope.formatDateSafe(saveVO.trade_date);
        saveVO.issue_date = $scope.formatDateSafe(saveVO.issue_date);
        saveVO.final_valuation_date = $scope.formatDateSafe(saveVO.final_valuation_date);

        $scope.sendRecv($scope.controllerName, 'save', 'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', saveVO,
            function(totas, isError) {
                if (!isError) {
                    alert("商品儲存/更新成功！");
                    $scope.argInit();   // 清空表單、清空清單、重置頁碼
                    $scope.inquire();   // 以全空條件重新查詢
                } else {
                    var msg = (totas[0] && totas[0].head) ? totas[0].head.msgId : "未知錯誤";
                    alert("儲存失敗，錯誤代碼: " + msg);
                }
            }, 0, 0);
    };

    // ================== 【匯入 Excel】 ==================
    // SheetJS 動態載入（避免 CSP 阻擋）：
    //   請將 xlsx.full.min.js 下載後放到 assets/scripts/xlsx.full.min.js
    //   下載網址：https://cdn.sheetjs.com (或請 IT 提供)
    var XLSX_LOCAL_PATH = 'assets/scripts/xlsx.full.min.js';

    function loadXlsxThen(callback) {
        if (typeof XLSX !== 'undefined') { callback(); return; }
        var s = document.createElement('script');
        s.src = XLSX_LOCAL_PATH;
        s.onload  = function() { callback(); };
        s.onerror = function() {
            alert('無法載入 Excel 解析函式庫。\n請確認 ' + XLSX_LOCAL_PATH + ' 檔案存在。\n（可向 IT 取得 xlsx.full.min.js 並放置於對應目錄）');
        };
        document.head.appendChild(s);
    }

    $scope.triggerImport = function() {
        loadXlsxThen(function() {
            var fileInput = document.getElementById('excelFileInput');
            if (fileInput) { fileInput.value = ''; fileInput.click(); }
        });
    };

    $timeout(function() {
        var fileInput = document.getElementById('excelFileInput');
        if (fileInput) {
            fileInput.addEventListener('change', function(event) {
                var file = event.target.files[0];
                if (!file) return;

                if (typeof XLSX === 'undefined') {
                    alert('錯誤：Excel 解析函式庫 (SheetJS) 未載入，請先確認 ' + XLSX_LOCAL_PATH + ' 存在。');
                    return;
                }

                var reader = new FileReader();
                reader.onload = function(e) {
                    try {
                        var data = new Uint8Array(e.target.result);
                        var wb = XLSX.read(data, { type: 'array', cellDates: true });
                        var ws = wb.Sheets[wb.SheetNames[0]];
                        var rows = XLSX.utils.sheet_to_json(ws, { raw: false, dateNF: 'yyyy-MM-dd' });

                        event.target.value = '';

                        if (!rows || rows.length === 0) {
                            $timeout(function() { alert('Excel 無資料可匯入（請確認第一列為欄位標題）。'); });
                            return;
                        }

                        $timeout(function() {
                            if (!confirm('讀取到 ' + rows.length + ' 筆資料，確定要批次匯入？')) return;

                            var HEADER_MAP = {
                                '債券代號': 'bond_id',       '交易日': 'trade_date',
                                'UF%': 'uf_pct',             '理專': 'rm_id',            '理專ID': 'rm_id',
                                '商品類型': 'prod_type',     '天期(月)': 'tenor',         '天期': 'tenor',
                                '發行日': 'issue_date',      '幣別': 'currency',          'IB': 'ib',
                                '標的1': 'und1_ticker',      '進場1': 'und1_price',
                                '標的2': 'und2_ticker',      '進場2': 'und2_price',
                                '標的3': 'und3_ticker',      '進場3': 'und3_price',
                                '標的4': 'und4_ticker',      '進場4': 'und4_price',
                                '標的5': 'und5_ticker',      '進場5': 'und5_price',
                                '執行價格(%)': 'strike_price', '執行價格%': 'strike_price', '執行價格': 'strike_price',
                                'KO價格(%)': 'ko_price',     'KO%': 'ko_price',
                                'KO類型': 'ko_type',
                                '收益率(%)': 'yield_pct',    '收益率%': 'yield_pct',      '收益率': 'yield_pct',
                                'KI價格(%)': 'ki_price',     'KI%': 'ki_price',
                                'KI類型': 'ki_type',
                                '最終評價日': 'final_valuation_date'
                            };

                            var NUM_FIELDS = ['uf_pct','strike_price','ko_price','yield_pct','ki_price',
                                              'und1_price','und2_price','und3_price','und4_price','und5_price'];
                            var DATE_FIELDS = ['trade_date','issue_date','final_valuation_date'];

                            function mapRowToVO(rawRow) {
                                var vo = {};
                                angular.forEach(rawRow, function(value, key) {
                                    var field = HEADER_MAP[key.trim()];
                                    if (!field || value === null || value === undefined || value === '') return;
                                    var s = String(value).trim();
                                    if (NUM_FIELDS.indexOf(field) >= 0) {
                                        vo[field] = parseFloat(s) || null;
                                    } else if (DATE_FIELDS.indexOf(field) >= 0) {
                                        // 統一轉為 yyyy-MM-dd (支援 / 或 - 分隔，或8碼數字)
                                        s = s.replace(/\//g, '-');
                                        if (/^\d{8}$/.test(s)) {
                                            s = s.substring(0,4) + '-' + s.substring(4,6) + '-' + s.substring(6,8);
                                        }
                                        vo[field] = s.substring(0, 10);
                                    } else {
                                        vo[field] = s;
                                    }
                                });
                                return vo;
                            }

                            var successCount = 0, failCount = 0, errors = [];

                            function processRow(index) {
                                if (index >= rows.length) {
                                    var msg = '批次匯入完成！\n成功：' + successCount + ' 筆\n失敗：' + failCount + ' 筆';
                                    if (errors.length > 0) {
                                        msg += '\n\n失敗明細（最多顯示 10 筆）：\n' + errors.slice(0, 10).join('\n');
                                    }
                                    alert(msg);
                                    $scope.inquireInit();
                                    $scope.inquire();
                                    return;
                                }

                                var saveVO = mapRowToVO(rows[index]);
                                if (!saveVO.bond_id) {
                                    failCount++;
                                    errors.push('第 ' + (index + 2) + ' 列：債券代號為空，略過');
                                    processRow(index + 1);
                                    return;
                                }

                                $scope.sendRecv($scope.controllerName, 'save',
                                    'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', saveVO,
                                    function(totas, isError) {
                                        if (!isError) {
                                            successCount++;
                                        } else {
                                            failCount++;
                                            var errId = (totas[0] && totas[0].head) ? totas[0].head.msgId : '儲存失敗';
                                            errors.push('第 ' + (index + 2) + ' 列 (' + saveVO.bond_id + ')：' + errId);
                                        }
                                        processRow(index + 1);
                                    }, 0, 0);
                            }

                            processRow(0);
                        });

                    } catch (err) {
                        $timeout(function() { alert('Excel 解析發生錯誤：' + err.message); });
                    }
                };
                reader.readAsArrayBuffer(file);
            });
        }
    }, 0);

    $scope.loadTickers();
});
