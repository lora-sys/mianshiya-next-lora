import { useRouter } from "next/router";
import { useSelector } from "react-redux";
import { RootState } from "@/stores";
import { useEffect } from "react";


/**
 * 高阶hoc组件,对组件进行权限校验，更方便
 *@usage
 * // pages/protected.js
 *  import withAuth from '@/components/withAuth';
 *  function ProtectedPage() {
 *  return <div>This is a protected page.</div>;
 *  }
 *  export default withAuth(ProtectedPage)
 * @param Component
 */
export default function withAuth(Component:any){
  return function AuthenticatedComponent(props :any){
    const router = useRouter();
    const isAuthenticated = useSelector((state:RootState)=>state.loginUser);
    useEffect(() => {
      if(!isAuthenticated){
        router.push("/login");
      }
    }, [isAuthenticated]);

    //没有登录,不渲染组件
    if(!isAuthenticated){
      return null;
    }
    //如果以及登录成功，渲染组件
    return <Component  {...props} />
  }
}