"use client";

import "./index.css";
import { Avatar, Card, Col, Row } from "antd";
import { useSelector } from "react-redux";
import { RootState } from "@/stores";
import Title from "antd/es/typography/Title";
import Paragraph from "antd/es/typography/Paragraph";
import { useState } from "react";
import CalendarChart from "@/app/user/center/components/CalendarChart";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

/**
 * 用户中心组件
 * @constructor
 */
export default function UserCenterPage() {
  //获取登录用户信息
  const loginUser = useSelector((state: RootState) => state.loginUser);
  //便于复用，新奇一个变量
  const user = loginUser;

  //控制菜单栏的tab高亮
  const [activeTabKey, setActiveTabKey] = useState<string>("record");

  return (
    <div id="userCenterPage" className="max-width-content">
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card style={{ textAlign: "center" }}>
            <Avatar src={user.userAvatar} size={72} />
            <div style={{ marginBottom: 16 }}></div>
            <Card.Meta
              title={
                <Title level={4} style={{ marginBottom: 0 }}>
                  {user.userName}
                </Title>
              }
              description={
                <Paragraph type="secondary">{user.userProfile}</Paragraph>
              }
            />
          </Card>
        </Col>
        <Col xs={24} md={18}>
          <Card
            tabList={[
              {
                key: "record",
                label: "刷题记录",
              },
              {
                key: "others",
                label: "其他",
              },
            ]}
            activeTabKey={activeTabKey}
            onTabChange={(key: string) => {
              setActiveTabKey(key);
            }}
          >
            {activeTabKey === "record" && <CalendarChart></CalendarChart>}
            {activeTabKey === "others" && <>bbb</>}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
