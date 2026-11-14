package com.aurxsiu.bupthelper.config;

import com.aurxsiu.bupthelper.helprequest.entity.HelpType;
import com.aurxsiu.bupthelper.helprequest.entity.Region;
import com.aurxsiu.bupthelper.helprequest.repository.HelpTypeRepository;
import com.aurxsiu.bupthelper.helprequest.repository.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时初始化帮忙类型与地域字典（《需求说明.md》三.2、三.3），
 * 与 doc/接口设计-请帮忙模块.md 6.3 保持一致。
 */
@Component
public class HelpDictInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HelpDictInitializer.class);

    private final HelpTypeRepository typeRepository;
    private final RegionRepository regionRepository;

    public HelpDictInitializer(HelpTypeRepository typeRepository, RegionRepository regionRepository) {
        this.typeRepository = typeRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (typeRepository.count() == 0) {
            typeRepository.saveAll(List.of(
                    type("TOUR_GUIDE", "导游", 1),
                    type("SHOPPING", "代购", 2),
                    type("PET_CARE", "宠物代管", 3),
                    type("PLANT_CARE", "绿植代培", 4),
                    type("HOSPITAL_CARE", "住院陪护", 5),
                    type("OTHER", "其他", 99)));
            log.info("已初始化帮忙类型字典");
        }

        if (regionRepository.count() == 0) {
            regionRepository.saveAll(List.of(
                    region("海淀区", "北京市", "北京市", 1),
                    region("朝阳区", "北京市", "北京市", 2),
                    region("西城区", "北京市", "北京市", 3),
                    region("昌平区", "北京市", "北京市", 4),
                    region("浦东新区", "上海市", "上海市", 5),
                    region("徐汇区", "上海市", "上海市", 6),
                    region("长安区", "石家庄市", "河北省", 7),
                    region("天河区", "广州市", "广东省", 8)));
            log.info("已初始化地域字典");
        }
    }

    private HelpType type(String typeCode, String typeName, int sortNo) {
        HelpType type = new HelpType();
        type.setTypeCode(typeCode);
        type.setTypeName(typeName);
        type.setSortNo(sortNo);
        return type;
    }

    private Region region(String regionName, String city, String province, int sortNo) {
        Region region = new Region();
        region.setRegionName(regionName);
        region.setCity(city);
        region.setProvince(province);
        region.setSortNo(sortNo);
        return region;
    }
}
