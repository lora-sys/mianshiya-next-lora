import ACCESS_ENUM, { type AccessEnum } from "@/app/access/accessEnum";

/**
 * 检查当前登录用户是否有某个权限
 * @param loginUser
 * @param needAccess
 * @return boolean 有无权限
 */
const checkAccess = (
  loginUser: API.LoginUserVO,
  needAccess: AccessEnum = ACCESS_ENUM.NOT_LOGIN,
) => {
  // 从登录用户信息中获取用户权限 (如果没有登录则代表没有权限)
  const loginUserAccess = loginUser?.userRole ?? ACCESS_ENUM.NOT_LOGIN;

  //如果当不需要任何权限
  if (needAccess === ACCESS_ENUM.NOT_LOGIN) {
    return true;
  }

  //如果需要登录才能访问
  if (needAccess === ACCESS_ENUM.USER) {
    //如果用户没有登录，则表示无权限
    if (loginUserAccess === ACCESS_ENUM.NOT_LOGIN) {
      return false;
    }
  }

  //需要管理员权限才能访问
  if (needAccess === ACCESS_ENUM.ADMIN) {
    //如果要有管理员权限，如果没有代表没有权限
    if (loginUserAccess !== ACCESS_ENUM.ADMIN) {
      return false;
    }
  }
  return true;
};

export default checkAccess;
