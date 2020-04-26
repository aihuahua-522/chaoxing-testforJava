import bean.ClassBean;
import bean.PicBean;
import bean.SignBean;
import bean.scanningBean;
import com.google.gson.Gson;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class testLoginMain {

    private static final ArrayList<PicBean> picBeans = new ArrayList<>();
    private static final Pattern pattern = Pattern.compile("\\d{5,}");
    private static final Pattern pattern2 = Pattern.compile("我的排名：\\d{1,2}");
    private static final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
    static String loginUrl = "http://passport2.chaoxing.com/cloudscanlogin?mobiletip=%e7%94%b5%e8%84%91%e7%ab%af%e7%99%bb%e5%bd%95%e7%a1%ae%e8%ae%a4&pcrefer=http://i.chaoxing.com";
    static HashMap<String, String> temp = new HashMap<>();
    private static String name = "";
    private static String qq = "";
    private static String sendMessage = "";
    private static HashMap<String, String> cookiesMap;
    private static boolean isAnswer;


    public static void saveMap(Map<String, String> cookiesMap) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("a.json"));
        outputStream.writeObject(cookiesMap);
    }


    public static void deleteMap() {
        File file = new File("a.json");
        if (file.exists()) {
            boolean delete = file.delete();
            if (delete) {
                System.out.println("退出登录成功");
            } else {
                System.out.println("请手动删除a.json文件");
            }
        }
    }

    public static Object getMap() throws IOException, ClassNotFoundException {
        File file = new File("a.json");
        if (file.exists()) {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("a.json"));
            return inputStream.readObject();
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            initSetting();
            // 1. 检查是否登录
            cookiesMap = (HashMap<String, String>) getMap();
            if (cookiesMap == null) {
                //2.未登录
                System.out.println("未登录");
                //判断用户选择扫码登录还是账号密码登录
                if ("".equals(temp.get("username")) || temp.get("password").equals("")) {
                    System.out.println("你选择的是扫码登录");
                    GetQr getQr = new GetQr().invoke();
                    Map<String, String> cookies = getQr.getCookies();
                    String uuid = getQr.getUuid();
                    String enc = getQr.getEnc();
                    // 保存二维码到本地
                    saveQr(uuid);
                    // 扫描二维码状态
                    startScanning(cookies, uuid, enc);
                } else {
                    System.out.println("你选择的是账号密码登录");
                    String loginUrl = "http://i.chaoxing.com/vlogin?passWord=" + temp.get("password") + "&userName=" + temp.get("username");
                    Connection.Response response = Jsoup.connect(loginUrl).method(Connection.Method.GET).timeout(5000).execute();
                    String s = response.parse().body().toString();
                    if (s.contains("true")) {
                        cookiesMap = (HashMap<String, String>) response.cookies();
                        saveMap(cookiesMap);
                        System.out.println("登录成功");
                    } else {
                        System.out.println(s);
                        System.exit(0);
                    }
                }
            }
            // 到这里一定登录成功（bug除外）
            // 初始化图片
            System.out.println("初始化图片");
            new picinit(cookiesMap).initPic(picBeans);
            // 4. 获取班级信息
            ArrayList<ClassBean> classBeans = getClassBeans();
            name = URLEncoder.encode(getName(), "utf-8");
            // 5. 开始签到
            int signTime = Integer.parseInt(temp.get("signTime"));
            String signPlace = temp.get("signPlace");
            qq = temp.get("qq");
            sendMessage = temp.get("sendMessage");
            System.out.println("等待10s上传拍照图片");
            service.scheduleWithFixedDelay(new signRunning(URLEncoder.encode(signPlace, "utf-8"), classBeans, picBeans), 10, signTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initSetting() throws IOException {
        Properties properties = new Properties();
        File file = new File("signInfo.properties");
        if (file.exists()) {
            properties.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            System.out.println("你的设置扫描时间为" + properties.getProperty("signTime").replaceAll("\"", ""));
            System.out.println("你的定位签到地点为" + properties.getProperty("signPlace").replaceAll("\"", ""));
            System.out.println("你的开始时间是" + properties.getProperty("startTime").replaceAll("\"", ""));
            System.out.println("你的结束时间是" + properties.getProperty("endTime").replaceAll("\"", ""));
            System.out.println("你的通知qq号是" + properties.getProperty("qq").replaceAll("\"", ""));
            System.out.println("你的发送消息url是" + properties.getProperty("sendMessage").replaceAll("\"", ""));
            System.out.println("是否抢答" + properties.getProperty("isAnswer").replaceAll("\"", ""));
            temp.put("signTime", properties.getProperty("signTime").replaceAll("\"", ""));
            temp.put("signPlace", properties.getProperty("signPlace").replaceAll("\"", ""));
            temp.put("username", properties.getProperty("username").replaceAll("\"", ""));
            temp.put("password", properties.getProperty("password").replaceAll("\"", ""));
            temp.put("startTime", properties.getProperty("startTime").replaceAll("\"", ""));
            temp.put("endTime", properties.getProperty("endTime").replaceAll("\"", ""));
            temp.put("qq", properties.getProperty("qq").replaceAll("\"", ""));
            temp.put("sendMessage", properties.getProperty("sendMessage").replaceAll("\"", ""));
            isAnswer = Integer.parseInt(properties.getProperty("isAnswer").replaceAll("\"", "")) == 1;
            return;
        }
        throw new RuntimeException("配置文件被删除，请重新解压");
    }

    private static String getName() throws Exception {
        String getName = "http://i.chaoxing.com/base";
        name = Jsoup.connect(getName).cookies(cookiesMap).timeout(30000).get().select(".user-name").text();
        System.out.println("name = " + name);
        return name;
    }

    private static void startScanning(Map<String, String> cookies, String uuid, String enc) throws IOException {
        // 3. 检测二维码扫描状态
        AtomicBoolean isScanning = new AtomicBoolean(false);
        String url = "http://passport2.chaoxing.com/getauthstatus";
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("enc", enc);
        Runnable runnable = () -> {
            isScanning.set(false);
            Connection.Response scanningResponse = null;
            try {
                scanningResponse = Jsoup.connect(url).cookies(cookies).data(map).timeout(30000).method(Connection.Method.POST).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            cookiesMap = (HashMap<String, String>) Objects.requireNonNull(scanningResponse).cookies();
            Gson gson = new Gson();
            scanningBean scanningBean = gson.fromJson(Objects.requireNonNull(scanningResponse).body(), scanningBean.class);
            if (scanningBean.isStatus()) {
                isScanning.set(true);
            } else {
                int type = Integer.parseInt(scanningBean.getType());
                System.out.println("type = " + type);
                if (type == 4) {
                    System.out.println("扫描成功,等待登录");
                } else if (type == 2) {
                    System.out.println("二维码失效，请重新打开");
                    isScanning.set(true);
                } else {
                    System.out.println("等待扫描");
                }
            }
        };

        while (!isScanning.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runnable.run();
        }
        saveMap(cookiesMap);
    }

    private static ArrayList<ClassBean> getClassBeans() throws Exception {

        // 4. 登录成功-->将课程封装list
        ArrayList<ClassBean> ClassBeans = new ArrayList<>();
        String getClassUrl = "http://mooc1-2.chaoxing.com/visit/courses";
        Document classDocument = Jsoup.connect(getClassUrl).cookies(cookiesMap).timeout(30000).get();
        if (classDocument.title().contains("用户登录")) {
            deleteMap();
            System.out.println("请重新登录");
            main(null);
        }
        Elements classElements = classDocument.select(".ulDiv > ul > li[style]");
        for (Element classElement : classElements) {
            ClassBean classBean = new ClassBean();
            String courseId = classElement.select("[name = courseId]").attr("value");
            String classId = classElement.select("[name = classId]").attr("value");
            //课程名
            String className = classElement.select(".clearfix > a ").attr("title");
            //班级名
            String classmate = classElement.select(".Mconright > p ").get(2).attr("title");
            //教师
            String teacher = classElement.select(".Mconright > p ").get(0).attr("title");
            //任务地址
            String url = "https://mobilelearn.chaoxing.com/widget/pcpick/stu/index?courseId=" + courseId + "&jclassId=" + classId;
            classBean.setCourseId(courseId);
            classBean.setClassId(classId);
            classBean.setClassName(className);
            classBean.setTeacher(teacher);
            classBean.setUrl(url);
            classBean.setClassmate(classmate);
            ClassBeans.add(classBean);
        }
        return ClassBeans;
    }

    private static void saveQr(String uuid) throws IOException {
        // 2. 保存二维码
        String scanningUrl = "http://passport2.chaoxing.com/createqr?uuid=" + uuid + "&xxtrefer=&type=1&mobiletip=%E7%94%B5%E8%84%91%E7%AB%AF%E7%99%BB%E5%BD%95%E7%A1%AE%E8%AE%A4";
        Connection.Response saveImageResponse = Jsoup.connect(scanningUrl).timeout(30000).execute();
        FileOutputStream outputStream = new FileOutputStream("1.png", false);
        outputStream.write(saveImageResponse.bodyAsBytes());
        outputStream.flush();
        outputStream.close();
//        File file = new File("1.png");
//        // 运行cmd命令执行程序
//        Runtime runtime = Runtime.getRuntime();
//        runtime.exec("cmd /c " + file.getAbsolutePath());
    }


    private static class GetQr {


        private Map<String, String> cookies;
        private String uuid;
        private String enc;

        public Map<String, String> getCookies() {
            return cookies;
        }

        public String getUuid() {
            return uuid;
        }

        public String getEnc() {
            return enc;
        }

        public GetQr invoke() throws IOException {
            Connection.Response response = Jsoup.connect(loginUrl).method(Connection.Method.GET).timeout(30000).execute();
            cookies = response.cookies();
            System.out.println("cookies = " + cookies);
            Document document = response.parse();
            uuid = document.select("input[ id =uuid]").attr("value");
            enc = document.select("input[ id =enc]").attr("value");
            return this;
        }
    }

    static class signRunning implements Runnable {

        String signPlace;
        ArrayList<ClassBean> classBeans;
        ArrayList<PicBean> picBeans;

        public signRunning(String signPlace, ArrayList<ClassBean> classBeans, ArrayList<PicBean> picBeans) {
            this.classBeans = classBeans;
            this.picBeans = picBeans;
            this.signPlace = signPlace;
        }

        @Override
        public void run() {
            try {
                System.out.println(DateUtil.getTime() + "正在运行");
                beginExecution(qq, cookiesMap, classBeans, name, picBeans, signPlace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void beginExecution(String email, HashMap<String, String> cookie, ArrayList<ClassBean> courseBeans, String name, ArrayList<PicBean> picBeans, String signPlace) throws Exception {
            for (ClassBean courseBean : courseBeans) {
                String signUrl = courseBean.getUrl();
                Connection.Response response = Jsoup.connect(signUrl).cookies(cookie).timeout(300000).execute();
                Document document = response.parse();
                if (!document.title().contains("学生端-活动首页")) {
                    main(null);
                    // TODO 刷新cookie
                }
                Elements elements = document.select("#startList > div> div");
                judgeType(email, cookie, name, picBeans, courseBean, elements, signPlace);
            }
        }


        private void judgeType(String email, HashMap<String, String> cookie, String name, ArrayList<PicBean> picBeans, ClassBean courseBean, Elements elements, String signPlace) throws Exception {
            for (Element element : elements) {
                String state = element.select("div > dl > a > dd ").text();
                String s = element.attr("onclick");
                Matcher matcher = pattern.matcher(s);
                String activeId = matcher.find() ? matcher.group() : "";
                String signTime = element.selectFirst(".Color_Orang").text();
                if (signTime == null || signTime.isEmpty()) {
                    signTime = "嘤嘤嘤.手动看他不香吗";
                }
                String signText = element.selectFirst(".Mct_center a[shape]").text();
                String signT = element.selectFirst(" a[shape]").text();
                if (temp.get(activeId) != null) {
                    continue;
                }
                switch (state) {
                    case "签到":
                        signByActiveId(email, signTime, signText, courseBean, cookie, name, picBeans, signPlace, activeId);
                        break;
                    case "问卷":
                        answerByActiveId(email, signTime, signText, courseBean, activeId);
                        break;
                    case "抢答":
                        answerQuickly(email, signTime, signText, courseBean, activeId);
                        break;
                    case "测验":
                        testVerification(email, courseBean, signText, signTime, activeId);
                        break;
                    case "评分":
                        score(email, courseBean, signText, signTime, activeId);
                        break;
                    default:
                        String message = "课程名称 -> " + "\t" + courseBean.getClassName() + "\n" +
                                "活动类型 -> " + "\t" + signT + "\n" +
                                "班级名称 -> " + "\t" + courseBean.getClassmate() + "\n" +
                                "活动标题 -> " + "\t" + signText + "\n" +
                                "剩余时间 -> " + signTime;
                        temp.put(activeId, activeId);
                        EmailUtil.sendMail(sendMessage, email, message);
                        break;
                }
            }
        }

        private void score(String email, ClassBean courseBean, String signText, String signTime, String activeId) {

            // TODO 发送邮件 有评分
            String message = "课程名称 -> " + "\t" + courseBean.getClassName() + "\n" +
                    "班级名称 -> " + "\t" + courseBean.getClassmate() + "\n" +
                    "评分标题 -> " + "\t" + signText + "\n" +
                    "剩余时间 -> " + signTime;
            try {
                EmailUtil.sendMail(sendMessage, email, message);
                temp.put(activeId, activeId);
            } catch (Exception e) {
                temp.remove(activeId);
                e.printStackTrace();
            }

        }

        private void testVerification(String email, ClassBean courseBean, String signText, String signTime, String activeId) {

            // TODO  发送邮件 有测验
            String message = "课程名称 -> " + "\t" + courseBean.getClassName() + "\n" +
                    "班级名称 -> " + "\t" + courseBean.getClassmate() + "\n" +
                    "测验标题 -> " + "\t" + signText + "\n" +
                    "剩余时间 -> " + "\t" + signTime;
            try {
                EmailUtil.sendMail(sendMessage, email, message);
                temp.put(activeId, activeId);
            } catch (Exception e) {
                e.printStackTrace();
                temp.remove(activeId);
            }
        }

        private void answerQuickly(String email, String signTime, String signText, ClassBean courseBean, String activeId) throws Exception {
            // TODO 发送邮件 有抢答
            if (isAnswer) {
                System.out.println(Boolean.parseBoolean(temp.get("answer")));
                String answer = "https://mobilelearn.chaoxing.com/pptAnswer/stuAnswer?answerId="
                        + activeId
                        + "&classId=" + courseBean.getClassId()
                        + "&courseId=" + courseBean.getCourseId()
                        + "&stuName=" + name
                        + "&role="
                        + "appType=15&stuMiddlePage=1";
                System.out.println(answer);
                System.out.println("==============" + activeId + "抢答中=================");
                HttpUtil.trustEveryone();
                Connection.Response signResponse = Jsoup.connect(answer).cookies(cookiesMap).method(Connection.Method.GET).timeout(30000).execute();
                Element element = signResponse.parse().body();
                temp.put(activeId, "抢答成功");
                Matcher matcher = pattern2.matcher(element.html());
                if (matcher.find()) {
                    System.out.println(courseBean.getClassName() + "抢答成功" + matcher.group());
                    String message = "课程名称-> " + courseBean.getClassName() + "\n" +
                            "班级名称 -> " + courseBean.getClassmate() + "\n" +
                            "抢答标题 -> " + signText + "\n" +
                            "抢答状态" + matcher.group() + "\n" +
                            "剩余时间 -> " + signTime;
                    try {
                        EmailUtil.sendMail(sendMessage, email, message);
                        temp.put(activeId, activeId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        temp.remove(activeId);
                    }
                }
            } else {
                String message = "课程名称-> " + courseBean.getClassName() + "\n" +
                        "班级名称 -> " + courseBean.getClassmate() + "\n" +
                        "抢答标题 -> " + signText + "\n" +
                        "抢答状态" + "未开启抢答" + "\n" +
                        "剩余时间 -> " + signTime;
                try {
                    EmailUtil.sendMail(sendMessage, email, message);
                    temp.put(activeId, activeId);
                } catch (Exception e) {
                    e.printStackTrace();
                    temp.remove(activeId);
                }
            }
        }


        private void answerByActiveId(String email, String signTime, String signText, ClassBean courseBean, String activeId) throws Exception {
            // TODO 发送邮件 有问卷
            String message = "课程名称-> " + courseBean.getClassName() + "\n" +
                    "班级名称 -> " + courseBean.getClassmate() + "\n" +
                    "问卷标题 -> " + signText + "\n" +
                    "剩余时间 -> " + signTime;
            try {
                EmailUtil.sendMail(sendMessage, email, message);
                temp.put(activeId, activeId);
            } catch (Exception e) {
                e.printStackTrace();
                temp.remove(activeId);
            }
        }

        private void signByActiveId(String email, String signTime, String signText, ClassBean courseBean, HashMap<String, String> cookie, String name, ArrayList<PicBean> picBeans, String signPlace, String activeId) throws Exception {
            // TODO 发送邮件  开始签到
            String finalSignUrl = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?name="
                    + name
                    + "&address="
                    + signPlace
                    + "&activeId="
                    + activeId
                    + "&uid="
                    + cookie.get("_uid")
                    + "&clientip=&latitude=-1&longitude=-1&fid="
                    + cookie.get("fid")
                    + "&appType=15&ifTiJiao=1"
                    + "&objectId=" + picBeans.get(new Random().nextInt(picBeans.size())).getObjectId();
            try {
                Document signBody = Jsoup.connect(finalSignUrl).timeout(300000).cookies(cookie).execute().parse();
                String signState = signBody.getElementsByTag("body").text();
                if ("您已签到过了".equals(signState)) {
                    temp.put(activeId, activeId);
                } else {
                    String message = "========这是签到=========" + "\n" +
                            "班级名称-> " + courseBean.getClassmate() + "\n" +
                            "课程名称 -> " + courseBean.getClassName() + "\n" +
                            "签到类型 -> " + signText + "\n" +
                            "签到状态 -> " + signState + "\n" +
                            "剩余时间 -> " + signTime + "\n" +
                            "=========签到结束========";
                    EmailUtil.sendMail(sendMessage, email, message);
                }
            } catch (Exception e) {
                EmailUtil.sendMail(sendMessage, email, courseBean.getClassmate() + "可能签到失败(服务器会自动重试),建议手动看看");
                e.printStackTrace();
                temp.remove(activeId);
            }
        }

    }

}