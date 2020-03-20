import bean.PicBean;
import bean.TokenBean;
import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class picinit {

    private HashMap<String, String> stringStringHashMap;

    public picinit(HashMap<String, String> stringStringHashMap) {
        this.stringStringHashMap = stringStringHashMap;
    }

    public void initPic(ArrayList<PicBean> picBeans) throws Exception {
        // 到这里一定登录成功（bug除外）
        String getTokenUrl = "https://pan-yz.chaoxing.com/api/token/uservalid";

        String tokenResult = Jsoup.connect(getTokenUrl).method(Connection.Method.GET).cookies(stringStringHashMap).execute().body();
        System.out.println("tokenResult ----> " + "\n" + tokenResult);
        TokenBean tokenBean = new Gson().fromJson(tokenResult, TokenBean.class);
        String sendUidUrl = "https://pan-yz.chaoxing.com/api/crcstatus?puid=" + stringStringHashMap.get("UID") + "&crc=bfb2e7968005665f8ac0d0465099a9d7&_token=" + tokenBean.get_token();
        String sendUidResult = Jsoup.connect(sendUidUrl).method(Connection.Method.GET).cookies(stringStringHashMap).execute().body();
        System.out.println("sendUidResult ---->" + "\n" + sendUidResult);
        String uploadUrl = "https://pan-yz.chaoxing.com/upload?_token=" + tokenBean.get_token();

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("puid", "82674419");
        File img = new File("img");
        File[] files = img.listFiles();
        if (files != null) {
            for (File file : files) {
                postFile(uploadUrl, hashMap, file, new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String imgJson = response.body().string();
                        PicBean picBean = new Gson().fromJson(imgJson, PicBean.class);
                        picBeans.add(picBean);
                        System.out.println("初始化图片数量" + picBeans.size());
                    }
                });
            }
        } else {
            System.out.println("未添加图片,签到将没有图片");
        }
    }

    public void postFile(final String url, final Map<String, String> map, File file, Callback callback) {
        // form 表单形式上传
        //创建OkHttpClient对象(前提是导入了okhttp.jar和okio.jar)
        OkHttpClient client = new OkHttpClient().newBuilder()
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置    //忽略验证证书
                .build();

        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (file != null) {
            // MediaType.parse() 里面是上传的文件类型。
            RequestBody body = RequestBody.create(MediaType.parse("image/*"), file);
            String filename = file.getName();
            // 参数分别为， 请求key ，文件名称 ， RequestBody
            requestBody.addFormDataPart("file", filename, body);
        }
        if (map != null) {
            // map 里面是请求中所需要的 key 和 value
            Set<Map.Entry<String, String>> entries = map.entrySet();
            for (Map.Entry entry : entries) {
                String key = String.valueOf(entry.getKey());
                String value = String.valueOf(entry.getValue());
//                Log.d("HttpUtils", "key=="+key+"value=="+value);
                requestBody.addFormDataPart(key, value);
            }
        }
        Request request = new Request.Builder().url(url).header("Cookie", stringStringHashMap.toString()).post(requestBody.build()).build();
        // readTimeout("请求超时时间" , 时间单位);
        client.newBuilder().readTimeout(5000, TimeUnit.MILLISECONDS).build().newCall(request).enqueue(callback);

    }
}