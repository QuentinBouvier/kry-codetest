package se.kry.codetest.model;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Date;

public class PollService {
    private String url;
    private String name;
    private Date createdAt;
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the PollService has the required fields to be inserted in the Base
     *
     * @return True if OK
     */
    public boolean isComplete() {
        return ObjectUtils.allNotNull(url, name);
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("url", url)
                .put("name", name)
                .put("created_at", null != createdAt ? createdAt.getTime() : null)
                .put("status", status);
    }

    public boolean isUrlValid() {
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        return urlValidator.isValid(this.getUrl());
    }

    /**
     * Create a PollService from a json with "name", "url" and "created_at" keys
     *
     * @param source The json providing the values
     * @return A PollService Instance
     */
    public static PollService fromJson(JsonObject source) {
        PollService output = new PollService();
        output.setName(source.getString("name"));
        output.setUrl(source.getString("url"));
        if (null != source.getInteger("created_at")) {
            output.setCreatedAt(new Date(source.getInteger("created_at")));
        }
        output.setStatus(source.getString("status"));

        return output;
    }
}
