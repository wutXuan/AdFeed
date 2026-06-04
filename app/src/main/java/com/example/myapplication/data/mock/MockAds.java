package com.example.myapplication.data.mock;

import com.example.myapplication.model.AdChannels;
import com.example.myapplication.model.AdItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MockAds {
    private static final String SAMPLE_VIDEO_ONE = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
    private static final String SAMPLE_VIDEO_TWO = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

    private MockAds() {
    }

    public static List<AdItem> create() {
        List<AdItem> ads = new ArrayList<>();
        int order = 0;
        ads.add(ad("featured-01", AdChannels.FEATURED, AdItem.TYPE_LARGE_IMAGE, "晨跑也能像开盲盒", "PulseRun",
                "轻量缓震跑鞋，适合通勤前 30 分钟唤醒身体。",
                image("photo-1460353581641-37baddab0fa2"), null, 128, order++,
                "运动", "学生党", "轻量"));
        ads.add(ad("featured-02", AdChannels.FEATURED, AdItem.TYPE_VIDEO, "三分钟做出露营拿铁", "CampBean",
                "便携冷萃套装，户外和办公室都能稳定出杯。",
                SAMPLE_VIDEO_ONE, image("photo-1495474472287-4d71bcdd2085"), 96, order++,
                "露营", "咖啡", "治愈"));
        ads.add(ad("featured-03", AdChannels.FEATURED, AdItem.TYPE_SMALL_IMAGE, "宿舍桌面变清爽", "GridBox",
                "磁吸收纳和分区线槽，把小桌面整理成高效工作台。",
                image("photo-1497366754035-f200968a6e72"), null, 214, order++,
                "数码", "学生党", "收纳"));
        ads.add(ad("featured-04", AdChannels.FEATURED, AdItem.TYPE_LARGE_IMAGE, "把周末装进口袋", "PocketTrip",
                "城市微旅行路线卡，自动生成吃喝玩乐半日计划。",
                image("photo-1500530855697-b586d89ba3ee"), null, 77, order++,
                "本地", "周末", "年轻人"));
        ads.add(ad("featured-05", AdChannels.FEATURED, AdItem.TYPE_VIDEO, "一键切换专注氛围", "FocusGlow",
                "智能氛围灯根据音乐和工作节奏切换光效。",
                SAMPLE_VIDEO_TWO, image("photo-1507473885765-e6ed057f782c"), 142, order++,
                "家居", "氛围感", "效率"));
        ads.add(ad("featured-06", AdChannels.FEATURED, AdItem.TYPE_SMALL_IMAGE, "健身餐不再像任务", "FreshFit",
                "低脂高蛋白套餐，每周换菜单，口味不重复。",
                image("photo-1498837167922-ddd27525d352"), null, 165, order++,
                "健康", "性价比", "健身"));

        order = 0;
        ads.add(ad("shop-01", AdChannels.ECOMMERCE, AdItem.TYPE_LARGE_IMAGE, "能装下一天的小方包", "MonoCarry",
                "通勤轻包，手机、耳机、雨伞和补妆都分区收纳。",
                image("photo-1542291026-7eec264c27ff"), null, 188, order++,
                "通勤", "性价比", "潮流"));
        ads.add(ad("shop-02", AdChannels.ECOMMERCE, AdItem.TYPE_SMALL_IMAGE, "手机拍照也有电影感", "LensPop",
                "磁吸补光镜头，夜景和美食拍摄更稳定。",
                image("photo-1511707171634-5f897ff02aa9"), null, 301, order++,
                "数码", "拍照", "学生党"));
        ads.add(ad("shop-03", AdChannels.ECOMMERCE, AdItem.TYPE_VIDEO, "五分钟早餐不翻车", "MorningKit",
                "全麦贝果、鸡胸和酱料组合，省时又有饱腹感。",
                SAMPLE_VIDEO_ONE, image("photo-1490818387583-1baba5e638af"), 119, order++,
                "早餐", "健康", "性价比"));
        ads.add(ad("shop-04", AdChannels.ECOMMERCE, AdItem.TYPE_LARGE_IMAGE, "租房也能有好睡眠", "CloudNest",
                "卷包床垫，搬家友好，回弹和支撑兼顾。",
                image("photo-1505693416388-ac5ce068fe85"), null, 84, order++,
                "家居", "租房", "舒适"));
        ads.add(ad("shop-05", AdChannels.ECOMMERCE, AdItem.TYPE_SMALL_IMAGE, "耳机里的通勤结界", "QuietPods",
                "主动降噪耳机，地铁、咖啡馆和自习室都更沉浸。",
                image("photo-1505740420928-5e560c06d30e"), null, 248, order++,
                "数码", "通勤", "效率"));
        ads.add(ad("shop-06", AdChannels.ECOMMERCE, AdItem.TYPE_VIDEO, "衣柜变聪明一点", "SmartHanger",
                "可折叠烘干衣架，阴雨天也能快速处理小件衣物。",
                SAMPLE_VIDEO_TWO, image("photo-1523381294911-8d3cead13475"), 66, order++,
                "家居", "租房", "实用"));

        order = 0;
        ads.add(ad("local-01", AdChannels.LOCAL, AdItem.TYPE_LARGE_IMAGE, "今晚去听一场小型现场", "LiveCorner",
                "本地独立音乐空间，工作日也有轻松场次。",
                image("photo-1501386761578-eac5c94b800a"), null, 72, order++,
                "本地", "音乐", "周末"));
        ads.add(ad("local-02", AdChannels.LOCAL, AdItem.TYPE_SMALL_IMAGE, "附近新开的深夜面馆", "NoodleLab",
                "手作汤底，夜跑后和加班后都能快速回血。",
                image("photo-1552611052-33e04de081de"), null, 153, order++,
                "美食", "夜宵", "本地"));
        ads.add(ad("local-03", AdChannels.LOCAL, AdItem.TYPE_VIDEO, "城市骑行路线挑战", "RideMap",
                "每周解锁一条安全骑行路线，完成后可领本地优惠。",
                SAMPLE_VIDEO_ONE, image("photo-1485965120184-e220f721d03e"), 207, order++,
                "运动", "本地", "年轻人"));
        ads.add(ad("local-04", AdChannels.LOCAL, AdItem.TYPE_LARGE_IMAGE, "把下午茶改成手作课", "CraftHour",
                "陶艺、香氛和皮具体验课程，适合朋友小聚。",
                image("photo-1452860606245-08befc0ff44b"), null, 88, order++,
                "手作", "周末", "治愈"));
        ads.add(ad("local-05", AdChannels.LOCAL, AdItem.TYPE_SMALL_IMAGE, "自习室还有 AI 座位推荐", "StudyHub",
                "按安静程度、插座和空调温度推荐合适座位。",
                image("photo-1497633762265-9d179a990aa6"), null, 131, order++,
                "学生党", "效率", "本地"));
        ads.add(ad("local-06", AdChannels.LOCAL, AdItem.TYPE_VIDEO, "小区门口的鲜花订阅", "BloomDay",
                "每周一束当季花材，送到家门口，顺手换个心情。",
                SAMPLE_VIDEO_TWO, image("photo-1490750967868-88aa4486c946"), 99, order++,
                "生活", "治愈", "本地"));
        return ads;
    }

    private static AdItem ad(String id, String channel, String type, String title, String brand,
                             String description, String mediaUrl, String thumbnailUrl, int likes,
                             int sortOrder, String... tags) {
        AdItem item = new AdItem();
        item.setId(id);
        item.setChannel(channel);
        item.setType(type);
        item.setTitle(title);
        item.setBrand(brand);
        item.setDescription(description);
        item.setMediaUrl(mediaUrl);
        item.setThumbnailUrl(thumbnailUrl == null ? mediaUrl : thumbnailUrl);
        item.setTargetUrl("https://example.com/ads/" + id);
        item.setLikeCount(likes);
        item.setTags(Arrays.asList(tags));
        item.setSortOrder(sortOrder);
        return item;
    }

    private static String image(String id) {
        return "https://images.unsplash.com/" + id + "?auto=format&fit=crop&w=1200&q=80";
    }
}
