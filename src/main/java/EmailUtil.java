import bean.qqBean;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * 提供邮件发送服务
 */
public class EmailUtil {
    public static void sendMail(String url, String qq, String context) {
        if (qq != null && !qq.isEmpty() && url != null && !url.isEmpty()) {
            qqBean qqBean = new qqBean();
            qqBean.setUser_id(qq);
            qqBean.setMessage(DateUtil.getTime() + "\n" + context + "\n");
            try {
                HttpUtil.trustEveryone();
                Jsoup.connect(url)
                        .requestBody(JsonUtil.objectToJson(qqBean))
                        .header("Content-Type", "application/json")
                        .ignoreContentType(true)
                        .post();
                System.out.println(context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("未指定qq或者sendMessage地址,将不会收到提醒" + "\n" + context);
        }

    }


}