package xyz.linsage.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.core.Controller;
import com.jfinal.json.Json;
import com.jfinal.kit.FileKit;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.ehcache.CacheKit;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.linsage.Constant;
import xyz.linsage.DateKit;
import xyz.linsage.DownloadKit;
import xyz.linsage.QiniuKit;
import xyz.linsage.dto.ContentDto;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author linjingjie
 * @create 2017-06-13  上午9:48
 */
public class IndexController extends Controller {

    /**
     * 刷新ins缓存信息
     */
    public void refreshIns() {
        String username = getPara("username");

        CacheKit.remove("userPhoto", username);

        renderText("ok");
    }


    /**
     * 下载ins，返回相册json
     *
     * @throws Exception
     */
    public void downloadIns() throws Exception {
        String username = getPara("username");

        JSONArray array = getEdges(username);

        //结果map（k-年月，v-内容）
        Map<String, JSONArray> nodeMap = new LinkedHashMap<>();

        if (array != null) { //不为空，下载图片
            for (int i = 0; i < array.size(); i++) {
                JSONObject node = array.getJSONObject(i).getJSONObject("node");
                //时间
                int timestamp = node.getIntValue("taken_at_timestamp");
                Date date = DateKit.getDateByUnixTime(timestamp);
                //code
                final String code = node.getString("shortcode");
                //标题
                final String title = node.getJSONObject("edge_media_to_caption").getJSONArray("edges").getJSONObject(0).getJSONObject("node").getString("text");
                //图片地址
                final String url = node.getString("display_url");
                final String extension = url.substring(url.lastIndexOf("."));
                final String fileName = code + extension;
                final String saveDir = PathKit.getWebRootPath() + File.separator + "ins";
                //年-月-1
                final String key = DateKit.dateFormat(date, "yyyy-MM-1");

                JSONObject obj = new JSONObject();
                obj.put("timestamp", timestamp);
                obj.put("url", Constant.Qiniu.domain + "/" + fileName);
                obj.put("title", title);
                obj.put("type", "image");
                if (!nodeMap.containsKey(key)) {
                    nodeMap.put(key, new JSONArray());
                }
                nodeMap.get(key).add(obj);

                DownloadKit.get().download(url, fileName, saveDir, new DownloadKit.OnDownloadListener() {
                    @Override
                    public void onDownloadSuccess() {
                        System.out.println("onDownloadSuccess：" + code);

                        //成功下载，上传到七牛云
                        QiniuKit.get().upload(new File(saveDir + File.separator + fileName), new QiniuKit.OnUploadListener() {
                                    @Override
                                    public void onUploadSuccess() {
                                        System.out.println("onUploadSuccess" + code);
                                        System.out.println(Constant.Qiniu.domain + "/" + fileName);

                                    }

                                    @Override
                                    public void onUploadFailed() {
                                        System.out.println("onUploadFailed" + code);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onDownloading(int progress) {
                    }

                    @Override
                    public void onDownloadFailed() {
                        System.out.println("onDownloadFailed：" + code);
                    }
                });


            }
        }

        //ins.json
        JSONObject result = new JSONObject();

        JSONArray list = new JSONArray();
        for (Map.Entry<String, JSONArray> entry : nodeMap.entrySet()) {
            String key = entry.getKey();
            JSONArray value = entry.getValue();
            // now work with key and value...
            Date date = DateKit.convertToDate(key);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;

            JSONObject item = new JSONObject();
            item.put("year", year);
            item.put("month", month);
            item.put("arr", value);
            list.add(item);
        }

        result.put("list", list);

        renderJson(result);
    }


    /**
     * 获取图文信息json
     *
     * @param username
     */
    private JSONArray getEdges(String username) {
        //先从缓存读取
        JSONArray array = CacheKit.get("userPhoto", username);

        if (array != null)
            return array;

        //1.获取userId
        String url = "https://www.instagram.com/" + username + "/?__a=1";
        //由于墙外地址，使用代理（请自行修改）
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().proxy((new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)))).build();
        Request request = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            JSONObject jsonObject = JSONObject.parseObject(response.body().string());
            String userId = ((JSONObject) jsonObject.get("user")).get("id").toString();
            //System.out.println(jsonObject.toJSONString());

            //2.获取内容
            String contentUrl = "https://www.instagram.com/graphql/query/?query_id=17880160963012870&id=" + userId + "&first=999";
            request = new Request.Builder().url(contentUrl).build();
            response = okHttpClient.newCall(request).execute();
            jsonObject = JSONObject.parseObject(response.body().string());
            //System.out.println(jsonObject.toJSONString());

            //data.user.edge_owner_to_timeline_media.edges
            array = jsonObject
                    .getJSONObject("data")
                    .getJSONObject("user")
                    .getJSONObject("edge_owner_to_timeline_media")
                    .getJSONArray("edges");

            //设置缓存（k-username,V-图文）
            CacheKit.put("userPhoto", username, array);

            return array;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}

