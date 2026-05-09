package com.tibet.tourism;

import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
    private BookingRepository bookingRepository;
    @Autowired
    private HotelBookingRepository hotelBookingRepository;

    @Autowired
    private CarouselRepository carouselRepository;

    @Autowired
    private TravelRouteRepository travelRouteRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

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
        
        // 只在数据库为空时才播种景点和资讯数据，避免破坏已有数据
        if (spotRepository.count() == 0) {
            System.out.println("数据库为空，开始播种景点数据...");
            seedSpots();
            if (newsRepository.count() == 0) {
                seedNews();
            }
            if (heritageRepository.count() == 0) {
                seedHeritage();
            }
        }
        if (historyRepository.count() == 0) {
            seedHistory();
        }
        if (carouselRepository.count() == 0) {
            seedCarousels();
        }
        if (travelRouteRepository.count() == 0) {
            seedRoutes();
        }
        if (hotelRepository.count() < 19) {
            seedHotels();
        }
    }

    private void seedUsers() {
        // 创建或更新 admin 用户
        userRepository.findByUsername("admin").ifPresentOrElse(
            existing -> {
                // 如果密码未加密（长度小于20，BCrypt hash通常更长），则更新
                if (existing.getPassword().length() < 20 || !passwordEncoder.matches("admin123", existing.getPassword())) {
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
                // 强制验证：密码长度<20 或 BCrypt验证不通过，都强制重设
                if (existing.getPassword().length() < 20 || !passwordEncoder.matches("031224", existing.getPassword())) {
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
                if (existing.getPassword().length() < 20 || !passwordEncoder.matches("123456", existing.getPassword())) {
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
                if (existing.getPassword().length() < 20 || !passwordEncoder.matches("123456", existing.getPassword())) {
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

        // 更多精选景点（扩展热力图覆盖范围）
        createSpot("普莫雍错", "ཕུ་མོ་གཡུ་མཚོ", "海拔最高的淡水湖之一，冰蓝梦境。", "མཐོ་ཚད་ཆེས་མཐོ་བའི་ཆུ་མཚོ་གཙོ་བོ། དར་ཡུལ་གྱི་རྨི་ལམ་ལྟ་བུ།", ScenicSpot.Category.NATURAL, "0", "28.5667", "90.4167", spotImage("普莫雍错.jpg"), Arrays.asList("湖泊", "自然", "山南"), 15000 + 350);
        createSpot("班公错", "སྤང་གོང་མཚོ", "中印边境的国际湖泊，鸟类的天堂。", "རྒྱ་དང་ཧིན་རྫིའི་མཚོ་ཆེན་པོ། བྱེའུ་རིགས་ཀྱི་གནས་ས།", ScenicSpot.Category.NATURAL, "30", "33.7333", "79.4667", spotImage("班公错.jpg"), Arrays.asList("湖泊", "边境", "阿里"), 15000 + 300);
        createSpot("色林错", "གཟི་ལིང་མཚོ", "西藏面积最大的湖泊。", "བོད་ཀྱི་མཚོ་ཆེན་པོ་གཙོ་བོ།", ScenicSpot.Category.NATURAL, "0", "31.8333", "88.7333", spotImage("色林错.jpg"), Arrays.asList("湖泊", "自然", "那曲"), 15000 + 250);
        createSpot("佩枯措", "པད་གུ་མཚོ", "珠峰保护区内的蓝宝石湖泊。", "ཇོ་མོ་གླང་མའི་སྲུང་སྐྱོབ་ས་ཁུལ་གྱི་མཚོ་མོ།", ScenicSpot.Category.NATURAL, "0", "28.8167", "85.5833", spotImage("佩枯措.jpg"), Arrays.asList("湖泊", "珠峰", "日喀则"), 15000 + 200);

        createSpot("江孜宗山古堡", "རྒྱལ་རྩེ་རྫོང", "抗英遗址，英雄之城的地标。", "དམག་འཁྲུག་གི་གནས་ས་དང་དཔའ་བའི་གྲོང་གི་རྫོང་།", ScenicSpot.Category.HISTORICAL, "30", "28.9167", "89.6000", spotImage("江孜宗山古堡.jpg"), Arrays.asList("古堡", "历史", "日喀则"), 15000 + 450);
        createSpot("白居寺", "དཔལ་འཁོར་ཆོས་སྡེ", "塔中有寺，寺中有塔的奇观。", "མཆོད་རྟེན་ནང་དུ་དགོན་པ་དང་དགོན་པའི་ནང་དུ་མཆོད་རྟེན་གྱི་གནས་ས།", ScenicSpot.Category.CULTURAL, "45", "28.9167", "89.6000", spotImage("白居寺.jpg"), Arrays.asList("寺庙", "佛塔", "日喀则"), 15000 + 400);
        createSpot("绒布寺", "རོང་བུ་དགོན་པ", "世界海拔最高的寺庙。", "འཛམ་གླིང་གི་མཐོ་ཚད་ཆེས་མཐོ་བའི་དགོན་པ།", ScenicSpot.Category.CULTURAL, "35", "28.2000", "86.8333", spotImage("绒布寺.jpg"), Arrays.asList("寺庙", "珠峰", "日喀则"), 15000 + 350);

        createSpot("盐井古盐田", "ཚྭ་ཁྲོན་ཚྭ་ཞིང", "千年盐田，茶马古道上的活化石。", "ལོ་སྟོང་གི་ཚྭ་ཞིང་། ཇ་ལམ་གྱི་གསོན་པོའི་གནས་ས།", ScenicSpot.Category.HISTORICAL, "50", "29.0500", "98.6000", spotImage("盐井古盐田.jpg"), Arrays.asList("盐田", "历史", "昌都"), 15000 + 300);
        createSpot("孜珠寺", "རྩི་འབྲུ་དགོན་པ", "悬崖上的苯教圣地，海拔4800米。", "གཡང་གཞུང་གི་བོན་པོའི་གནས་ས། མཐོ་ཚད་4800 མི་ཡིན།", ScenicSpot.Category.CULTURAL, "30", "31.0833", "96.7000", spotImage("孜珠寺.jpg"), Arrays.asList("寺庙", "苯教", "昌都"), 15000 + 250);
        createSpot("强巴林寺", "བྱམས་པ་གླིང་དགོན་པ", "昌都最大的格鲁派寺院。", "ཆབ་མདོའི་དགོན་པ་ཆེན་པོ་དང་དགེ་ལུགས་པའི་དགོན་པ།", ScenicSpot.Category.CULTURAL, "0", "31.1500", "97.1833", spotImage("强巴林寺.jpg"), Arrays.asList("寺庙", "佛教", "昌都"), 15000 + 200);

        createSpot("勒布沟", "ལེབ་བུ་ལུང་པ", "山南的亚热带秘境，门巴族故乡。", "ལྷོ་ཁའི་ཚ་བའི་གནས་ས་དང་མོན་པའི་གནས་ས།", ScenicSpot.Category.NATURAL, "0", "27.8500", "91.8333", spotImage("勒布沟.jpg"), Arrays.asList("峡谷", "森林", "山南"), 15000 + 200);
        createSpot("40冰川", "40 གངས་རི", "中不边境的蓝冰世界。", "རྒྱ་དང་འབྲུག་གི་གངས་རི། སྔོན་པོའི་གངས་རི།", ScenicSpot.Category.NATURAL, "0", "28.0000", "89.9500", spotImage("40冰川.jpg"), Arrays.asList("冰川", "自然", "山南"), 15000 + 150);

        createSpot("狮泉河镇", "སེང་གཅུག་ཁ", "阿里地区的中心城镇。", "མངའ་རིས་ས་ཁུལ་གྱི་གྲོང་གཙོ།", ScenicSpot.Category.CULTURAL, "0", "32.5000", "80.1000", spotImage("狮泉河镇.jpg"), Arrays.asList("城镇", "阿里", "边境"), 15000 + 150);
        createSpot("托林寺", "མཐོ་གླིང་དགོན་པ", "阿里古格王朝的皇家寺院。", "མངའ་རིས་གུ་གེའི་རྒྱལ་དགོན།", ScenicSpot.Category.HISTORICAL, "45", "31.1333", "79.9167", spotImage("托林寺.jpg"), Arrays.asList("寺庙", "历史", "阿里"), 15000 + 100);

        createSpot("波密桃花沟", "སྤོ་མེས་ཤིང་ཏོག་ལུང་པ", "中国最长的桃花沟。", "རྒྱ་ནག་གི་ཤིང་ཏོག་ལུང་པ་ཆེས་རིང་བ།", ScenicSpot.Category.NATURAL, "0", "29.8667", "95.7667", spotImage("波密桃花沟.jpg"), Arrays.asList("桃花", "自然", "林芝"), 15000 + 250);
        createSpot("希夏邦马峰", "ཞི་ཞ་སྤང་མ", "唯一完全在中国境内的8000米级山峰。", "རྒྱ་ནག་གི་ནང་དུ་ཆ་ཚང་དུ་གནས་པའི་8000 མི་ཡིན་པའི་རི་གནོན་པོ།", ScenicSpot.Category.NATURAL, "0", "28.3500", "85.7833", spotImage("希夏邦马峰.jpg"), Arrays.asList("雪山", "自然", "日喀则"), 15000 + 150);

        createSpot("易贡国家地质公园", "ཡིད་འོང་ས་རི་སྤྱི་གླིང", "世界最大规模的山体崩塌遗迹。", "འཛམ་གླིང་གི་ས་རི་ལྷུང་བའི་གནས་ས་ཆེས་ཆེ་བ།", ScenicSpot.Category.NATURAL, "40", "30.2667", "94.8167", spotImage("易贡国家地质公园.jpg"), Arrays.asList("地质", "自然", "林芝"), 15000 + 100);
        createSpot("比如骷髅墙", "འབྲི་རུ་ཐོད་པའི་རྩིག་པ", "藏北神秘的天葬台文化景观。", "བྱང་ཐང་གི་གསང་བའི་དུར་ཁྲོད་ཀྱི་རིག་གནས།", ScenicSpot.Category.CULTURAL, "50", "31.4833", "93.5667", spotImage("比如骷髅墙.jpg"), Arrays.asList("天葬", "神秘", "那曲"), 15000 + 50);
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
        // ========== 民间文学类 ==========
        createHeritage("格萨尔史诗",
                "被誉为「世界上最长的史诗」，通过艺人口耳相传、即兴说唱的方式一代代流传下来，是藏族民间文学的巅峰之作。",
                "民间文学",
                "/heritage/格萨尔史诗.jpg",
                "https://baike.baidu.com/item/%E6%A0%BC%E8%90%A8%E5%B0%94%E7%8E%8B%E4%BC%A0",
                "格萨尔王传起源于公元11世纪前后，以藏族英雄格萨尔南征北战、降妖伏魔、统一各部、造福百姓的传奇故事为主线。史诗融合了藏族神话、传说、谚语、歌谣等口头传统，由「仲肯」（说唱艺人）在篝火旁、帐篷中代代传唱。部分艺人自称「神授」，声称在梦中得到神灵传授，醒来后便能完整说唱数十万诗行。",
                "格萨尔史诗不仅是一部英雄史诗，更是藏族古代社会的百科全书，涵盖历史、宗教、民俗、军事、医学等方方面面。2009年入选联合国教科文组织人类非物质文化遗产代表作名录，被国际学术界誉为「东方的荷马史诗」。");

        // ========== 传统音乐类 ==========
        createHeritage("拉萨囊玛",
                "西藏古典音乐的代表形式，融合了藏族传统音乐与内地音乐元素，以悠扬的旋律和典雅的歌词著称，被誉为「西藏的古典音乐」。",
                "传统音乐",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E6%8B%89%E8%90%A8%E5%9B%8A%E7%8E%9B",
                "囊玛（ནང་མ）起源于17世纪的拉萨，最初在布达拉宫和贵族府邸中演出，是上层社会的室内音乐。18世纪后逐渐流传至民间，吸收了堆谐等民间音乐元素，形成了以扎念琴、扬琴、笛子等乐器伴奏的经典组合。传统的囊玛表演通常由慢板（囊玛）和快板（堆谐）两部分组成。",
                "囊玛是藏汉文化交流融合的艺术结晶，其歌词多取材于藏族古典诗歌，内容涉及爱情、自然、宗教等主题，旋律优美含蓄，具有极高的艺术价值。2008年列入国家级非物质文化遗产名录，是研究藏族音乐史和藏汉文化交流的重要活态资料。");

        createHeritage("那曲山歌",
                "藏北草原牧民在放牧和劳作中即兴演唱的传统民歌，旋律辽阔奔放，歌词贴近生活，是草原文化的音乐符号。",
                "传统音乐",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%82%A3%E6%9B%B2%E5%B1%B1%E6%AD%8C",
                "那曲山歌（拉伊）流传于藏北那曲广大牧区，是牧民在放牧、迁徙、聚会等场合中自发演唱的民歌形式。其旋律高亢悠远，音域宽广，常用真假声交替的技巧，模仿风过草原、鸟鸣长空的天籁之音。歌词多为即兴创作，内容涵盖爱情、劳作、自然风光、人生哲理等。",
                "那曲山歌是藏北游牧文化的音乐化表达，承载着牧民与高原自然环境的情感连接和生存智慧。2006年列入第一批国家级非物质文化遗产名录，是研究藏族音乐多样性和游牧文化的重要样本。");

        // ========== 传统舞蹈类 ==========
        createHeritage("热巴舞",
                "源自昌都丁青县的综合性表演艺术，融合歌舞、杂技、说唱于一体，以铃鼓舞为核心，历史可追溯至公元11世纪。",
                "传统舞蹈",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E7%83%AD%E5%B7%B4%E8%88%9E",
                "热巴舞的起源与藏传佛教噶举派大师米拉日巴（1040-1123）密切相关。相传米拉日巴的弟子热穹巴将佛教教义融入民间歌舞，创编了这种以铃鼓为主要道具的表演形式。热巴艺人（热巴瓦）走村串寨，以家族或师徒形式传承，表演时男子持铜铃、女子持扁鼓，边击边舞，穿插杂技、谐剧等即兴表演。",
                "热巴舞集藏族民间艺术的精华于一身，既是宗教艺术的世俗化表达，也是流浪艺人生存智慧的结晶。2008年列入国家级非物质文化遗产名录，被誉为「藏族民间歌舞的活化石」。其豪放热烈的风格在国内外舞台上广受欢迎。");

        createHeritage("锅庄舞",
                "藏族最普及的集体舞蹈形式，人们围圈踏歌而舞，广泛流行于昌都、那曲、甘孜、迪庆等藏区，不同地区各有流派。",
                "传统舞蹈",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%94%85%E5%BA%84%E8%88%9E",
                "锅庄（果卓）意为「圆圈歌舞」，起源可追溯至原始社会的火塘祭祀活动。古时人们围火而舞，踏地为节，抒发丰收喜悦或祈求神灵庇佑。随着历史演变，锅庄从宗教仪式中分离出来，成为节庆、婚礼、迎宾等场合必不可少的群众性歌舞活动。不同地区的锅庄风格迥异——昌都锅庄雄浑刚健，那曲锅庄粗犷豪迈。",
                "锅庄舞是藏族社会凝聚力和文化认同的重要载体，它打破了年龄、阶层和地域的界限，让所有人在共同的节奏中找到归属感。2006年列入第一批国家级非物质文化遗产名录，如今不仅在藏区盛行，更成为全国广场舞的热门舞种之一。");

        createHeritage("弦子舞",
                "以弦乐（毕旺琴）伴奏的集体舞蹈，流行于芒康、巴塘等地，舞姿舒展优美，弦音悠扬婉转，被誉为「康区的华尔兹」。",
                "传统舞蹈",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E5%BC%A6%E5%AD%90%E8%88%9E",
                "弦子（谐）舞起源于康巴地区的农耕和游牧生活，以藏族传统拉弦乐器「毕旺」（类似二胡）为主要伴奏乐器。舞时男女列队，由领舞者拉奏毕旺在前引导，众人随乐起舞，步伐以三步一抬、踏地为节为基本特征。歌词多用比兴手法，内容涉及爱情、家乡、自然和宗教。",
                "弦子舞以其优雅舒展的风格和深情的旋律成为康巴文化的代表符号。2008年列入国家级非物质文化遗产名录。弦子舞不仅在藏区广为流传，也多次走出国门，在国际舞台上展现藏族艺术的独特魅力。");

        // ========== 传统戏剧类 ==========
        createHeritage("藏戏",
                "被誉为「藏文化的活化石」，集歌舞、说唱、表演于一体，以独特的面具艺术和程式化表演为特色，常在寺院法会和民间节日中演出。",
                "传统戏剧",
                "/heritage/藏戏.jpg",
                "https://baike.baidu.com/item/%E8%97%8F%E6%88%8F",
                "藏戏（阿吉拉姆）起源于14世纪，由藏传佛教噶举派高僧唐东杰布为募资建造铁索桥而创立。他组织七姐妹以歌舞说唱形式四处演出，逐渐发展为一门综合性的舞台艺术。藏戏表演以面具区分角色身份和性格——白面具代表善良正直，红面具代表威严勇猛，黑面具代表邪恶奸诈。经典剧目包括《文成公主》《诺桑王子》《卓娃桑姆》等八大传统藏戏。",
                "藏戏综合了宗教仪式、历史故事和民间传说，是研究藏族社会生活与信仰体系的重要窗口。2006年列入第一批国家级非物质文化遗产名录，2009年入选联合国教科文组织人类非物质文化遗产代表作名录。每年雪顿节期间的罗布林卡藏戏展演，已成为西藏最具影响力的文化盛事之一。");

        createHeritage("门巴戏",
                "山南地区门巴族的传统戏剧，以独特的舞蹈动作和叙事方式讲述门巴族的历史传说，保留了诸多原始戏剧元素。",
                "传统戏剧",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%97%A8%E5%B7%B4%E6%88%8F",
                "门巴戏（门巴阿吉拉姆）流传于山南市错那县勒布区门巴族聚居地。它的产生与佛教的传入和藏戏的影响密切相关，同时保留了门巴族原始的祭祀舞蹈和民间说唱元素。门巴戏的表演不用面具，而以面部化妆和肢体语言塑造角色，以鼓、钹为主要伴奏乐器。代表剧目有《诺桑王子》《阿拉卡教》等。",
                "门巴戏是人口较少的门巴族传承自身文化记忆的独特方式，承载着门巴族的迁徙历史、宗教信仰和审美观念。2006年列入第一批国家级非物质文化遗产名录，对于保护民族文化的多样性具有不可替代的意义。");

        // ========== 传统体育类 ==========
        createHeritage("藏族传统马术",
                "集骑马技艺、传统体育和民俗文化于一体的综合性活动，每逢赛马节，藏区各地的骑手云集，展示精湛的马上技艺。",
                "传统体育·游艺与杂技",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E6%97%8F%E9%A9%AC%E6%9C%AF",
                "藏族传统马术起源于古代高原游牧生活和军事需要。在吐蕃王朝时期，骑兵是军队主力，马术训练成为男子必修课。随着历史发展，军事性的马术逐渐转化为民间体育活动，形成了赛马、马术表演、马上射箭、马上拾哈达等丰富多彩的项目。藏北那曲赛马节、理塘赛马节等是集中展示传统马术的重要平台。",
                "藏族传统马术体现了藏族人民与马的深厚情感和高超的驾驭能力，是游牧文明的活态传承。2011年列入国家级非物质文化遗产名录。如今，传统马术与现代马术运动相互借鉴，在各类文化节庆中继续绽放光彩。");

        // ========== 传统技艺类 ==========
        createHeritage("藏族唐卡",
                "以矿物颜料在布、纸或丝绸上绘制的宗教卷轴画，色彩瑰丽、构图严谨、线条精细，多悬挂于寺院与居室，是西藏艺术的代表符号。",
                "传统技艺",
                "/heritage/唐卡.jpg",
                "https://baike.baidu.com/item/%E5%94%90%E5%8D%A1",
                "唐卡（ཐང་ཀ）的历史可追溯至吐蕃时期，公元7世纪佛教传入西藏后，唐卡作为便于携带的宗教圣物逐渐兴起。绘制唐卡需经过选布、绷框、打底、起稿、着色、勾线、开脸、装裱等多道工序，颜料均取自天然矿物和植物，如青金石、朱砂、金箔等，可数百年不褪色。勉唐画派、钦泽画派、噶玛嘎孜画派是唐卡三大流派，各具风格。",
                "唐卡承载着藏传佛教教义、历史人物与宇宙观，被称为「可以卷起来带走的宫殿壁画」，是西藏艺术最具辨识度的视觉符号。2006年列入第一批国家级非物质文化遗产名录。近年来，唐卡艺术在保持传统的同时不断创新，尺幅从手掌大小到百米巨幅不等，题材也从宗教扩展到历史、民俗等更广阔的领域。");

        createHeritage("藏香制作技艺",
                "以柏木、檀香、藏红花、麝香等数十种天然药材和香料为原料，按照传统配方手工制作，是藏族礼佛供佛和日常养生的重要用品。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E9%A6%99",
                "藏香（བོད་སྤོས）的历史可以追溯到吐蕃时期。公元7世纪，吞弥·桑布扎从印度带回制香技术，结合西藏本地的草药和香料，创制了最早的藏香配方。藏香以柏木粉为基底，配以甘松、檀香、丁香、豆蔻、藏红花、麝香等三十余味天然材料，经研磨、和合、发酵、成型、阴干等数十道工序手工制成。敏珠林寺、尼木县吞巴乡是藏香制作的两大核心传承地。",
                "藏香不仅是一种宗教用品，更承载着藏族传统医学「香疗」的养生理念，其配方借鉴了藏医药学理论，具有安神静气、净化空气、驱虫防病的功效。2008年列入国家级非物质文化遗产名录，如今藏香已成为西藏最具代表性的文化伴手礼之一。");

        createHeritage("藏刀锻制技艺",
                "西藏传统刀具制作工艺，以拉孜藏刀和谢通门藏刀最为著名，刀身锻造精良，刀鞘装饰繁复，集实用与艺术于一体。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E5%88%80",
                "藏刀的锻造历史可追溯至吐蕃时期，最初是游牧民族生产和自卫的必备工具。传统藏刀以钢和铁为原料，经过反复折叠锻打、淬火、回火等工序，使刀身兼具硬度和韧性。刀鞘和刀柄以铜、银等金属包覆，雕刻有龙、狮子、祥云、八吉祥等精美图案，部分还镶嵌珊瑚、松石等宝石。拉孜藏刀以刀形修长、锋利耐用著称，谢通门藏刀则以装饰华丽闻名。",
                "藏刀不仅是藏族人民的日常生活用品，更是身份地位和审美情趣的象征，体现了藏族金属锻造技艺的巅峰水平。2008年列入国家级非物质文化遗产名录。如今，藏刀作为工艺收藏品广受游客喜爱，传统锻造技艺在保护中创新发展。");

        createHeritage("藏族邦典/卡垫织造技艺",
                "邦典（围裙）和卡垫（地毯）是西藏最具代表性的传统纺织品，以色彩浓烈、图案丰富、质地厚实而著称。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%82%A6%E5%85%B8",
                "邦典和卡垫的织造历史悠久，在吐蕃时期的文献中已有记载。邦典以氆氇为原料，通过传统木质织机手工织造，色彩以彩虹条纹为标志性图案，是藏族妇女的传统服饰配件。卡垫则以羊毛为原料，经纺线、染色、编织、剪花、平剪等多道工序制成，图案多取材于宗教符号、花卉和几何纹样。日喀则江孜县是卡垫织造的核心传承地。",
                "邦典和卡垫织造技艺反映了藏族人民在高原环境下利用有限资源创造美好生活的能力和智慧。2006年列入第一批国家级非物质文化遗产名录。江孜卡垫以其精湛的工艺享誉海内外，被誉为「西藏地毯中的极品」。");

        createHeritage("藏族雕版印刷技艺",
                "以日喀则纳塘寺为代表的传统雕版印刷技艺，曾印制了举世闻名的纳塘版《大藏经》，是藏文典籍得以流传的技术基础。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E6%97%8F%E9%9B%95%E7%89%88%E5%8D%B0%E5%88%B7%E6%8A%80%E8%89%BA",
                "藏族的雕版印刷技术在公元13世纪传入西藏，在纳塘寺、德格印经院、布达拉宫印经院等地得到极大发展。雕版选用质地细密的桦木或檀木，经多年阴干后由雕刻工匠用特制刀具将藏文正楷字体反刻于木板上。印刷时以手工刷墨、覆纸、压印，完全依靠人工完成。纳塘寺在1730年代刻印的纳塘版《大藏经》共4569块经版，是藏文雕版印刷史上的里程碑。",
                "藏族雕版印刷技艺为保存和传播藏传佛教经典、历史文献、医药典籍和文学著作做出了不可磨灭的贡献。2008年列入国家级非物质文化遗产名录。今天，虽然现代印刷技术已普及，但传统雕版印刷作为一项珍贵的手工技艺和文化遗产仍在传承。");

        createHeritage("藏医药浴法",
                "将全身或局部浸泡于天然温泉或药物熬制的汤液中，利用水的热能和药物作用防治疾病，是藏医最具特色的外治疗法之一。",
                "传统医药",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E5%8C%BB%E8%8D%AF%E6%B5%B4%E6%B3%95",
                "藏医药浴法（泷沐）有1300多年的历史，早在公元8世纪的藏医经典《四部医典》中就有系统论述。其理论基础是藏医学的「五源」（土、水、火、风、空）和「三因」（隆、赤巴、培根）学说，认为通过药浴可以调和体内三因平衡。经典药浴方剂「五味甘露」以杜鹃叶、麻黄、圆柏枝、水柏枝、野蒿为主药。在西藏，天然温泉药浴尤为盛行，羊八井、沃卡、德仲等温泉地是著名的浴疗胜地。",
                "藏医药浴法体现了藏族人民与高原自然环境和谐共生的生存智慧，是藏医学「治未病」理念的实践典范。2018年列入联合国教科文组织人类非物质文化遗产代表作名录，标志着这一古老疗法得到国际社会的广泛认可。");

        createHeritage("藏族造纸技艺",
                "以瑞香科植物狼毒草等为原料手工制作的传统藏纸，质地坚韧、防虫防蛀，千年不蠹，是藏文典籍得以长久保存的物质基础。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E7%BA%B8",
                "藏纸的制作历史可追溯至公元7世纪吐蕃王朝时期。传统藏纸以高原特有的狼毒草（热如）根茎为主要原料，经采摘、浸泡、蒸煮、捣浆、抄纸、晾干等数十道工序纯手工制成。由于狼毒草含有微毒成分，藏纸具有天然的抗虫蛀和耐腐蚀特性，可保存千年而不坏。尼木县、金东乡等地是藏纸的传统产地。",
                "藏纸为浩瀚的藏文大藏经、历史文献、文学作品的书写和保存提供了不可或缺的载体，是藏族文明延续的物质见证。2006年列入第一批国家级非物质文化遗产名录。如今，非遗传承人和文创企业在保护传统工艺的基础上，开发出笔记本、灯笼、书签等藏纸衍生品，让古老技艺融入现代生活。");

        // ========== 民俗类 ==========
        createHeritage("雪顿节",
                "西藏最盛大的传统节日之一，以展佛仪式和藏戏汇演为核心内容，藏历六月底七月初（公历约8月）举行，意为「酸奶宴」。",
                "民俗",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%9B%AA%E9%A1%BF%E8%8A%82",
                "雪顿节（ཞོ་སྟོན）起源于公元11世纪，最初是佛教僧侣在夏季闭关修行结束后，接受信徒供奉酸奶的宗教活动。17世纪后，五世达赖喇嘛将藏戏表演引入雪顿节，使其从单纯的宗教活动演变为全民性的文化盛会。节日期间，哲蚌寺和色拉寺举行庄严的展佛仪式，巨幅唐卡从山顶铺展而下，数万信众和游客前往朝拜观礼。罗布林卡内连续上演藏戏和歌舞，全城沉浸在欢乐的节日氛围中。",
                "雪顿节集宗教仪式、戏剧演出、民俗活动和集市贸易于一体，是西藏文化认同和民族团结的重要纽带。2006年列入第一批国家级非物质文化遗产名录。如今雪顿节不仅吸引海内外游客，更成为展示西藏文化传承与发展的标志性窗口。");

        createHeritage("望果节",
                "藏族农民庆祝丰收的传统节日，以巡游田间、赛马、射箭、歌舞等为主要活动，广泛流行于西藏农区，是农耕文明的节庆标志。",
                "民俗",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E6%9C%9B%E6%9E%9C%E8%8A%82",
                "望果节（འོང་སྐོར）的历史可追溯至吐蕃时期的原始苯教时期，最初是人们在庄稼成熟前举行的祈求丰收的祭祀仪式。「望」意为田地，「果」意为转圈巡游。节日当天，村民身着盛装，抬着佛像、手持经幡和青稞穗，绕田间巡游一周，随后举行赛马、射箭、摔跤、藏戏表演和集体歌舞等活动，最后全村人共享丰盛的宴席。",
                "望果节是藏族农耕文化的百科全书，展现了高原民族顺应自然、感恩大地的朴素生态伦理。2014年列入国家级非物质文化遗产名录。在现代化进程中，望果节依然保持着旺盛的生命力，是连接传统农耕文明与当代乡村文化的重要纽带。");

        createHeritage("藏族金属锻造技艺",
                "包括锻铜、金银加工、佛像铸造等多种金属工艺，是藏族传统手工艺的重要组成部分，广泛应用于宗教法器、建筑装饰和日常生活用品。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E8%97%8F%E6%97%8F%E9%87%91%E5%B1%9E%E9%94%BB%E9%80%A0%E6%8A%80%E8%89%BA",
                "藏族金属锻造技艺历史悠久，吐蕃时期已达到很高水平。传统工艺以锻铜和金银加工为主，产品包括佛像、法轮、供水碗等宗教法器，以及银碗、铜壶、首饰等生活用品。制作过程中，匠人运用锤揲、錾刻、鎏金、错金等多种技法，在金属表面雕刻出精致的图案纹样。日喀则扎西吉彩、南木林县等地是金属锻造技艺的重要传承地。",
                "藏族金属锻造技艺融合了印度、尼泊尔和中原汉地的工艺传统，形成了独具特色的西藏金属工艺体系。2008年列入国家级非物质文化遗产名录。其产品不仅是宗教活动和日常生活的必需品，更是藏族审美观念和工艺智慧的实物载体。");

        createHeritage("墨脱石锅制作技艺",
                "林芝市墨脱县门巴族的传统厨具制作技艺，用当地特有的皂石手工凿制而成，石锅烹煮的食物味道鲜美，营养丰富。",
                "传统技艺",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E5%A2%A8%E8%84%B1%E7%9F%B3%E9%94%85",
                "墨脱石锅是门巴族先民在雅鲁藏布江大峡谷中就地取材的智慧创造。制作石锅选用墨脱特有的天然皂石（云母石），这种石头质地柔软易凿、耐高温、导热均匀。制作时先将石料从山体上凿取下来，再用铁锤和凿子纯手工敲打成型，一口石锅从选料到完成需要数天时间。石锅在使用过程中会慢慢吸收食物的油脂和香味，越用越温润，越煮越鲜美。",
                "墨脱石锅是门巴族人与自然和谐相处的生动写照，其制作和使用体现了原生态的饮食文化和生存智慧。2014年列入国家级非物质文化遗产名录，墨脱石锅也成为西藏著名的地理标志产品，深受美食爱好者和文化收藏者的青睐。");

        createHeritage("羌姆",
                "藏传佛教寺院中举行的法舞仪式，僧侣头戴精美面具、身着华丽服饰，以庄严的舞蹈演绎佛教教义和护法神话。",
                "传统舞蹈",
                "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=600&q=50",
                "https://baike.baidu.com/item/%E9%87%91%E5%88%9A%E6%B3%95%E8%88%9E",
                "羌姆（金刚法舞）起源于公元8世纪，由莲花生大师在建造桑耶寺时首次引入。当时为降服阻碍建寺的妖魔，莲花生大师跳起了象征降魔的金刚法舞。此后，羌姆成为藏传佛教各教派寺院法会中的重要仪式。表演时，僧侣头戴造型夸张的立体面具，身着锦缎法衣，按照严格的仪轨，合着法号、铙钹的节奏跳出规定动作，以舞蹈语言传达密宗教义。",
                "羌姆是宗教、舞蹈、音乐、面具艺术的综合体，是藏传佛教密宗修行的艺术化体现。它将深奥的佛教哲理转化为可视可感的舞蹈语言，让普通信众也能直观地感受佛法的力量。2006年作为「日喀则扎什伦布寺羌姆」被列入第一批国家级非物质文化遗产扩展项目名录。");

        System.out.println("已播种 " + heritageRepository.count() + " 项非遗文化数据");
    }

    private void createHeritage(String name, String desc, String category, String imgUrl,
                                String baikeUrl, String originStory, String significance) {
        HeritageItem item = new HeritageItem();
        item.setName(name);
        item.setDescription(desc);
        item.setCategory(category);
        item.setImageUrl(imgUrl);
        item.setBaikeUrl(baikeUrl);
        item.setOriginStory(originStory);
        item.setSignificance(significance);
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

    private void seedCarousels() {
        createCarousel("探索神秘西藏", "雪域高原 · 心灵之旅", "热门推荐", "/heritage/布达拉宫3.jpg", "/spots", 0);
        createCarousel("圣湖纳木错", "天湖之美 · 洗涤心灵", "自然奇观", "/heritage/纳木错.jpg", "/spots", 1);
        createCarousel("藏戏非遗文化", "千年传承 · 文化瑰宝", "非遗文化", "/heritage/藏戏.jpg", "/heritage", 2);
        createCarousel("雪顿节盛典", "展佛法会 · 藏戏汇演", "民俗节庆", "https://images.unsplash.com/photo-1559827291-baf8ef4d3285?w=1200&q=70", "/news", 3);
    }

    private void createCarousel(String title, String subtitle, String tag, String imageUrl, String linkUrl, int sortOrder) {
        Carousel carousel = new Carousel();
        carousel.setTitle(title);
        carousel.setSubtitle(subtitle);
        carousel.setTag(tag);
        carousel.setImageUrl(imageUrl);
        carousel.setLinkUrl(linkUrl);
        carousel.setSortOrder(sortOrder);
        carousel.setActive(true);
        carouselRepository.save(carousel);
    }

    private void seedRoutes() {
        createRoute("拉萨经典三日游", "ལྷ་སའི་གསུམ་ཉིན་ལམ་ཐོག",
            "从布达拉宫到大昭寺，深度体验圣城拉萨的文化魅力。第一天参观布达拉宫和罗布林卡，第二天游览大昭寺和八廓街，第三天前往色拉寺和哲蚌寺。",
            "ལྷ་སའི་གནས་ས་རྙེད་པའི་ལམ་ཐོག ཉིན་དང་པོར་པོ་ཏ་ལ་དང་ནོར་བུ་གླིང་ཁ། ཉིན་གཉིས་པར་ཇོ་ཁང་དང་བར་སྐོར་ཁྲོམ་གཞུང་། ཉིན་གསུམ་པར་སེར་ར་དང་འབྲས་སྤུངས།",
            3, "899", TravelRoute.Difficulty.EASY, "15°C - 25°C", "高原河谷地带，拉萨平原",
            "[1,2,5,8,9]");

        createRoute("林芝桃花深度四日游", "ཉིང་ཁྲིའི་ཤིང་ཏོག་ལམ་ཐོག",
            "每年3-4月，林芝桃花盛开，漫山遍野如诗如画。途经鲁朗林海、雅鲁藏布大峡谷、南迦巴瓦峰、波密桃花沟等精华景点。",
            "ལོ་རེའི་ཟླ་༣-༤ པར་ཉིང་ཁྲིའི་ཤིང་ཏོག་རྒྱས་པ་དང་། རི་རྒྱུད་ཀུན་ཏུ་ཤིང་ཏོག་གི་མཚོན་རྟགས་ཡོད།",
            4, "1280", TravelRoute.Difficulty.EASY, "10°C - 22°C", "藏东南峡谷森林地带",
            "[6,7,11,12]");

        createRoute("珠峰大本营探险五日游", "ཇོ་མོ་གླང་མའི་ལམ་ཐོག",
            "挑战世界之巅，从日喀则出发，途经扎什伦布寺、萨迦寺，最终抵达珠峰大本营，近距离感受世界最高峰的震撼。",
            "འཛམ་གླིང་གི་མཐོ་ཚད་ཆེས་མཐོ་བའི་གནས་ས་རྙེད་པ།",
            5, "2580", TravelRoute.Difficulty.HARD, "-5°C - 15°C", "高海拔山地，珠峰保护区",
            "[10,14,17,21]");

        createRoute("阿里转山朝圣七日游", "མངའ་རིས་གནས་སྐོར་ལམ་ཐོག",
            "深入西藏西部阿里地区，朝拜神山冈仁波齐和圣湖玛旁雍措，探访古格王国遗址和扎达土林奇观。",
            "བོད་ཀྱི་ནུབ་ཕྱོགས་མངའ་རིས་ས་ཁུལ་དུ་འགྲོ་བ།",
            7, "3980", TravelRoute.Difficulty.HARD, "5°C - 20°C", "高海拔荒漠草原地带",
            "[20,21,24,25]");

        createRoute("山南文化探索三日游", "ལྷོ་ཁའི་རིག་གནས་ལམ་ཐོག",
            "探访西藏文明的发源地——山南。游览西藏第一座宫殿雍布拉康、第一座寺庙桑耶寺，感受藏源文化的深厚底蕴。",
            "བོད་ཀྱི་རིག་གནས་ཀྱི་འབྱུང་ཁུངས་ལྷོ་ཁར་འཚོལ་ཞིབ།",
            3, "680", TravelRoute.Difficulty.MEDIUM, "12°C - 24°C", "河谷平原，雅鲁藏布江中游",
            "[14,15,18]");
    }

    private void createRoute(String name, String nameTibetan, String description, String descriptionTibetan,
                             int days, String price, TravelRoute.Difficulty difficulty,
                             String temperature, String geography, String spotsJson) {
        TravelRoute route = new TravelRoute();
        route.setName(name);
        route.setNameTibetan(nameTibetan);
        route.setDescription(description);
        route.setDescriptionTibetan(descriptionTibetan);
        route.setDays(days);
        route.setPrice(new BigDecimal(price));
        route.setDifficulty(difficulty);
        route.setTemperature(temperature);
        route.setGeography(geography);
        route.setSpotsJson(spotsJson);
        travelRouteRepository.save(route);
    }

    private void seedHotels() {
        // ==================== 拉萨 (4家) ====================
        Hotel h1 = createHotel("拉萨瑞吉度假酒店", "拉萨市城关区江苏路22号", "0891-6808888",
            "1280-4000", "/images/hotels/hotel-lhasa-ruiji.jpg",
            "WiFi, 停车场, 泳池, SPA, 藏式餐厅, 酒吧, 供氧系统", "4.9");
        createRoomType(h1, "豪华大床房", "1280", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h1, "经典双床房", "1380", 2, "2张单床 · 可住2人 · 含早餐", 1);
        createRoomType(h1, "布宫景观套房", "2280", 3, "1大床 · 可观布达拉宫 · 含行政礼遇", 2);

        Hotel h2 = createHotel("拉萨香格里拉大酒店", "拉萨市城关区罗布林卡路19号", "0891-6558888",
            "980-2000", "/images/hotels/hotel-lhasa-xianggelila.jpg",
            "WiFi, 停车场, 餐厅, 健身房, 氧气吧, 商务中心", "4.8");
        createRoomType(h2, "豪华大床房", "980", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h2, "豪华双床房", "1080", 2, "2张单床 · 可住2人 · 含早餐", 1);
        createRoomType(h2, "行政套房", "1880", 3, "更大空间 · 含行政酒廊礼遇", 2);

        Hotel h3 = createHotel("拉萨凡莲酒店", "拉萨市城关区江苏东路10号", "0891-6677888",
            "680-1280", "/images/hotels/hotel-lhasa-interior.jpg",
            "WiFi, 停车场, 供氧, 管家服务, 疗愈中心", "4.9");
        createRoomType(h3, "藏韵大床房", "680", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h3, "景观双床房", "780", 2, "2张单床 · 可住2人 · 远眺布宫", 1);
        createRoomType(h3, "疗愈套房", "1280", 3, "更大空间 · 含颂钵体验", 2);

        Hotel h4 = createHotel("拉萨城际酒店", "拉萨市堆龙德庆区柳梧街道顿珠金融城", "0891-6699000",
            "350-680", "/images/hotels/hotel-luxury-1.jpg",
            "WiFi, 停车场, 供氧, 健身房", "4.8");
        createRoomType(h4, "商务大床房", "350", 2, "1张大床 · 可住2人 · 丝涟床品", 0);
        createRoomType(h4, "商务双床房", "420", 2, "2张单床 · 可住2人", 1);
        createRoomType(h4, "高级套房", "680", 3, "更大空间 · 含会客区", 2);

        // ==================== 林芝 (5家) ====================
        Hotel h5 = createHotel("林芝工布庄园希尔顿酒店", "林芝市巴宜区尼洋河畔", "0894-5886666",
            "480-1500", "/images/hotels/hotel-nyingchi-hilton.jpg",
            "WiFi, 停车场, 藏式餐厅, 花园, 会议室", "4.5");
        createRoomType(h5, "标准双床房", "480", 2, "35㎡, 河谷景观", 0);
        createRoomType(h5, "景观大床房", "680", 2, "45㎡, 尼洋河全景", 1);

        Hotel h6 = createHotel("美豪丽致酒店(林芝工布印象店)", "林芝市巴宜区八一镇纺织新街30号", "0894-5822888",
            "220-420", "/images/hotels/hotel-nyingchi-hilton.jpg",
            "WiFi, 停车场, 供氧, 智能客控", "4.9");
        createRoomType(h6, "雪山景观大床房", "220", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h6, "尼洋河景双床房", "260", 2, "2张单床 · 可住2人", 1);
        createRoomType(h6, "豪华家庭套房", "420", 4, "适合家庭 · 更大空间", 2);

        Hotel h7 = createHotel("林芝悦皇冠度假酒店", "林芝市巴宜区八一镇309号", "0894-5833999",
            "880-1680", "/images/hotels/hotel-luxury-1.jpg",
            "WiFi, 停车场, 泳池, 星空房", "4.8");
        createRoomType(h7, "河景大床房", "880", 2, "1张大床 · 可住2人 · 尼洋河景", 0);
        createRoomType(h7, "山景双床房", "980", 2, "2张单床 · 可住2人 · 比日神山景", 1);
        createRoomType(h7, "星空套房", "1680", 2, "360°全透明穹顶 · 含早餐", 2);

        Hotel h8 = createHotel("林芝保利雅途酒店", "林芝市鲁朗国际旅游小镇北区", "0894-5893666",
            "780-1380", "/images/hotels/hotel-lhasa-ruiji.jpg",
            "WiFi, 停车场, 观景台, 藏餐厅", "4.7");
        createRoomType(h8, "湖景大床别墅", "780", 2, "独栋 · 1大床 · 扎塘鲁措湖景", 0);
        createRoomType(h8, "雪山双床别墅", "880", 2, "独栋 · 2单床 · 雪山景观", 1);
        createRoomType(h8, "豪华家庭别墅", "1380", 4, "独栋 · 适合4人 · 含庭院", 2);

        Hotel h9 = createHotel("艺龙安悦酒店(尼洋河景区店)", "林芝市巴宜区尼洋河景区附近", "0894-5733888",
            "140-260", "/images/hotels/hotel-naqu-caoyuan.jpg",
            "WiFi, 停车场, 洗衣, 机器人服务", "4.8");
        createRoomType(h9, "标准大床房", "140", 2, "1张大床 · 可住2人", 0);
        createRoomType(h9, "舒适双床房", "180", 2, "2张单床 · 可住2人", 1);
        createRoomType(h9, "家庭三人房", "260", 3, "1大床+1单床 · 可住3人", 2);

        // ==================== 日喀则 (5家) ====================
        Hotel h10 = createHotel("日喀则乔穆朗宗酒店", "日喀则市桑珠孜区上海中路", "0892-8838888",
            "680-1280", "/images/hotels/hotel-shigatse-qiaomu.jpg",
            "WiFi, 停车场, 藏式餐厅, 供氧服务", "4.8");
        createRoomType(h10, "藏韵大床房", "680", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h10, "藏式双床房", "780", 2, "2张单床 · 可住2人", 1);
        createRoomType(h10, "扎寺景观套房", "1280", 3, "可观扎什伦布寺 · 含行政礼遇", 2);

        Hotel h11 = createHotel("日喀则藏域雅布酒店", "日喀则市桑珠孜区黑龙江南路与亚瓦路交叉口", "0892-8911888",
            "420-680", "/images/hotels/hotel-lhasa-xianggelila.jpg",
            "WiFi, 停车场, 供氧, 智能客控, 充电桩", "4.7");
        createRoomType(h11, "雪山景观大床房", "420", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h11, "青稞田景双床房", "480", 2, "2张单床 · 可住2人", 1);
        createRoomType(h11, "豪华套房", "680", 3, "更大空间 · 含独立会客区", 2);

        Hotel h12 = createHotel("日喀则天临大饭店", "日喀则市桑珠孜区珠峰路75号", "0892-8855777",
            "380-580", "/images/hotels/hotel-lhasa-interior.jpg",
            "WiFi, 停车场, 供氧, 地暖", "4.7");
        createRoomType(h12, "富氧大床房", "380", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h12, "富氧双床房", "430", 2, "2张单床 · 可住2人", 1);
        createRoomType(h12, "扎寺观景房", "580", 2, "可观扎寺 · 含早餐", 2);

        Hotel h13 = createHotel("岗巴悦美供氧酒店", "日喀则市岗巴县中华大道03号", "0892-8233666",
            "260-380", "/images/hotels/hotel-ngari-wenquan.jpg",
            "WiFi, 停车场, 供氧, 电热毯", "4.6");
        createRoomType(h13, "雪山观景大床房", "260", 2, "1张大床 · 可观干城章嘉峰", 0);
        createRoomType(h13, "舒适双床房", "300", 2, "2张单床 · 可住2人", 1);
        createRoomType(h13, "家庭房", "380", 3, "适合家庭入住", 2);

        // ==================== 阿里 (5家) ====================
        Hotel h14 = createHotel("阿里大峡谷温泉酒店", "阿里地区普兰县塔尔钦镇", "0897-2666888",
            "320-480", "/images/hotels/hotel-ngari-wenquan.jpg",
            "WiFi, 停车场, 温泉, 藏式餐厅", "4.2");
        createRoomType(h14, "标准双床房", "320", 2, "25㎡, 温泉景观", 0);
        createRoomType(h14, "舒适大床房", "480", 2, "35㎡, 雪山景观", 1);

        Hotel h15 = createHotel("喜玛拉雅·冈仁波齐酒店", "阿里地区普兰县塔尔钦镇", "0897-2603666",
            "560-980", "/images/hotels/hotel-ngari-wenquan.jpg",
            "WiFi, 停车场, 供氧, 餐厅", "4.6");
        createRoomType(h15, "神山观景大床房", "560", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h15, "朝圣双床房", "620", 2, "2张单床 · 可住2人", 1);
        createRoomType(h15, "神山套房", "980", 3, "更大空间 · 可观冈仁波齐", 2);

        Hotel h16 = createHotel("鹏润大酒店", "阿里地区噶尔县狮泉河镇北京路南段4号", "0897-2822888",
            "380-680", "/images/hotels/hotel-luxury-1.jpg",
            "WiFi, 停车场, 供氧", "4.5");
        createRoomType(h16, "商务大床房", "380", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h16, "商务双床房", "430", 2, "2张单床 · 可住2人", 1);
        createRoomType(h16, "行政套房", "680", 3, "更大空间 · 含会客区", 2);

        Hotel h17 = createHotel("阿里岷山大酒店", "阿里地区噶尔县文化路东段", "0897-2833555",
            "320-580", "/images/hotels/hotel-lhasa-interior.jpg",
            "WiFi, 停车场, 供氧", "4.3");
        createRoomType(h17, "标准大床房", "320", 2, "1张大床 · 可住2人 · 含早餐", 0);
        createRoomType(h17, "标准双床房", "380", 2, "2张单床 · 可住2人", 1);
        createRoomType(h17, "高级套房", "580", 3, "更大空间 · 含早餐", 2);

        Hotel h18 = createHotel("札达土林城堡酒店", "阿里地区札达县团结路北段", "0897-2903888",
            "280-460", "/images/hotels/hotel-naqu-caoyuan.jpg",
            "WiFi, 停车场, 藏式庭院", "4.5");
        createRoomType(h18, "藏式大床房", "280", 2, "1张大床 · 可住2人", 0);
        createRoomType(h18, "藏式双床房", "320", 2, "2张单床 · 可住2人", 1);
        createRoomType(h18, "土林景观房", "460", 2, "可观札达土林 · 含早餐", 2);

        // ==================== 那曲 (1家) ====================
        Hotel h19 = createHotel("那曲草原驿站酒店", "那曲市色尼区文化路", "0896-3822888",
            "220-380", "/images/hotels/hotel-naqu-caoyuan.jpg",
            "WiFi, 停车场, 餐厅, 供氧服务, 骑马体验", "4.0");
        createRoomType(h19, "经济双床房", "220", 2, "20㎡, 基础设施", 0);
        createRoomType(h19, "草原观景房", "380", 2, "30㎡, 草原全景窗", 1);
    }

    private Hotel createHotel(String name, String location, String phone, String priceRange,
                               String imageUrl, String facilities, String rating) {
        return hotelRepository.findByName(name).orElseGet(() -> {
            Hotel hotel = new Hotel();
            hotel.setName(name);
            hotel.setLocation(location);
            hotel.setPhone(phone);
            hotel.setPriceRange(priceRange);
            hotel.setImageUrl(imageUrl);
            hotel.setFacilities(facilities);
            hotel.setRating(new BigDecimal(rating));
            return hotelRepository.save(hotel);
        });
    }

    private void createRoomType(Hotel hotel, String name, String price, int capacity, String amenities, int sortOrder) {
        // Check if this room type already exists for this hotel
        List<RoomType> existing = roomTypeRepository.findByHotelId(hotel.getId());
        boolean exists = existing.stream().anyMatch(r -> r.getName().equals(name));
        if (exists) return;

        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setPrice(new BigDecimal(price));
        roomType.setCapacity(capacity);
        roomType.setAmenities(amenities);
        roomType.setSortOrder(sortOrder);
        roomTypeRepository.save(roomType);
    }
}
