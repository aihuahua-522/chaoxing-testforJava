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
import java.util.concurrent.atomic.AtomicBoolean;

public class testLoginMain {

    static String loginUrl = "http://passport2.chaoxing.com/cloudscanlogin?mobiletip=%e7%94%b5%e8%84%91%e7%ab%af%e7%99%bb%e5%bd%95%e7%a1%ae%e8%ae%a4&pcrefer=http://i.chaoxing.com";
    static HashMap<String, String> temp = new HashMap<>();
    private static String name = "";
    private static HashMap<String, String> cookiesMap;
    private static ArrayList<PicBean> picBeans = new ArrayList<>();

    public static void saveMap(Map<String, String> cookiesMap) throws IOException {
        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("a.json"));
        outputStream.writeObject(cookiesMap);
    }

    public static void deleteMap() {
        File file = new File("a.json");
        if (file.exists()) {
            boolean delete = file.delete();
            if (delete == true) {
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
            String name = getName();
            temp.put("name", name);
            // 5. 开始签到
            int signTime = Integer.parseInt(temp.get("signTime"));
            while (true) {
                startSign(classBeans);
                Thread.sleep(signTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initSetting() throws IOException {
        Properties properties = new Properties();
//        properties.load(testLoginMain.class.getResourceAsStream("signInfo.properties"));
        File file = new File("signInfo.properties");
        if (file.exists()) {
            properties.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            System.out.println("你的设置扫描时间为" + properties.getProperty("signTime").replaceAll("\"", ""));
            System.out.println("你的定位签到地点为" + properties.getProperty("signPlace").replaceAll("\"", ""));
            System.out.println("你的开始时间是" + properties.getProperty("startTime").replaceAll("\"", ""));
            System.out.println("你的结束时间是" + properties.getProperty("endTime").replaceAll("\"", ""));
            temp.put("signTime", properties.getProperty("signTime").replaceAll("\"", ""));
            temp.put("signPlace", properties.getProperty("signPlace").replaceAll("\"", ""));
            temp.put("username", properties.getProperty("username").replaceAll("\"", ""));
            temp.put("password", properties.getProperty("password").replaceAll("\"", ""));
            temp.put("startTime", properties.getProperty("startTime").replaceAll("\"", ""));
            temp.put("endTime", properties.getProperty("endTime").replaceAll("\"", ""));
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
//            System.out.println(classElement);
//            System.out.println("======================================================");
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

    private static synchronized void startSign(ArrayList<ClassBean> classBeans) {
        ArrayList<SignBean> signBeans = new ArrayList<>();
        int startTime = Integer.parseInt(temp.get("startTime"));
        int endTime = Integer.parseInt(temp.get("endTime"));
        int thisHour = new Date().getHours();
        if (!(startTime <= thisHour && thisHour <= endTime)) {
            System.out.println("时间 -> " + thisHour + "不是扫描时间点");
            return;
        }
        System.out.println("时间 -> " + thisHour + "是扫描时间点,开始运行");
        new Thread(new Runnable() {
            int num;

            @Override
            public void run() {
                System.out.println(dateUtil.getThisTime() + "签到运行");
                signBeans.clear();
                for (int i = 0; i < classBeans.size(); i++) {
                    String url = classBeans.get(i).getUrl();
                    try {
                        Connection.Response response = Jsoup.connect(url).cookies(cookiesMap).method(Connection.Method.GET).timeout(30000).execute();
                        Document document = response.parse();
                        Elements elements = document.select("#startList div .Mct");
                        if (elements == null || elements.size() == 0) {
                            continue;
                        }
                        for (Element ele : elements) {
                            String onclick = ele.attr("onclick");
                            if (onclick != null && onclick.length() > 0) {
                                String split = onclick.split("\\(")[1];
                                String activeId = split.split(",")[0];
                                if (temp.get(activeId) != null) {
                                    SignBean signBean = new SignBean();
                                    signBean.setSignClass(classBeans.get(i).getClassName());
                                    signBean.setSignName(classBeans.get(i).getClassmate());
                                    signBean.setSignState("签到成功");
                                    signBean.setSignTime(ele.select(".Color_Orang").text());
                                    num++;
                                    signBeans.add(signBean);
                                    continue;
                                }
                                String signUrl = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?name="
                                        + URLDecoder.decode(temp.get("name"), "utf-8")
                                        + "&address="
                                        + URLEncoder.encode(temp.get("signPlace"), "utf-8")
                                        + "&activeId="
                                        + activeId
                                        + "&uid="
                                        + cookiesMap.get("_uid")
                                        + "&clientip=&latitude=-1&longitude=-1&fid="
                                        + cookiesMap.get("fid")
                                        + "&appType=15&ifTiJiao=1";
                                if (picBeans != null && picBeans.size() != 0) {
                                    Random random = new Random();
                                    int nextInt = random.nextInt(picBeans.size());
                                    signUrl = signUrl + "&objectId=" + picBeans.get(nextInt).getObjectId();
                                }
//                                System.out.println(signUrl);
//                                System.out.println("==============" + activeId + "签到中=================");
                                Connection.Response signResponse = Jsoup.connect(signUrl).cookies(cookiesMap).method(Connection.Method.GET).timeout(30000).execute();
                                Element element = signResponse.parse().body();
//                                System.out.println("签到状态" + element.getElementsByTag("body").text());
                                SignBean signBean = new SignBean();
                                signBean.setSignClass(classBeans.get(i).getClassName());
                                signBean.setSignName(classBeans.get(i).getClassmate());
                                signBean.setSignState(element.getElementsByTag("body").text());
                                signBean.setSignTime(ele.select(".Color_Orang").text());
                                if (signBean.getSignState().equals("您已签到过了")) {
                                    temp.put(activeId, "签到成功");
                                }
                                num++;
                                signBeans.add(signBean);
                                Thread.sleep(1000);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                StringBuilder temp = new StringBuilder();
                if (signBeans == null || signBeans.size() == 0) {
                    temp.append("无正在签到的活动");
                } else {
                    for (SignBean signBean : signBeans) {
                        temp.append(dateUtil.getThisTime()).append("\n")
                                .append("签到课程").append(signBean.getSignClass()).append("\n")
                                .append("签到状态").append(signBean.getSignState()).append("\n")
                                .append("剩余时间").append(signBean.getSignTime()).append("\n");
                    }
                    temp.append(dateUtil.getThisTime() + "-----------------------扫描完成").append("\n")
                            .append("总个数").append(signBeans.size()).append("\n")
                            .append("签到成功个数").append(num);
                }
                System.out.println(temp.toString());
            }
        }).start();

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
//            System.out.println("uuid = " + uuid);
//            System.out.println(" enc = " + enc);
            return this;
        }
    }
}