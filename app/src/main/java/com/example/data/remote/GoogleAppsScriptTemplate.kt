package com.example.data.remote

object GoogleAppsScriptTemplate {

    const val SCRIPT_TEMPLATE = """/**
 * Google Apps Script Web App for Driver Drowsiness Monitor
 * 
 * Instructions:
 * 1. Open your Google Sheet.
 * 2. Click Extensions > Apps Script.
 * 3. Replace any code with this entire script.
 * 4. Click Deploy > New deployment.
 * 5. Select type: "Web app".
 * 6. Set "Execute as": "Me".
 * 7. Set "Who has access": "Anyone".
 * 8. Copy the Web App URL and paste it into the app's Settings screen.
 */

function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    // Check if header row exists
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "Timestamp",
        "Date Time",
        "Driver Name",
        "Vehicle ID",
        "State",
        "Score",
        "PERCLOS",
        "EAR",
        "MAR",
        "Speed (km/h)",
        "Latitude",
        "Longitude"
      ]);
    }
    
    var data = JSON.parse(e.postData.contents);
    var dateStr = new Date(data.timestamp).toISOString();
    
    sheet.appendRow([
      data.timestamp,
      dateStr,
      data.driverName || "Driver",
      data.vehicleId || "VH-101",
      data.state || "UNKNOWN",
      data.score || 0,
      data.perclos || 0,
      data.ear || 0,
      data.mar || 0,
      data.speedKmh || 0,
      data.latitude || "",
      data.longitude || ""
    ]);
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Event recorded successfully"
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
"""
}
