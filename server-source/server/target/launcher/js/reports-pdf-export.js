// Headwind MDM Reports PDF Export Functionality
// Add this to the Summary/Reports Controller

// Function to export reports as PDF
function exportReportToPDF(reportType, reportData) {
    // Using jsPDF library (must be included in index.html)
    // <script src="https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"></script>
    // <script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js"></script>

    if (typeof html2pdf === 'undefined') {
        console.error('html2pdf library not loaded');
        alert('PDF export library not available. Please refresh the page.');
        return;
    }

    var element = document.getElementById('report-content-' + reportType);
    if (!element) {
        console.error('Report element not found: report-content-' + reportType);
        return;
    }

    var opt = {
        margin: 10,
        filename: 'hwmdm-report-' + reportType + '-' + new Date().toISOString().split('T')[0] + '.pdf',
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: { scale: 2 },
        jsPDF: { orientation: 'portrait', unit: 'mm', format: 'a4' }
    };

    html2pdf().set(opt).from(element).save();
}

// Alternative using jsPDF + AutoTable for better table formatting
function exportReportToPDFAdvanced(reportType, reportTitle, reportData) {
    if (typeof jsPDF === 'undefined') {
        console.error('jsPDF library not loaded');
        return;
    }

    var doc = new jsPDF('p', 'mm', 'a4');
    var pageWidth = doc.internal.pageSize.getWidth();
    var pageHeight = doc.internal.pageSize.getHeight();
    var margin = 15;
    var yPosition = margin;

    // Header with logo
    doc.setFillColor(14, 165, 183);
    doc.rect(0, 0, pageWidth, 25, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(18);
    doc.text('Headwind MDM - Admin Report', margin, yPosition + 15);

    yPosition += 30;

    // Title
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(14);
    doc.text(reportTitle, margin, yPosition);
    yPosition += 10;

    // Date
    doc.setFontSize(10);
    doc.setTextColor(100, 100, 100);
    doc.text('Generated: ' + new Date().toLocaleString(), margin, yPosition);
    yPosition += 10;

    // Content based on report type
    if (reportType === 'telemetry') {
        exportTelemetryReport(doc, reportData, margin, yPosition, pageWidth, pageHeight);
    } else if (reportType === 'devices') {
        exportDevicesReport(doc, reportData, margin, yPosition, pageWidth, pageHeight);
    } else if (reportType === 'summary') {
        exportSummaryReport(doc, reportData, margin, yPosition, pageWidth, pageHeight);
    }

    // Footer
    doc.setFontSize(8);
    doc.setTextColor(150, 150, 150);
    doc.text('Confidential - Headwind MDM System', margin, pageHeight - 10);

    // Save
    doc.save('hwmdm-' + reportType + '-' + new Date().getTime() + '.pdf');
}

function exportTelemetryReport(doc, data, margin, yPosition, pageWidth, pageHeight) {
    var maxY = pageHeight - 20;
    var columnWidth = (pageWidth - 2 * margin) / 3;

    // Status Distribution
    doc.setFontSize(11);
    doc.setTextColor(14, 165, 183);
    doc.text('Device Status Distribution', margin, yPosition);
    yPosition += 8;

    doc.setFontSize(9);
    doc.setTextColor(0, 0, 0);

    var statusData = [
        ['Status', 'Count', 'Percentage'],
        ['Online', data.onlineCount || 0, ((data.onlineCount / (data.totalDevices || 1)) * 100).toFixed(1) + '%'],
        ['Offline', data.offlineCount || 0, ((data.offlineCount / (data.totalDevices || 1)) * 100).toFixed(1) + '%'],
        ['Idle', data.idleCount || 0, ((data.idleCount / (data.totalDevices || 1)) * 100).toFixed(1) + '%']
    ];

    doc.autoTable({
        startY: yPosition,
        head: [statusData[0]],
        body: statusData.slice(1),
        margin: { left: margin, right: margin },
        columnStyles: { 0: { cellWidth: columnWidth }, 1: { cellWidth: columnWidth }, 2: { cellWidth: columnWidth } }
    });

    yPosition = doc.lastAutoTable.finalY + 10;

    // Android Versions
    if (yPosition + 20 < maxY) {
        doc.setFontSize(11);
        doc.setTextColor(14, 165, 183);
        doc.text('Android Version Distribution', margin, yPosition);
        yPosition += 8;

        var versionData = [['Version', 'Count', 'Percentage']];
        if (data.androidVersions) {
            Object.keys(data.androidVersions).forEach(function(version) {
                var count = data.androidVersions[version];
                versionData.push([
                    version,
                    count,
                    ((count / (data.totalDevices || 1)) * 100).toFixed(1) + '%'
                ]);
            });
        }

        doc.autoTable({
            startY: yPosition,
            head: [versionData[0]],
            body: versionData.slice(1),
            margin: { left: margin, right: margin }
        });
    }

    yPosition = doc.lastAutoTable.finalY + 10;

    // Battery Health
    if (yPosition + 20 < maxY) {
        doc.setFontSize(11);
        doc.setTextColor(14, 165, 183);
        doc.text('Battery Health Distribution', margin, yPosition);
        yPosition += 8;

        var batteryData = [
            ['Health Level', 'Count', 'Percentage'],
            ['Good (>80%)', data.batteryGood || 0, ((data.batteryGood / (data.totalDevices || 1)) * 100).toFixed(1) + '%'],
            ['Medium (60-80%)', data.batteryMedium || 0, ((data.batteryMedium / (data.totalDevices || 1)) * 100).toFixed(1) + '%'],
            ['Low (<60%)', data.batteryLow || 0, ((data.batteryLow / (data.totalDevices || 1)) * 100).toFixed(1) + '%']
        ];

        doc.autoTable({
            startY: yPosition,
            head: [batteryData[0]],
            body: batteryData.slice(1),
            margin: { left: margin, right: margin }
        });
    }
}

function exportDevicesReport(doc, data, margin, yPosition, pageWidth, pageHeight) {
    var maxY = pageHeight - 20;

    doc.setFontSize(11);
    doc.setTextColor(14, 165, 183);
    doc.text('Enrolled Devices', margin, yPosition);
    yPosition += 8;

    var deviceTableData = [['Device Name', 'Model', 'Serial', 'Status', 'Last Update']];

    if (data.devices && data.devices.length > 0) {
        data.devices.forEach(function(device) {
            deviceTableData.push([
                device.name || 'N/A',
                device.model || 'N/A',
                device.serial || 'N/A',
                device.status || 'Unknown',
                device.lastUpdate || 'Never'
            ]);
        });
    }

    doc.autoTable({
        startY: yPosition,
        head: [deviceTableData[0]],
        body: deviceTableData.slice(1),
        margin: { left: margin, right: margin },
        columnStyles: {
            0: { cellWidth: 35 },
            1: { cellWidth: 30 },
            2: { cellWidth: 30 },
            3: { cellWidth: 25 },
            4: { cellWidth: 35 }
        }
    });
}

function exportSummaryReport(doc, data, margin, yPosition, pageWidth, pageHeight) {
    doc.setFontSize(11);
    doc.setTextColor(14, 165, 183);
    doc.text('Summary Information', margin, yPosition);
    yPosition += 8;

    var summaryData = [
        ['Metric', 'Value'],
        ['Total Devices', data.totalDevices || 0],
        ['Online Devices', data.onlineDevices || 0],
        ['Apps Deployed', data.appsDeployed || 0],
        ['Active Configurations', data.activeConfigurations || 0],
        ['Total Alerts', data.totalAlerts || 0],
        ['System Uptime', data.systemUptime || 'N/A']
    ];

    doc.autoTable({
        startY: yPosition,
        head: [summaryData[0]],
        body: summaryData.slice(1),
        margin: { left: margin, right: margin }
    });
}

// Controller integration example:
// In SummaryController, add these methods:
/*
$scope.exportPDF = function() {
    var reportData = {
        totalDevices: $scope.deviceCount,
        onlineCount: $scope.onlineCount,
        offlineCount: $scope.offlineCount,
        idleCount: $scope.idleCount,
        androidVersions: $scope.androidVersions,
        batteryGood: $scope.batteryGood,
        batteryMedium: $scope.batteryMedium,
        batteryLow: $scope.batteryLow
    };
    exportReportToPDFAdvanced('telemetry', 'Telemetry Report', reportData);
};

$scope.exportCSV = function() {
    adminConsoleService.exportReportCSV('telemetry', {});
};
*/
