import ReactECharts from "echarts-for-react";
import { useEffect, useState } from "react";
import dayjs from "dayjs";
import { useUserSignInRecord } from "@/services/userServvice";

interface Props {}
/**
 * 刷题日历图
 * @param props
 * @constructor
 */
const CalendarChart = (props: Props) => {
  //签到日期列表（[1,200]） ,表示第一天和第200天签到记录
  const [dataList, setDataList] = useState<number[]>([]);
  //计算图标所需要的数据
  const year = new Date().getFullYear();

  // 使用SWR获取数据
  const { data: signInData, isLoading, error } = useUserSignInRecord(year);

  // 当数据变化时更新本地状态
  useEffect(() => {
    if (Array.isArray(signInData?.data)) {
      setDataList(signInData.data as number[]);
    }
  }, [signInData]);

  // 处理错误
  useEffect(() => {
    if (error) {
      console.error("获取刷题记录失败，", error);
    }
  }, [error]);

  if (!dataList || dataList.length === 0) {
    return <div>暂无刷题记录</div>;
  }

  const optionsData = dataList.map((dayOfYear) => {
    //计算日期字符串
    const dateStr = dayjs(`${year}-01-01`)
      .add(dayOfYear - 1, "day")
      .format("YYYY-MM-DD");
    return [dateStr, 1];
  });

  const option = {
    visualMap: {
      show: false,
      min: 0,
      max: 1,
      inRange: {
        color: ["#efefef", "lightgreen"], //颜色从灰色到浅绿
      },
    },
    calendar: {
      cellSize: ["auto", 16],
      range: year,
      yearLabel: {
        position: "top",
        formatter: `${year}年刷题记录`,
      },
      left: 20,
    },
    series: {
      type: "heatmap",
      coordinateSystem: "calendar",
      data: optionsData,
    },
  };

  // 显示加载状态
  if (isLoading) {
    return <div>加载中...</div>;
  }

  return (
    <div className="Calendar-chart">
      <ReactECharts option={option} style={{ height: 400 }} />
    </div>
  );
};

export default CalendarChart;