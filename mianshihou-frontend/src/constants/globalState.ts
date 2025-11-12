// 全局登录状态
export const loginConflictState = {
  isConflict: false,
  showConflictAlert: () => {
    loginConflictState.isConflict = true;
    // 触发重新渲染
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("login-conflict"));
    }
  },
  hideConflictAlert: () => {
    loginConflictState.isConflict = false;
  },
};
