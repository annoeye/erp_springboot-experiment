// Hàm tạo UUID nếu browser không hỗ trợ crypto.randomUUID
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Lấy hoặc tạo Device ID lưu vào localStorage
function getOrCreateDeviceId() {
    let deviceId = localStorage.getItem("myAppDeviceId");
    if (!deviceId) {
        deviceId = (typeof crypto !== 'undefined' && crypto.randomUUID)
            ? crypto.randomUUID()
            : generateUUID();
        localStorage.setItem("myAppDeviceId", deviceId);
    }
    return deviceId;
}

export function setCookie(name, value, days) {
    let expires = "";
    if (days) {
        const date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }
    const stringValue = encodeURIComponent(JSON.stringify(value));
    document.cookie = name + "=" + stringValue + expires + "; path=/";
    console.log(`✅ Cookie '${name}' đã được lưu:`, value);
}

export function getCookie(name) {
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for (let c of ca) {
        c = c.trim();
        if (c.indexOf(nameEQ) === 0) {
            try {
                return JSON.parse(decodeURIComponent(c.substring(nameEQ.length)));
            } catch (e) {
                console.error("❌ Lỗi parse JSON từ cookie:", e);
                return null;
            }
        }
    }
    return null;
}


// Chờ DOM ready mới thực hiện
document.addEventListener("DOMContentLoaded", function () {
    if (typeof UAParser === 'undefined') {
        console.error("❌ UAParser.js chưa được load. Vui lòng kiểm tra lại script.");
        return;
    }

    const parser = new UAParser();
    const result = parser.getResult();

    const deviceInfo = {
        deviceType: result.device.type || "Desktop",
        osName: result.os.name || "Unknown",
        osVersion: result.os.version || "Unknown",
        browserName: result.browser.name || "Unknown",
        browserVersion: result.browser.version || "Unknown",
        screenWidth: window.screen.width,
        screenHeight: window.screen.height,
        userAgent: navigator.userAgent,
        language: navigator.language || navigator.userLanguage,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        deviceId: getOrCreateDeviceId()
    };

    setCookie("deviceInfoCookie", deviceInfo, 7);
    document.dispatchEvent(new CustomEvent("deviceInfoReady"));

    // (Không bắt buộc) Gửi lên server
    /*
    fetch("/auth/device-info", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(deviceInfo)
    })
    .then(res => {
        if (res.ok) {
            console.log("✅ Gửi thông tin thiết bị thành công.");
        } else {
            console.warn(`❌ Gửi thất bại. Mã trạng thái: ${res.status}`);
        }
    })
    .catch(error => {
        console.error("❌ Lỗi khi gửi device info:", error);
    });
    */
});
