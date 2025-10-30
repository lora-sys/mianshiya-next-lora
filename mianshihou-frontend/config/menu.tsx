import { MenuDataItem } from "@ant-design/pro-layout";
import { CrownOutlined } from "@ant-design/icons";
import accessEnum from "@/app/access/accessEnum";

// 菜单列表
export const menus = [
  {
    path: "/",
    name: "主页",
  },
  {
    path: "/banks",
    name: "题库",
  },
  {
    path: "/questions",
    name: "题目",
  },
  {
    name: "面试猴",
    path: "https://mianshihou.com",
    target: "_blank",
  },
  {
    path: "/admin",
    name: "管理",
    icon: <CrownOutlined />,
    access: accessEnum.ADMIN,
    children: [
      {
        path: "/admin/user",
        name: "用户管理",
        access: accessEnum.ADMIN,
      },
    ],
  },
] as MenuDataItem[];

//根据路径查找所有菜单
export const findAllMenuItemByPath = (path: string): MenuDataItem | null => {
  return findMenuItemByPath(menus, path);
};

const findMenuItemByPath = (
  menus: MenuDataItem[],
  path: string,
): MenuDataItem | null => {
  for (const menu of menus) {
    if (menu.path === path) {
      return menu;
    }
    if (menu.children) {
      // If the current menu has children, search within them
      const childResult = findMenuItemByPath(
        menu.children as MenuDataItem[],
        path,
      );
      if (childResult) {
        return childResult;
      }
    }
  }
  return null;
};
