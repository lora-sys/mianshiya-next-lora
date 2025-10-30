import loginUser from "@/stores/loginUser";
import { menus } from "../../../config/menu";
import checkAccess from "@/app/access/checkAccess";

/**
 * 递归过滤
 * 获取有权限，可以访问的菜单
 * @param loginUser
 * @param menuItems
 */
const getAccessibleMenus = (loginUser: API.LoginUserVO, menuItems = menus) => {
  return menuItems.filter((item) => {
    if (!checkAccess(loginUser, item.access)) {
      return false;
    }
    if (item.children) {
      item.children = getAccessibleMenus(loginUser, item.children);
    }
    return true;
  });
};

export default getAccessibleMenus;
