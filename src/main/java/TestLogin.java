import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestLogin {
    
    String loginUrl = "http://passport2.chaoxing.com/cloudscanlogin?mobiletip=%e7%94%b5%e8%84%91%e7%ab%af%e7%99%bb%e5%bd%95%e7%a1%ae%e8%ae%a4&pcrefer=http://i.chaoxing.com";

    //  http://passport2.chaoxing.com/createqr?uuid=c39795c4faaf46fcbf2c2b2a3c9d91d8&xxtrefer=&type=1&mobiletip=%E7%94%B5%E8%84%91%E7%AB%AF%E7%99%BB%E5%BD%95%E7%A1%AE%E8%AE%A4
    @Test
    public void testGetImgUrl() {
        try {
            Connection.Response response = Jsoup.connect(loginUrl).method(Connection.Method.GET).execute();
            Map<String, String> cookies = response.cookies();
            System.out.println(cookies);
            Document document = response.parse();
            //获取uuid
            //19e69fae51d04bee95cedb65a45c4b79
            Elements imgUrl = document.select("input[ id =uuid]");
            Elements encElements = document.select("input[ id =enc]");
            System.out.println(imgUrl);
            System.out.println(encElements);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getImg() throws IOException {
        String uuid = "c38a1afb46be403a8369a9c0a0e1a644";
        String imgUrl = "http://passport2.chaoxing.com/createqr?uuid=" + uuid + "&xxtrefer=&type=1&mobiletip=%E7%94%B5%E8%84%91%E7%AB%AF%E7%99%BB%E5%BD%95%E7%A1%AE%E8%AE%A4";
        Connection.Response response = Jsoup.connect(imgUrl).execute();
        FileOutputStream outputStream = new FileOutputStream("1.jpg", false);
        outputStream.write(response.bodyAsBytes());
    }

    @Test
    public void scanningImg() {
        AtomicBoolean isScanning = new AtomicBoolean(false);
        String uuid = "c38a1afb46be403a8369a9c0a0e1a644";
        String enc = "56b0f210c7a8f8b619e246c21e61b15d";
        String url = "http://passport2.chaoxing.com/getauthstatus";
        String JSESSIONID = "BD5C82018A501DB1293CADCF0594FF91";
        String route = "b9434b2aa11d2e38febba82dc6592cde";
        HashMap<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("enc", enc);

        HashMap<String, String> cookiesMap = new HashMap<>();
        cookiesMap.put("JSESSIONID", JSESSIONID);
        cookiesMap.put("route", route);

      /*  Runnable runnable = () -> {
            isScanning.set(false);
            try {
                String document = Jsoup.connect(url).data(map).post().text();
                System.out.println(document);
                Gson gson = new Gson();
                scanningBean scanningBean = gson.fromJson(document, scanningBean.class);
                int type = Integer.parseInt(scanningBean.getType());
                System.out.println("type = " + type);
                if (type == 4) {
                    System.out.println(document);
                    System.out.println("扫描成功,等待登录");
                } else if (type == 2) {
                    System.out.println("二维码失效");
                    isScanning.set(true);
                } else {
                    System.out.println("等待扫描");
                }
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        };*/

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    Connection.Response response = Jsoup.connect(url).method(Connection.Method.POST).cookies(cookiesMap).data(map).execute();
                    Map<String, String> cookies = response.cookies();
                    System.out.println(response.body());
                    System.out.println(cookies);
                } catch (IOException e) {
                    e.printStackTrace();
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
    }

    @Test
    public void getLoginCookie() {


    }

    /*
    *
    *
    *
    * vc3=eTwLMgR%2FqRptQes6Z33nOtohNGMlomulXi1QhgeYIAtFstRG4OKuICnRN2qtvsKBkwAVzuJikMB7Bvbd3dg27xSLRGpEIVA14ioFTDeeWZgiE03kJANN%2BwCh5j4DeXcdk5EVq8GgcBBOwoZFufNcrqKZG7MGpWfJBfsSQRDliP8%3D074d048c91f04e46b31bffc961c49e54;
		lv=4;
		fid=10567;
		_uid=82659775;
		UID=82659775;
		uf=b2d2c93beefa90dc0e22d16d4c94f2c4076946db6c04e7927b3dde5c0bdf867a3622052ec4d2095d222318412de87a55913b662843f1f4ad6d92e371d7fdf644d490c7da6f959ca6f3dba97bc9a5444941f23d7060bd78cc23c89035a26a838e23d4cf69bba19a94aa2ebad65cd196bb;
    *
     */
    @Test
    public void getClassRoom() throws IOException {
        String getClassUrl = "http://mooc1-2.chaoxin" +
                "g.com/visit/courses";
        HashMap<String, String> map = new HashMap<>();

//        map.put("lv", "5");
//        map.put("fid", "10567");
//        map.put("UID", "82659775");
//        map.put("uf", "b2d2c93beefa90dc0e22d16d4c94f2c4076946db6c04e7927b3dde5c0bdf867a3622052ec4d2095d222318412de87a55913b662843f1f4ad6d92e371d7fdf644d490c7da6f959ca6f3dba97bc9a5444941f23d7060bd78cc23c89035a26a838e23d4cf69bba19a94aa2ebad65cd196bb");


        map.put("vc3", "dvEL906hztK19CosWIZpSGz67kwI6gd5Mlsjp4RqfrvNAuEtVD4wDAZAE2c5X2pwJkXhE8EhIKsBII%2BG543z%2ByxF8KAjZS8NI6%2F%2F3%2FiEcNKHjeElRnjnWYacz6I0DMaWZZrqkL5xK457Y6%2FgIHdBXpmNzGOSQytlhNAJhngXyVU%3D2056c3211a7c892431b083c64b32d311");
        map.put("_d", "1583637170922");
        map.put("_uid", "82659775");


        Document document = Jsoup.connect(getClassUrl).cookies(map).get();
        System.out.println(document);
        Elements elements = document.select(".ulDiv > ul > li");
        System.out.println(elements.get(1).text());
    }
}
