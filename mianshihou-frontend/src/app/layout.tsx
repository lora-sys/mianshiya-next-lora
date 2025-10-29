import "./globals.css";
import { AntdRegistry } from '@ant-design/nextjs-registry';
import React from 'react';
import BasicLayout from "@/layouts/BasicLayout";


const RootLayout = ({ children }: React.PropsWithChildren) => (
  <html lang="zh">
    <body>
      <AntdRegistry>
        <BasicLayout>{children}</BasicLayout>
      </AntdRegistry>
    </body>
  </html>
);

export default RootLayout;



