package xyz.linsage;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;

import java.io.File;

/**
 * 七牛工具类
 *
 * @author linjingjie
 * @create 2017-06-16  上午10:44
 */
public class QiniuKit {
    private static QiniuKit qiniuKit;
    private final UploadManager uploadManager;
    private final String upToken;

    public static QiniuKit get() {
        if (qiniuKit == null) {
            qiniuKit = new QiniuKit();
        }
        return qiniuKit;
    }

    private QiniuKit() {
        //构造一个带指定Zone对象的配置类（华南）
        Configuration cfg = new Configuration(Zone.zone2());
        //...其他参数参考类注释
        uploadManager = new UploadManager(cfg);
        //凭证
        Auth auth = Auth.create(Constant.Qiniu.accessKey, Constant.Qiniu.secretKey);
        upToken = auth.uploadToken(Constant.Qiniu.bucket);
    }

    public void upload(File file, final OnUploadListener listener) {
        try {
            Response response = uploadManager.put(file, file.getName(), upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            listener.onUploadSuccess();

        } catch (QiniuException ex) {
            Response r = ex.response;
            System.out.println(r.toString());
            listener.onUploadFailed();
        } catch (Exception ex) {
            ex.printStackTrace();
            listener.onUploadFailed();
        }
    }


    public interface OnUploadListener {
        /**
         * 上传成功
         */
        void onUploadSuccess();


        /**
         * 上传失败
         */
        void onUploadFailed();
    }

}
