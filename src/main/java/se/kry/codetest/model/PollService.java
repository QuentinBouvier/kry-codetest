package se.kry.codetest.model;

import io.vertx.core.json.JsonObject;

import java.util.Date;

public class PollService {
    private String url;
    private String name;
    private Date createdAt;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public static PollService fromJson(JsonObject source) {
        PollService output = new PollService();
        output.setName(source.getString("name"));
        output.setUrl(source.getString("url"));
        output.setCreatedAt(new Date(source.getInteger("created_at")));

        return output;
    }
}
