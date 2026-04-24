'use strict';
eSoafApp.controller('PMS435Controller', function($rootScope, $scope, $controller, $timeout, $sce, $http) {
    $controller('BaseController', { $scope: $scope });

    $scope.controllerName = "PMS435";
    $scope.pageSize  = 10;
    $scope.dbTickers = [];
    $scope.queried   = false;

    // ================== 【初始化】 ==================
    $scope.argInit = function() {
        $scope.inputVO   = {};
        $scope.resultList = [];
        $scope.paramList  = [];
        $scope.currentPage = 1;
        $scope.queried    = false;
    };

    if (!$scope.inputVO)    { $scope.argInit(); }
    if (!$scope.currentPage) { $scope.currentPage = 1; }

    // ================== 【日期格式化】 ==================
    $scope.formatDateSafe = function(dateVal) {
        if (!dateVal) return "";
        if (typeof dateVal.toISOString === 'function') {
            return dateVal.toISOString().split('T')[0];
        }
        return String(dateVal).substring(0, 10).replace(/\//g, '-');
    };

    // ================== 【載入標的清單（下拉建議）】 ==================
    $scope.loadTickers = function() {
        $scope.sendRecv($scope.controllerName, 'queryTickers',
            'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', {},
            function(totas, isError) {
                if (!isError) {
                    var body = totas[0].body || totas[0];
                    $scope.dbTickers = body.tickerList || [];
                }
            }, 0, 0);
    };

    // ================== 【燈號邏輯】KI 優先於 KO ==================
    // 回傳 'KI' / 'KO' / 'NONE'
    $scope.getUndStatus = function(row, n) {
        if (row['UND' + n + '_KI_DATE']) return 'KI';
        if (row['UND' + n + '_KO_DATE']) return 'KO';
        return 'NONE';
    };

    // 回傳對應的燈號 HTML（需搭配 ng-bind-html + $sce.trustAsHtml）
    $scope.getUndLight = function(row, n) {
        var status = $scope.getUndStatus(row, n);
        if (!row['UND' + n + '_TICKER']) return $sce.trustAsHtml('');
        if (status === 'KI') return $sce.trustAsHtml('<span style="color:#dc3545; font-size:16px;" title="KI 已觸發">&#9679;</span> ');
        if (status === 'KO') return $sce.trustAsHtml('<span style="color:#28a745; font-size:16px;" title="KO 已觸發">&#9679;</span> ');
        return $sce.trustAsHtml('<span style="color:#adb5bd; font-size:16px;" title="觀察中">&#9679;</span> ');
    };

    // 回傳觸發日期字串（KI 優先）
    $scope.getUndDate = function(row, n) {
        if (row['UND' + n + '_KI_DATE']) return row['UND' + n + '_KI_DATE'];
        if (row['UND' + n + '_KO_DATE']) return row['UND' + n + '_KO_DATE'];
        return null;
    };

    // ================== 【現價樣式與漲跌幅】 ==================
    $scope.getPriceStyle = function(entry, current) {
        if (!entry || !current) return {};
        if (current < entry) return { color: '#dc3545' };
        if (current > entry) return { color: '#28a745' };
        return {};
    };

    $scope.getPctChange = function(entry, current) {
        if (!entry || !current) return null;
        return (current - entry) / entry * 100;
    };

    // ================== 【刪除】 ==================
    $scope.deleteRow = function(bondId) {
        if (!confirm('確定要刪除商品 ' + bondId + ' ？此操作無法復原。')) return;
        $scope.sendRecv($scope.controllerName, 'delete',
            'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO',
            { bond_id: bondId },
            function(totas, isError) {
                if (!isError) {
                    alert('商品 ' + bondId + ' 已刪除。');
                    $scope.inquireInit();
                    $scope.inquire();
                } else {
                    var msg = (totas[0] && totas[0].head) ? totas[0].head.msgId : '未知錯誤';
                    alert('刪除失敗：' + msg);
                }
            }, 0, 0);
    };

    // ================== 【分頁】 ==================
    $scope.inquireInit = function() { $scope.currentPage = 1; };

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

    // ================== 【查詢】 ==================
    $scope.inquire = function() {
        var queryVO = {
            bond_id: $scope.inputVO.bond_id || '',
            rm_id:   $scope.inputVO.rm_id   || '',
            ticker:  $scope.inputVO.ticker  || ''
        };
        $scope.sendRecv($scope.controllerName, 'query',
            'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO', queryVO,
            function(totas, isError) {
                $scope.queried = true;
                if (!isError) {
                    var body = totas[0].body || totas[0];
                    $scope.resultList = body.resultList || [];
                    $scope.updateParamList();
                } else {
                    var msg = (totas[0] && totas[0].head) ? totas[0].head.msgId : '未知錯誤';
                    alert('查詢失敗：' + msg);
                }
            }, 0, 0);
    };

    // ================== 【CSV 批次匯入】 ==================
    // 流程：
    //   1. 使用者選取 .csv 檔（BIG5 編碼，台灣 Excel 預設格式）
    //   2. 透過 FormData + $http.post 上傳至伺服器暫存路徑（TEMP_PATH）
    //   3. 伺服器回傳已儲存的 fileName
    //   4. 呼叫 importCSV，後端以 CSVUtil.getBig5CSVFile 讀取並轉碼

    $scope.triggerCsvImport = function() {
        var fileInput = document.getElementById('csvFileInput');
        if (fileInput) { fileInput.value = ''; fileInput.click(); }
    };

    $timeout(function() {
        var fileInput = document.getElementById('csvFileInput');
        if (!fileInput) return;

        fileInput.addEventListener('change', function(event) {
            var file = event.target.files[0];
            if (!file) return;
            event.target.value = '';

            if (!confirm('即將上傳並匯入：' + file.name + '\n確定繼續？')) return;

            // 第一步：上傳檔案至伺服器 TEMP_PATH
            var formData = new FormData();
            formData.append('file', file);

            $http.post('uploadFile.action', formData, {
                headers: { 'Content-Type': undefined },
                transformRequest: angular.identity
            }).then(function(resp) {
                var serverFileName = resp.data && resp.data.fileName ? resp.data.fileName : file.name;

                // 第二步：通知後端以 CSVUtil 讀取並匯入
                $scope.sendRecv($scope.controllerName, 'importCSV',
                    'com.systex.jbranch.app.server.fps.pms435.PMS435InputVO',
                    { fileName: serverFileName },
                    function(totas, isError) {
                        if (!isError) {
                            var body   = totas[0].body || totas[0];
                            var msg    = '批次匯入完成！\n成功：' + (body.importSuccessCount || 0) +
                                         ' 筆\n失敗：'  + (body.importFailCount    || 0) + ' 筆';
                            var errs   = body.importErrors || [];
                            if (errs.length > 0) {
                                msg += '\n\n失敗明細（最多顯示 10 筆）：\n' + errs.slice(0, 10).join('\n');
                            }
                            $timeout(function() {
                                alert(msg);
                                $scope.inquireInit();
                                $scope.inquire();
                            });
                        } else {
                            var errMsg = (totas[0] && totas[0].head) ? totas[0].head.msgId : '未知錯誤';
                            $timeout(function() { alert('匯入失敗：' + errMsg); });
                        }
                    }, 0, 0);

            }).catch(function(err) {
                $timeout(function() {
                    alert('檔案上傳失敗，請確認網路連線或洽 IT。\n錯誤：' + (err.statusText || err.status));
                });
            });
        });
    }, 0);

    // 初始化載入標的清單
    $scope.loadTickers();
});
