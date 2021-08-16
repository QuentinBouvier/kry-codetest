package se.kry.codetest.model;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.joda.time.DateTime;

@Data
public class ServiceStatus {
    private String url;
    private String name;
    private DateTime createdAt;
    private ServiceStatusValueEnum status;

    /**
     * Checks if the PollService has the required fields to be inserted in the Base
     *
     * @return True if OK
     */
    public boolean isValid() {
        return ObjectUtils.allNotNull(url, name);
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("url", url)
                .put("name", name)
                .put("created_at", null != createdAt ? createdAt.getMillis() : null)
                .put("status", null != status ? status.name() : null);
    }

    public boolean isUrlValid() {
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
        return urlValidator.isValid(this.getUrl());
    }

    /**
     * Create a PollService from a json with "name", "url" and "created_at" keys
     *
     * @param source The json providing the values
     * @return A PollService Instance
     */
    public static ServiceStatus fromJson(@NonNull final JsonObject source) {
        ServiceStatus output = new ServiceStatus();
        output.setName(source.getString("name"));
        output.setUrl(source.getString("url"));
        if (null != source.getInteger("created_at")) {
            output.setCreatedAt(new DateTime(source.getLong("created_at")));
        }
        output.setStatus(ServiceStatusValueEnum.valueOfOrDefault(source.getString("status")));

        return output;
    }
}
