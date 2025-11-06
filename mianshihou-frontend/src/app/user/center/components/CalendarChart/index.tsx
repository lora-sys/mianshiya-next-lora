import ReactECharts from "echarts-for-react";
import { useEffect, useState } from "react";
import dayjs from "dayjs";
import { getUserSignInRecordUsingGet } from "@/api/userController";

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
  //请求后端获取数据

  const fetchDataList = async () => {
    try {
      const res = await getUserSignInRecordUsingGet(year);
      setDataList(res.data as any);
    } catch (e: any) {
      // 在服务端渲染时，不能使用 message 组件
      console.error("获取刷题记录失败，", e.message);
    }
  };
  //保证只会调用一次
  useEffect(() => {
    fetchDataList();
  }, []);

  const optionsData = dataList.map((dayOfYear) => {
    //计算日期字符串
    const dateStr = dayjs(`${year}-01-01`)
      .add(dayOfYear - 1, "day")
      .format("YYYY-MM-DD");
    console.log(dateStr);
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
  return (
    <div className="Calendar-chart">
      return <ReactECharts option={option} style={{ height: 400 }} />;
    </div>
  );
};

export default CalendarChart;
