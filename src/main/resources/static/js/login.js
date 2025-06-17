import { getCookie } from './device-info.js';

document.addEventListener("deviceInfoReady", function () {
    const deviceInfoFromCookie = getCookie("deviceInfoCookie");
    const deviceInfoHiddenInput = document.getElementById("deviceInfoField");

    if (!deviceInfoFromCookie || !deviceInfoHiddenInput) {
        console.warn("⚠ Không tìm thấy thông tin thiết bị hoặc input hidden.");
        return;
    }

    const deviceInfoPayload = {
        deviceType: deviceInfoFromCookie.deviceType || null,
        osName: deviceInfoFromCookie.osName || null,
        osVersion: deviceInfoFromCookie.osVersion || null,
        browserName: deviceInfoFromCookie.browserName || null,
        browserVersion: deviceInfoFromCookie.browserVersion || null,
        screenWidth: deviceInfoFromCookie.screenWidth || 0,
        screenHeight: deviceInfoFromCookie.screenHeight || 0,
        userAgent: deviceInfoFromCookie.userAgent || null,
        language: deviceInfoFromCookie.language || null,
        timeZone: deviceInfoFromCookie.timeZone || null,
        deviceId: deviceInfoFromCookie.deviceId || null
    };

    deviceInfoHiddenInput.value = JSON.stringify(deviceInfoPayload);
    console.log("✅ Gán deviceInfo vào input ẩn:", deviceInfoPayload);
});
