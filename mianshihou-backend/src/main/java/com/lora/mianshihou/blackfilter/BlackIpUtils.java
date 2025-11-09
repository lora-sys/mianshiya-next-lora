package com.lora.mianshihou.blackfilter;


import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 *
 * 黑白名单过滤器
 */
@Slf4j
public class BlackIpUtils {

    private static BitMapBloomFilter bitMapBloomFilter= new BitMapBloomFilter(100);

//判断ip是否在黑名单

    public static boolean isBlackIp(String ip) {
        return bitMapBloomFilter.contains(ip);
    }


    //重建ip黑名单
    public static void rebuildBlackIp(String configInfo) {
        if (StringUtils.isBlank(configInfo)) {
            configInfo = "{}";
        }
        //解析yaml配置文件
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(configInfo, Map.class);
        List<String> blackIpList = (List<String>) map.get("blackIpList");
        //工具类加锁防止并发问题，
        synchronized (BlackIpUtils.class) {
            int listSize = blackIpList.size();
            log.info("黑名单IP数量: {}", listSize);

            // 严格限制IP数量，防止内存溢出
            if (listSize > 1000) {
                log.warn("黑名单IP数量过多({})，截取前1000个", listSize);
                blackIpList = blackIpList.subList(0, 1000);
                listSize = 1000;
            }
            int capacity = Math.min(Math.max(listSize, 10), 100);
            log.info("使用容量: {}", capacity);
            if (CollUtil.isNotEmpty(blackIpList)) {
                //注意构造参数的设置
                BitMapBloomFilter bloomFilter = new BitMapBloomFilter( capacity);
                for (String blackIp : blackIpList) {
                    bloomFilter.add(blackIp);
                }
                bitMapBloomFilter = bloomFilter;
            } else {
                bitMapBloomFilter = new BitMapBloomFilter(100);
            }
        }
    }


}
