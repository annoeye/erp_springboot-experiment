function createOption(start, end, elementId, order = 'asc' ) {
    const select = document.getElementById(elementId);
    if (!select) return;

    const range = [];
    for (let i = start; i <= end; i++) range.push(i);
    if (order === 'desc') range.reverse();

    for (const value of range) {
        const option = document.createElement("option");
        option.value = value;
        option.text = value;
        select.appendChild(option);
    }
}

function initDatePiker() {
    createOption(1, 31, "select-day")
    createOption(1, 12, "select-month")
    const thisYear = new Date().getFullYear()
    createOption(1900, thisYear, "select-year", "desc")
}

function validateField(inputElement) {
    const value = inputElement.value.trim();
    let isValid = true;
    let message = '';

    switch (inputElement.id) {
        case 'username' :
            if (value.length === 0) {
                isValid = false;
                message = 'Tên đăng nhập không được để trống.';
            } else if (value.length < 6) {
                isValid = false;
                message = 'Tên đăng nhập phải có ít nhất 6 ký tự.';
            }
            break;

        case 'email':
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (value.length === 0) {
                isValid = false;
                message = 'Email không được để trống.';
            } else if (!emailRegex.test(value)) {
                isValid = false;
                message = 'Định dạng email không hợp lệ.';
            }
            break;

        case 'password':
            if (value.length === 0) {
                isValid = false;
                message = 'Mật khẩu không được để trống.';
            } else if (value.length < 6) {
                isValid = false;
                message = 'Mật khẩu phải có ít nhất 6 ký tự.';
            }
            break;

        case 'confirmPassword':
            const passwordInput = document.getElementById(inputElement.id);
            if (value.length === 0) {
                isValid = false;
                message = 'Xác nhận mật khẩu không được để trống.';
            } else if (value !== passwordInput.value) {
                isValid = false;
                message = 'Xác nhận mật khẩu không đúng.';
            }
            break;
    }
}

function initRealtimeValidation() {
    const fieldsToValidate = ['username', 'email', 'password', 'confirmPassword'];

    fieldsToValidate.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.addEventListener('input', () => validateField(input));
        }
    })
}


document.addEventListener('DOMContentLoaded', () => {
    initDatePiker();
    initRealtimeValidation();
});