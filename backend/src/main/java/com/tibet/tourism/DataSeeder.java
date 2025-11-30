package com.tibet.tourism;

import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final String SPOT_IMAGE_BASE = "/images/spots/";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ScenicSpotRepository spotRepository;
    @Autowired
    private SpotTagRepository tagRepository;
    @Autowired
    private UserVisitHistoryRepository historyRepository;
    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private HeritageItemRepository heritageRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.tibet.tourism.service.PasswordEncryptionService passwordEncryptionService;
    
    @Autowired
    private com.tibet.tourism.service.TibetanTranslationService tibetanTranslationService;

    @Override
    public void run(String... args) throws Exception {
        // 初始化藏语词典
        System.out.println("初始化藏语词典...");
        tibetanTranslationService.initializeDefaultDictionary();
        
        seedUsers();
        
        // 清空现有景点数据并重新加载
        long spotCount = spotRepository.count();
        if (spotCount > 0) {
            System.out.println("清空现有景点数据...");
            spotRepository.deleteAll();
        }
        
        System.out.println("加载新景点数据...");
        seedSpots();
        
        if (newsRepository.count() == 0) {
            seedNews();
        }
        if (heritageRepository.count() == 0) {
            seedHeritage();
        }
        if (historyRepository.count() == 0) {
            seedHistory();
        }
    }

    private void seedUsers() {
        // 创建或更新 admin 用户
        userRepository.findByUsername("admin").ifPresentOrElse(
            existing -> {
                // 如果密码未加密（长度小于20，BCrypt hash通常更长），则更新
                if (existing.getPassword().length() < 20) {
                    String plainPassword = "admin123";
                    existing.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                    userRepository.save(existing);
                    System.out.println("已更新 admin 用户密码");
                } else if (existing.getEncryptedPassword() == null || existing.getEncryptedPassword().isEmpty()) {
                    // 如果BCrypt已存在但AES加密不存在，补充AES加密
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt("admin123"));
                    userRepository.save(existing);
                    System.out.println("已补充 admin 用户AES加密密码");
                }
            },
            () -> {
                String plainPassword = "admin123";
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                admin.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                admin.setRole(User.Role.ADMIN);
                admin.setNickname("管理员");
                userRepository.save(admin);
                System.out.println("已创建 admin 用户");
            }
        );

        // 创建或更新 lzh 超级管理员
        userRepository.findByUsername("lzh").ifPresentOrElse(
            existing -> {
                if (existing.getPassword().length() < 20) {
                    String plainPassword = "031224";
                    existing.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                    userRepository.save(existing);
                    System.out.println("已更新 lzh 用户密码");
                } else if (existing.getEncryptedPassword() == null || existing.getEncryptedPassword().isEmpty()) {
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt("031224"));
                    userRepository.save(existing);
                    System.out.println("已补充 lzh 用户AES加密密码");
                }
            },
            () -> {
                String plainPassword = "031224";
                User superAdmin = new User();
                superAdmin.setUsername("lzh");
                superAdmin.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                superAdmin.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                superAdmin.setRole(User.Role.ADMIN);
                superAdmin.setNickname("超级管理员");
                userRepository.save(superAdmin);
                System.out.println("已创建 lzh 用户");
            }
        );

        // 创建或更新 user1
        userRepository.findByUsername("user1").ifPresentOrElse(
            existing -> {
                if (existing.getPassword().length() < 20) {
                    String plainPassword = "123456";
                    existing.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                    userRepository.save(existing);
                    System.out.println("已更新 user1 用户密码");
                } else if (existing.getEncryptedPassword() == null || existing.getEncryptedPassword().isEmpty()) {
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt("123456"));
                    userRepository.save(existing);
                    System.out.println("已补充 user1 用户AES加密密码");
                }
            },
            () -> {
                String plainPassword = "123456";
                User user1 = new User();
                user1.setUsername("user1");
                user1.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                user1.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                user1.setNickname("扎西");
                userRepository.save(user1);
                System.out.println("已创建 user1 用户");
            }
        );

        // 创建或更新 user2
        userRepository.findByUsername("user2").ifPresentOrElse(
            existing -> {
                if (existing.getPassword().length() < 20) {
                    String plainPassword = "123456";
                    existing.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                    userRepository.save(existing);
                    System.out.println("已更新 user2 用户密码");
                } else if (existing.getEncryptedPassword() == null || existing.getEncryptedPassword().isEmpty()) {
                    existing.setEncryptedPassword(passwordEncryptionService.encrypt("123456"));
                    userRepository.save(existing);
                    System.out.println("已补充 user2 用户AES加密密码");
                }
            },
            () -> {
                String plainPassword = "123456";
                User user2 = new User();
                user2.setUsername("user2");
                user2.setPassword(passwordEncoder.encode(plainPassword)); // BCrypt
                user2.setEncryptedPassword(passwordEncryptionService.encrypt(plainPassword)); // AES
                user2.setNickname("卓玛");
                userRepository.save(user2);
                System.out.println("已创建 user2 用户");
            }
        );
    }

    private void seedSpots() {
        // 热门景点（点击量较高）
        createSpot("布达拉宫", "པོ་ཏ་ལ", "世界海拔最高的宫堡式建筑群，藏传佛教圣地。", "འཛམ་གླིང་གི་མཐོ་ཚད་ཆེས་མཐོ་བའི་གནས་ས་རིག་གནས་ཀྱི་རི་མོ་དང་བོད་ཀྱི་རིག་གནས་ཀྱི་གནས་ས་རྙེད་པ།", ScenicSpot.Category.CULTURAL, "200", "29.6578", "91.1172", spotImage("布达拉宫.jpg"), Arrays.asList("宫殿", "佛教", "世界遗产"), 15000 + 5000);
        createSpot("大昭寺", "ཇོ་ཁང་", "拉萨老城中心，藏传佛教至高殿堂。", "ལྷ་སའི་གནས་ས་རྙེད་པའི་ནང་དུ། བོད་ཀྱི་རིག་གནས་ཀྱི་གནས་ས་རྙེད་པ།", ScenicSpot.Category.CULTURAL, "85", "29.6533", "91.1322", spotImage("大昭寺.jpg"), Arrays.asList("寺庙", "佛教", "朝圣"), 15000 + 4500);
        createSpot("纳木错", "གནམ་མཚོ", "西藏第二大湖泊，三大圣湖之一。", "བོད་ཀྱི་མཚོ་ཆེན་པོ་གཉིས་པ་དང་གནས་ས་རྙེད་པའི་གནས་ས་རྙེད་པ།", ScenicSpot.Category.NATURAL, "120", "30.7667", "90.5667", spotImage("纳木错.jpg"), Arrays.asList("湖泊", "自然", "圣湖"), 15000 + 4000);
        createSpot("羊卓雍措", "ཡར་འབྲོག་གཡུ་མཚོ", "三大圣湖之一，湖水蓝如宝石。", "གནས་ས་རྙེད་པའི་གནས་ས་རྙེད་པ་དང་མཚོའི་ཆུ་ནི་རིན་པོ་ཆེ་ལྟ་བུ་ཡིན་པ།", ScenicSpot.Category.NATURAL, "60", "28.9333", "90.6833", spotImage("羊卓雍措.jpg"), Arrays.asList("湖泊", "自然", "圣湖"), 15000 + 3800);
        createSpot("罗布林卡", "ནོར་བུ་གླིང་ཁ", "历代达赖喇嘛的夏宫。", "རྒྱུན་མཐུན་དུ་འགྲོ་བའི་དགེ་སློང་གི་གནས་ས་རྙེད་པ།", ScenicSpot.Category.CULTURAL, "60", "29.6483", "91.1033", spotImage("罗布林卡.jpg"), Arrays.asList("园林", "世界遗产", "拉萨"), 15000 + 3500);
        createSpot("巴松措", "བྲག་གསུམ་མཚོ", "林芝红教神湖。", "ཉིང་ཁྲིའི་དམར་པོའི་གནས་ས་རྙེད་པའི་མཚོ།", ScenicSpot.Category.NATURAL, "120", "29.9167", "93.8667", spotImage("巴松措.jpg"), Arrays.asList("湖泊", "森林", "5A景区"), 15000 + 3200);
        createSpot("雅鲁藏布江大峡谷", "ཡར་ཀླུངས་གཙང་པོའི་རྒྱུད་ཆེན", "世界最深峡谷。", "འཛམ་གླིང་གི་རྒྱུད་ཆེན་པོ་གསང་བའི་གནས་ས།", ScenicSpot.Category.NATURAL, "290", "29.6000", "95.0000", spotImage("雅鲁藏布江大峡谷.jpg"), Arrays.asList("峡谷", "自然", "林芝"), 15000 + 3000);
        createSpot("哲蚌寺", "འབྲས་སྤུངས་དགོན་པ", "拉萨三大寺之一，规模宏大。", "ལྷ་སའི་དགོན་པ་གསུམ་པའི་གཅིག་དང་ཆེས་ཆེ་བའི་གནས་ས།", ScenicSpot.Category.CULTURAL, "50", "29.6747", "91.0469", spotImage("哲蚌寺.jpg"), Arrays.asList("寺庙", "佛教", "拉萨"), 15000 + 2800);
        createSpot("色拉寺", "སེར་ར་དགོན་པ", "以辩经闻名的寺庙。", "དཔྱད་གཞི་ལ་གཞི་བཅོལ་ནས་གནས་ས་རྙེད་པའི་དགོན་པ།", ScenicSpot.Category.CULTURAL, "50", "29.6956", "91.1336", spotImage("色拉寺.jpg"), Arrays.asList("寺庙", "佛教", "辩经"), 15000 + 2600);
        createSpot("羊八井地热温泉", "ཡངས་པ་ཅིང་ས་དྲོད་ཆུ་ཚན", "世界最高地热温泉。", "འཛམ་གླིང་གི་ས་དྲོད་ཆུ་ཚན་མཐོ་ཚད་ཆེས་མཐོ་བ།", ScenicSpot.Category.NATURAL, "98", "30.0933", "90.5267", spotImage("羊八井地热温泉.jpg"), Arrays.asList("温泉", "自然", "拉萨"), 15000 + 2400);
        
        // 中等热门景点
        createSpot("珠穆朗玛峰", "ཇོ་མོ་གླང་མ", "世界最高峰，海拔8848米。", "འཛམ་གླིང་གི་མཐོ་ཚད་ཆེས་མཐོ་བའི་རི་གནོན་པོ། མཐོ་ཚད་8848 མི་ཡིན།", ScenicSpot.Category.NATURAL, "180", "28.1433", "86.8533", spotImage("珠穆朗玛峰.jpg"), Arrays.asList("雪山", "探险", "世界之巅"), 15000 + 2200);
        createSpot("南迦巴瓦峰", "གནམ་ལྕགས་དཔལ་བརྟན", "林芝最高峰，中国最美山峰。", "ཉིང་ཁྲིའི་མཐོ་ཚད་ཆེས་མཐོ་བའི་རི་གནོན་པོ། རྒྱ་ནག་གི་མཐོ་ཚད་ཆེས་མཐོ་བའི་རི་གནོན་པོ།", ScenicSpot.Category.NATURAL, "0", "29.6297", "95.0503", spotImage("南迦巴瓦峰.jpg"), Arrays.asList("雪山", "林芝", "自然"), 15000 + 2000);
        createSpot("鲁朗林海", "ཀླུ་རང་ནགས་ཚལ", "西藏江南，森林氧吧。", "བོད་ཀྱི་རྒྱ་ནག་གི་ནགས་ཚལ་དང་རླུང་གི་གནས་ས།", ScenicSpot.Category.NATURAL, "90", "29.7333", "94.7333", spotImage("鲁朗林海.jpg"), Arrays.asList("森林", "自然", "林芝"), 15000 + 1800);
        createSpot("然乌湖", "རང་བུ་མཚོ", "西藏东部最大冰川湖。", "བོད་ཀྱི་ཤར་ཕྱོགས་ཀྱི་མཚོ་ཆེན་པོ་གསུམ་པ་དང་གངས་རི་གི་མཚོ།", ScenicSpot.Category.NATURAL, "0", "29.4333", "96.8167", spotImage("然乌湖.jpg"), Arrays.asList("湖泊", "自然", "昌都"), 15000 + 1600);
        createSpot("扎什伦布寺", "བཀྲ་ཤིས་ལྷུན་པོ", "日喀则最大寺庙，格鲁派四大寺之一。", "གཞིས་རྩེའི་དགོན་པ་ཆེན་པོ་དང་དགེ་ལུགས་པའི་དགོན་པ་བཞི་པའི་གཅིག", ScenicSpot.Category.CULTURAL, "100", "29.2667", "88.8833", spotImage("扎什伦布寺.jpg"), Arrays.asList("寺庙", "佛教", "日喀则"), 15000 + 1500);
        createSpot("桑耶寺", "བསམ་ཡས་དགོན་པ", "西藏第一座寺庙。", "བོད་ཀྱི་དགོན་པ་དང་པོ།", ScenicSpot.Category.CULTURAL, "45", "29.2500", "91.5333", spotImage("桑耶寺.jpg"), Arrays.asList("寺庙", "历史", "山南"), 15000 + 1400);
        createSpot("米堆冰川", "མི་སྟེང་གངས་རི", "中国最美冰川之一。", "རྒྱ་ནག་གི་གངས་རི་མཐོ་ཚད་ཆེས་མཐོ་བའི་གཅིག", ScenicSpot.Category.NATURAL, "50", "29.4500", "96.5000", spotImage("米堆冰川.jpg"), Arrays.asList("冰川", "自然", "波密"), 15000 + 1300);
        createSpot("墨脱", "མེད་ཐོག", "中国最后通公路的县。", "རྒྱ་ནག་གི་ལམ་ཐོག་མཐའ་མའི་རྒྱལ་ཁབ།", ScenicSpot.Category.NATURAL, "160", "29.3250", "95.3333", spotImage("墨脱.png"), Arrays.asList("秘境", "热带雨林", "林芝"), 15000 + 1200);
        createSpot("雍布拉康", "ཡུམ་བུ་ལྷ་ཁང", "西藏第一座宫殿。", "བོད་ཀྱི་ཕོ་བྲང་དང་པོ།", ScenicSpot.Category.HISTORICAL, "60", "29.3167", "91.7667", spotImage("雍布拉康.png"), Arrays.asList("宫殿", "历史", "山南"), 15000 + 1100);
        createSpot("甘丹寺", "དགའ་ལྡན་དགོན་པ", "格鲁派第一座寺庙。", "དགེ་ལུགས་པའི་དགོན་པ་དང་པོ།", ScenicSpot.Category.CULTURAL, "45", "29.7567", "91.4756", spotImage("甘丹寺.jpg"), Arrays.asList("寺庙", "佛教", "格鲁派"), 15000 + 1000);
        
        // 较冷门景点
        createSpot("冈仁波齐", "གངས་རིན་པོ་ཆེ", "藏传佛教公认的神山。", "བོད་ཀྱི་རིག་གནས་ཀྱི་རི་གནོན་པོ་གནས་ས་རྙེད་པ།", ScenicSpot.Category.CULTURAL, "150", "31.0667", "81.3125", spotImage("冈仁波齐.jpg"), Arrays.asList("神山", "朝圣", "阿里"), 15000 + 900);
        createSpot("玛旁雍措", "མ་ཕམ་གཡུ་མཚོ", "三大圣湖之一，透明度极高。", "གནས་ས་རྙེད་པའི་གནས་ས་རྙེད་པ་གསུམ་པའི་གཅིག་དང་གསལ་བཤད་ཆེས་མཐོ་བ།", ScenicSpot.Category.NATURAL, "150", "30.6667", "81.4667", spotImage("玛旁雍措.jpg"), Arrays.asList("湖泊", "圣湖", "阿里"), 15000 + 800);
        createSpot("卡若拉冰川", "ཁ་རོ་ལ་གངས་རི", "距离公路最近的冰川。", "ལམ་ཐོག་གི་ཉེ་འཁོར་གྱི་གངས་རི།", ScenicSpot.Category.NATURAL, "50", "28.8867", "90.4133", spotImage("卡若拉冰川.jpg"), Arrays.asList("冰川", "自然", "日喀则"), 15000 + 700);
        createSpot("来古冰川", "ལེགས་གུ་གངས་རི", "西藏最宽冰川之一。", "བོད་ཀྱི་གངས་རི་ཆེས་ཆེ་བའི་གཅིག", ScenicSpot.Category.NATURAL, "30", "29.3000", "96.8333", spotImage("来古冰川.jpg"), Arrays.asList("冰川", "自然", "昌都"), 15000 + 600);
        createSpot("拉姆拉错", "ལྷ་མོ་ལ་མཚོ", "能看到前世今生的神湖。", "སྔོན་གྱི་ལོ་རྒྱུས་དང་ད་ལྟའི་ལོ་རྒྱུས་མཐོང་ཆོག་པའི་མཚོ་གནས།", ScenicSpot.Category.CULTURAL, "50", "29.0167", "92.9667", spotImage("拉姆拉错.png"), Arrays.asList("圣湖", "神秘", "山南"), 15000 + 500);
        createSpot("萨迦寺", "ས་སྐྱ་དགོན་པ", "萨迦派主寺，藏有大量经书。", "ས་སྐྱ་པའི་དགོན་པ་གཙོ་བོ་དང་ཆོས་ལུགས་ཀྱི་ཡིག་ཆ་མང་པོ་ཉར་ཚགས་བྱས་པ།", ScenicSpot.Category.CULTURAL, "45", "28.9000", "88.0167", spotImage("萨迦寺.jpg"), Arrays.asList("寺庙", "佛教", "日喀则"), 15000 + 400);
        createSpot("古格王国遗址", "གུ་གེ་རྒྱལ་ཁབ་གནས་ས", "神秘的古格文明遗址。", "གསང་བའི་གུ་གེ་རིག་གནས་ཀྱི་གནས་ས།", ScenicSpot.Category.HISTORICAL, "65", "31.4833", "79.8000", spotImage("古格王国遗址.jpeg"), Arrays.asList("遗址", "历史", "阿里"), 15000 + 300);
        createSpot("扎达土林", "རྩ་དར་ས་རི", "世界罕见的土林奇观。", "འཛམ་གླིང་གི་ས་རི་མཐོ་ཚད་ཆེས་མཐོ་བའི་གནས་ས།", ScenicSpot.Category.NATURAL, "0", "31.4833", "79.8000", spotImage("扎达土林.png"), Arrays.asList("地质奇观", "自然", "阿里"), 15000 + 200);
        createSpot("纳木那尼峰", "གནམ་མ་ནི་རི", "海拔7694米的圣母之山。", "མཐོ་ཚད་7694 མི་ཡིན་པའི་མ་ཡུམ་གྱི་རི་གནོན་པོ།", ScenicSpot.Category.NATURAL, "0", "30.4333", "81.3000", spotImage("纳木那尼峰.png"), Arrays.asList("雪山", "阿里", "自然"), 15000 + 100);
        createSpot("当惹雍错", "དང་རེ་གཡུ་མཚོ", "苯教崇拜的最大圣湖。", "བོན་པོའི་གནས་ས་རྙེད་པའི་མཚོ་ཆེན་པོ་གཙོ་བོ།", ScenicSpot.Category.NATURAL, "0", "31.0000", "86.6333", spotImage("当惹雍错.png"), Arrays.asList("湖泊", "苯教", "那曲"), 15000 + 50);
    }

    private void createSpot(String name, String desc, ScenicSpot.Category category, String price, String lat, String lng, String imgUrl, List<String> tags, int visitCount) {
        createSpot(name, null, desc, null, category, price, lat, lng, imgUrl, tags, visitCount);
    }
    
    private void createSpot(String name, String nameTibetan, String desc, String descTibetan, ScenicSpot.Category category, String price, String lat, String lng, String imgUrl, List<String> tags, int visitCount) {
        ScenicSpot spot = new ScenicSpot();
        spot.setName(name);
        spot.setDescription(enrichDescription(name, desc, category));
        if (nameTibetan != null && !nameTibetan.isEmpty()) {
            spot.setNameTibetan(nameTibetan);
        }
        if (descTibetan != null && !descTibetan.isEmpty()) {
            spot.setDescriptionTibetan(enrichDescriptionTibetan(nameTibetan != null ? nameTibetan : name, descTibetan, category));
        }
        spot.setCategory(category);
        spot.setTicketPrice(new BigDecimal(price));
        spot.setLatitude(new BigDecimal(lat));
        spot.setLongitude(new BigDecimal(lng));
        spot.setImageUrl(imgUrl);
        spot.setVisitCount(visitCount);  // 使用传入的点击量值（以15000为基准+差异）
        spot = spotRepository.save(spot);
        
        for (String tagName : tags) {
            createTag(spot, tagName);
        }
    }

    private void createTag(ScenicSpot spot, String tagName) {
        SpotTag tag = new SpotTag();
        tag.setSpot(spot);
        tag.setTag(tagName);
        tagRepository.save(tag);
    }

    private String spotImage(String filename) {
        return SPOT_IMAGE_BASE + filename;
    }

    private String enrichDescription(String name, String baseDesc, ScenicSpot.Category category) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseDesc).append(" ");
        switch (category) {
            case NATURAL:
                builder.append(name)
                        .append("以纯粹的自然气息著称，雪山、湖泊与草甸在同一视野里交织，云海与日照金山常在此上演，适合徒步、摄影与深度观景。 ");
                break;
            case CULTURAL:
                builder.append(name)
                        .append("承载着藏传文化的精神脉络，殿堂、壁画与法器层层铺陈，僧侣的梵音与转经的脚步在此交织，游客可静心感受信仰的力量。 ");
                break;
            case HISTORICAL:
            default:
                builder.append(name)
                        .append("诉说着横跨数百年的历史，遗址与建筑记录着王朝更迭与民间传说，石刻与壁画仍在讲述那段璀璨的高原文明。 ");
                break;
        }
        builder.append("清晨或黄昏是拍摄的黄金时刻，阳光在高原稀薄空气里折射出柔和色彩，连普通手机也能拍出电影感大片。 ");
        builder.append("景区周边设有补给点与观景平台，慢行路线让旅人能够在不急不躁的节奏里调匀呼吸，逐步适应海拔变化。 ");
        builder.append("建议提前预订门票或向导，并准备保暖衣物、防晒与能量补给，尊重当地生态与宗教礼仪，让这一段旅程更从容圆满。 ");

        String extra = "热爱探索的你也可以延伸行程，串联周边村落与小众景观点，在真实的日常里理解西藏的烟火气。 ";
        while (builder.length() < 220) {
            builder.append(extra);
        }
        return builder.toString();
    }
    
    private String enrichDescriptionTibetan(String name, String baseDesc, ScenicSpot.Category category) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseDesc).append(" ");
        switch (category) {
            case NATURAL:
                builder.append(name)
                        .append("རང་བྱུང་གི་རི་མོ་ལ་གཞི་བཅོལ་ནས། རི་གནོན་པོ་དང་མཚོ་དང་རྩྭ་ཐང་གཅིག་གི་ནང་དུ་འདྲེས་པ། སྤྲིན་རྒྱུན་དང་ཉི་མའི་འོད་རི་གནོན་པོ་རྒྱུན་མཐུན་དུ་འགྲོ་བ། སྤྱོད་པའི་ལམ་ཐོག་དང་རི་མོ་དང་གནས་ས་རྙེད་པ་ལ་མཐུན་པ། ");
                break;
            case CULTURAL:
                builder.append(name)
                        .append("བོད་ཀྱི་རིག་གནས་ཀྱི་སེམས་ཀྱི་རྒྱུན་མཐུན་དུ་འགྲོ་བ། གནས་ས་དང་རི་མོ་དང་ཆོས་ལུགས་ཀྱི་རྒྱུན་མཐུན་དུ་འགྲོ་བ། དགེ་སློང་གི་སྤྱོད་པའི་སྤྱོད་པ་དང་སྤྱོད་པའི་ལམ་ཐོག་གཅིག་གི་ནང་དུ་འདྲེས་པ། སྤྱོད་པ་རྣམས་སེམས་ཀྱི་ནང་དུ་ཆོས་ལུགས་ཀྱི་སྟོབས་རྙེད་པ་ལ་མཐུན་པ། ");
                break;
            case HISTORICAL:
            default:
                builder.append(name)
                        .append("ལོ་རྒྱུས་ཀྱི་རྒྱུན་མཐུན་དུ་འགྲོ་བ། གནས་ས་དང་རི་མོ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རི་མོ་དང་རི་མོ་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རི་མོ་དང་རི་མོ་རྒྱུན་མཐུན་དུ་འགྲོ་བ། ");
                break;
        }
        builder.append("ཞོགས་པ་དང་དགོངས་པ་ནི་རི་མོ་བཟོ་བའི་དུས་ཚོད་ཡིན་པ། ཉི་མའི་འོད་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རི་མོ་དང་རི་མོ་རྒྱུན་མཐུན་དུ་འགྲོ་བ། ");
        builder.append("གནས་ས་རྙེད་པའི་ཁོར་ཡུག་ནང་དུ་རྒྱུན་མཐུན་དུ་འགྲོ་བ་དང་གནས་ས་རྙེད་པ་ལ་མཐུན་པ། སྤྱོད་པའི་ལམ་ཐོག་གིས་སྤྱོད་པ་རྣམས་ལ་མཐུན་པའི་དུས་ཚོད་ནང་དུ་འགྲོ་བ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། ");
        builder.append("ཞོགས་པ་ནས་ཐོ་འགོད་དང་ལམ་ཐོག་བཟོ་བ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རྒྱུན་མཐུན་དུ་འགྲོ་བ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རྒྱུན་མཐུན་དུ་འགྲོ་བ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། ");

        String extra = "རྒྱུན་མཐུན་དུ་འགྲོ་བའི་སྤྱོད་པ་རྣམས་ལ་མཐུན་པའི་ལམ་ཐོག་བཟོ་བ་དང་རྒྱུན་མཐུན་དུ་འགྲོ་བ། རྒྱུན་མཐུན་དུ་འགྲོ་བའི་གནས་ས་རྙེད་པ་ལ་མཐུན་པ། ";
        while (builder.length() < 220) {
            builder.append(extra);
        }
        return builder.toString();
    }

    private void seedNews() {
        // 政策类资讯
        createNews("2024年冬游西藏优惠政策正式启动", 
            "为促进冬季旅游发展，西藏自治区推出'冬游西藏'优惠政策。即日起至2025年3月31日，全区所有A级景区（含布达拉宫、大昭寺、纳木错等）对游客实行免门票政策。同时，三星级以上酒店按淡季价格执行，旅游车辆通行费减半。此外，进藏航班和火车票也有相应优惠，让更多游客能够以更实惠的价格体验雪域高原的独特魅力。", 
            News.Category.POLICY, 
            "https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600&q=50");
        
        createNews("西藏旅游进藏证件办理指南更新", 
            "根据最新政策，除港澳台同胞和外国游客外，国内游客前往西藏大部分地区无需办理边防证。但前往阿里、日喀则部分边境地区（如珠峰大本营、冈仁波齐、古格王朝遗址等）仍需提前在户籍所在地或拉萨办理边防证。建议游客提前规划行程，如需前往边境地区，可在拉萨市公安局或各旅行社代办，一般1-2个工作日即可办理完成。", 
            News.Category.POLICY, 
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=600&q=50");
        
        createNews("拉萨至林芝高速全线通车，车程缩短至4小时", 
            "拉林高速公路（拉萨至林芝）已全线通车，这是西藏首条高速公路，全长409.2公里。通车后，从拉萨到林芝的车程从原来的8小时缩短至4小时，极大提升了游客的出行便利性。沿途可欣赏雅鲁藏布江大峡谷、巴松措等著名景点，为西藏旅游增添了新的交通选择。", 
            News.Category.POLICY, 
            "https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600&q=50");
        
        // 活动类资讯
        createNews("2024年林芝桃花节盛大开幕，花期持续至4月底", 
            "第21届林芝桃花节于3月20日正式开幕，本届桃花节以'桃花映雪域，春色满林芝'为主题。林芝地区拥有中国最大的野生桃林，每年3-4月，漫山遍野的桃花与雪山、峡谷、河流交相辉映，形成独特的春日画卷。主要观赏点包括嘎拉桃花村、波密桃花沟、雅鲁藏布江大峡谷等。活动期间还将举办藏式歌舞表演、桃花摄影大赛、特色美食节等丰富多彩的活动，预计花期将持续至4月底。", 
            News.Category.EVENT, 
            "https://images.unsplash.com/photo-1528164344705-47542687000d?w=600&q=50");
        
        createNews("雪顿节即将到来，拉萨将举办盛大藏戏展演", 
            "一年一度的雪顿节（藏历六月底七月初，公历约8月）即将到来，这是西藏最盛大的传统节日之一。节日期间，哲蚌寺、色拉寺将举行盛大的展佛仪式，数万名信众和游客将前往观礼。同时，罗布林卡将举办为期一周的藏戏展演，来自全区的藏戏团将表演《文成公主》《诺桑王子》等经典剧目。此外，还有赛马、射箭、歌舞表演等传统活动，是体验西藏文化的最佳时机。", 
            News.Category.EVENT, 
            "https://images.unsplash.com/photo-1514316454349-750a7fd3da3a?w=600&q=50");
        
        createNews("纳木错国际徒步大会报名启动", 
            "第三届纳木错国际徒步大会将于6月15日-20日举行，现已开始接受报名。活动路线围绕圣湖纳木错，全程约50公里，分为3天完成。参与者将穿越草原、湿地，近距离感受念青唐古拉山的壮美，体验高原徒步的独特魅力。活动还包含星空观测、摄影采风、藏式文化体验等环节。报名截止日期为5月31日，限报200人，额满即止。", 
            News.Category.EVENT, 
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=600&q=50");
        
        createNews("西藏文化旅游节7月开幕，展示非遗文化", 
            "2024年西藏文化旅游节将于7月10日在拉萨开幕，为期15天。本届文化节将集中展示西藏非物质文化遗产，包括唐卡绘制、藏香制作、藏毯编织等传统手工艺现场演示。同时还将举办藏医药文化展、藏式建筑艺术展、传统服饰秀等活动。文化节期间，游客可免费参观多个展览，并有机会参与互动体验，深入了解西藏深厚的文化底蕴。", 
            News.Category.EVENT, 
            "https://images.unsplash.com/photo-1578320662939-563635f76451?w=600&q=50");
        
        // 通知类资讯
        createNews("布达拉宫实行分时段预约参观，每日限流5000人", 
            "为保护文物和提升参观体验，布达拉宫自即日起实行分时段预约参观制度。游客需提前7天通过官方微信公众号或官网进行实名预约，每日限流5000人，分为上午场（9:00-12:00）和下午场（12:00-15:00）。参观时需携带身份证原件，建议提前30分钟到达，配合安检和防疫检查。淡季（11月-次年4月）门票100元，旺季（5月-10月）门票200元，学生、老人等可享受半价优惠。", 
            News.Category.NOTICE, 
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=600&q=50");
        
        createNews("珠峰大本营开放时间调整，注意高反防护", 
            "根据天气和路况，珠峰大本营（海拔5200米）开放时间为每年4月至10月。由于海拔较高，游客需特别注意高原反应防护。建议提前在拉萨适应2-3天，携带氧气瓶、红景天等抗高反药物，避免剧烈运动。大本营气温较低，需准备羽绒服、保暖帽等防寒装备。同时，为保护环境，大本营禁止使用一次性塑料制品，请游客自觉遵守环保规定。", 
            News.Category.NOTICE, 
            "https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600&q=50");
        
        createNews("西藏旅游安全提醒：雨季出行需谨慎", 
            "每年6-9月是西藏的雨季，降雨主要集中在夜间和清晨。雨季期间，部分山区道路可能出现塌方、泥石流等自然灾害，建议游客出行前关注天气预报和路况信息。前往阿里、那曲等偏远地区时，建议选择有经验的旅行社或向导，配备卫星电话等应急设备。同时，雨季也是西藏最美的季节之一，草原绿意盎然，野花盛开，只要做好充分准备，依然可以安全愉快地游览。", 
            News.Category.NOTICE, 
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=600&q=50");
        
        createNews("大昭寺朝圣高峰期，建议错峰参观", 
            "每年藏历正月、四月、六月是藏传佛教的重要节日，大昭寺将迎来朝圣高峰期，每日参观人数可达上万人次。为获得更好的参观体验，建议游客避开这些时段，选择平日或淡季前往。参观时需顺时针方向绕行，尊重当地宗教习俗，禁止在殿内拍照。大昭寺开放时间为9:00-18:00，门票85元，建议提前了解藏传佛教基本知识，以便更好地理解这座千年古寺的文化内涵。", 
            News.Category.NOTICE, 
            "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=600&q=50");
        
        createNews("西藏旅游旺季到来，酒店预订需提前", 
            "随着旅游旺季（5月-10月）的到来，拉萨、林芝等热门旅游城市的酒店预订量大幅增加，特别是布达拉宫、大昭寺周边的酒店更是一房难求。建议计划前往西藏的游客提前1-2个月预订酒店，尤其是7-8月的暑假期间。同时，旺季期间酒店价格会有一定上涨，建议提前做好预算规划。可选择通过官方平台或信誉良好的旅行社进行预订，避免临时找不到住宿的情况。", 
            News.Category.NOTICE, 
            "https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600&q=50");
    }

    private void createNews(String title, String content, News.Category category, String imgUrl) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setCategory(category);
        news.setImageUrl(imgUrl);
        news.setViewCount(100 + (int)(Math.random() * 1000));
        newsRepository.save(news);
    }

    private void seedHeritage() {
        createHeritage("藏戏", "藏族戏剧，以面具表演为特色。", "表演艺术", "https://images.unsplash.com/photo-1514316454349-750a7fd3da3a?w=600&q=50");
        createHeritage("唐卡", "藏族绘画艺术，色彩鲜明。", "传统美术", "https://images.unsplash.com/photo-1578320662939-563635f76451?w=600&q=50");
    }

    private void createHeritage(String name, String desc, String category, String imgUrl) {
        HeritageItem item = new HeritageItem();
        item.setName(name);
        item.setDescription(desc);
        item.setCategory(category);
        item.setImageUrl(imgUrl);
        heritageRepository.save(item);
    }

    private void seedHistory() {
        User user1 = userRepository.findByUsername("user1").get();
        List<ScenicSpot> spots = spotRepository.findAll();
        
        if (!spots.isEmpty()) {
            createHistory(user1, spots.get(0), 5, 6, 240);
            createHistory(user1, spots.get(1), 4, 3, 120);
            
            User user2 = userRepository.findByUsername("user2").get();
            createHistory(user2, spots.get(2), 5, 8, 360);
            createHistory(user2, spots.get(3), 5, 2, 90);
        }
    }

    private void createHistory(User user, ScenicSpot spot, Integer rating, Integer clickCount, Integer dwellSeconds) {
        UserVisitHistory history = new UserVisitHistory();
        history.setUser(user);
        history.setSpot(spot);
        history.setRating(rating);
        history.setClickCount(clickCount);
        history.setDwellSeconds(dwellSeconds);
        historyRepository.save(history);
    }
}
