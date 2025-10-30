/**
 *
 * 权限定义
 */

const ACCESS_ENUM = {
  NOT_LOGIN :"notlogin",
  USER : "user",
  ADMIN : "admin",
} as const; // Use 'as const' to make it a literal type

// Export type definition
export type AccessEnum = typeof ACCESS_ENUM[keyof typeof ACCESS_ENUM];

export default ACCESS_ENUM;