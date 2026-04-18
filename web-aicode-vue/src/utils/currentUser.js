const USER_TOKEN_KEY = 'app_current_emp_no';
const USER_TOKEN_EXPIRE_KEY = 'app_current_emp_no_expire_at';
const DEFAULT_EMP_NO = '2036377';
const TEN_DAYS_MS = 10 * 24 * 60 * 60 * 1000;

function isValidEmpNo(empNo) {
  return /^\d{7}$/.test(empNo || '');
}

function clearStoredEmpNo() {
  localStorage.removeItem(USER_TOKEN_KEY);
  localStorage.removeItem(USER_TOKEN_EXPIRE_KEY);
}

export function getCurrentEmpNo() {
  const stored = localStorage.getItem(USER_TOKEN_KEY);
  const expireAt = Number(localStorage.getItem(USER_TOKEN_EXPIRE_KEY) || 0);
  const now = Date.now();

  if (!stored || !isValidEmpNo(stored) || !expireAt || now > expireAt) {
    clearStoredEmpNo();
    localStorage.setItem(USER_TOKEN_KEY, DEFAULT_EMP_NO);
    localStorage.setItem(USER_TOKEN_EXPIRE_KEY, String(now + TEN_DAYS_MS));
    return DEFAULT_EMP_NO;
  }

  return stored;
}

export function setCurrentEmpNo(empNo) {
  if (!isValidEmpNo(empNo)) {
    throw new Error('员工号必须是7位数字');
  }
  localStorage.setItem(USER_TOKEN_KEY, empNo);
  localStorage.setItem(USER_TOKEN_EXPIRE_KEY, String(Date.now() + TEN_DAYS_MS));
  window.dispatchEvent(new CustomEvent('emp-no-change', { detail: { empNo } }));
}

export function getDefaultEmpNo() {
  return DEFAULT_EMP_NO;
}

export function isEmpNoValid(empNo) {
  return isValidEmpNo(empNo);
}
