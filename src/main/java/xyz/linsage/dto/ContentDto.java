package xyz.linsage.dto;

import com.alibaba.fastjson.JSONArray;

import java.io.Serializable;

/**
 * 内容
 *
 * @author linjingjie
 * @create 2017-06-16  下午5:28
 */
public class ContentDto implements Serializable {
    private JSONArray before;

    private JSONArray after;

    public JSONArray getBefore() {
        return before;
    }

    public void setBefore(JSONArray before) {
        this.before = before;
    }

    public JSONArray getAfter() {
        return after;
    }

    public void setAfter(JSONArray after) {
        this.after = after;
    }
}
