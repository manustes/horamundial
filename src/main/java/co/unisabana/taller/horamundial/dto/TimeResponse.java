package co.unisabana.taller.horamundial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeResponse {
    private String datetime;
    private String timezone;
    private String city;
    private String country;
    private String formattedDatetime;
    private boolean isDaytime;

    public TimeResponse() {
    }

    public TimeResponse(String datetime, String timezone, String city, String country, String formattedDatetime, boolean isDaytime) {
        this.datetime = datetime;
        this.timezone = timezone;
        this.city = city;
        this.country = country;
        this.formattedDatetime = formattedDatetime;
        this.isDaytime = isDaytime;
    }

    public static TimeResponseBuilder builder() {
        return new TimeResponseBuilder();
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getFormattedDatetime() {
        return formattedDatetime;
    }

    public void setFormattedDatetime(String formattedDatetime) {
        this.formattedDatetime = formattedDatetime;
    }

    @JsonProperty("isDaytime")
    public boolean isDaytime() {
        return isDaytime;
    }

    public void setDaytime(boolean daytime) {
        isDaytime = daytime;
    }

    public static class TimeResponseBuilder {
        private String datetime;
        private String timezone;
        private String city;
        private String country;
        private String formattedDatetime;
        private boolean isDaytime;

        public TimeResponseBuilder datetime(String datetime) {
            this.datetime = datetime;
            return this;
        }


        public TimeResponseBuilder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }


        public TimeResponseBuilder city(String city) {
            this.city = city;
            return this;
        }


        public TimeResponseBuilder country(String country) {
            this.country = country;
            return this;
        }


        public TimeResponseBuilder formattedDatetime(String formattedDatetime) {
            this.formattedDatetime = formattedDatetime;
            return this;
        }


        public TimeResponseBuilder isDaytime(boolean isDaytime) {
            this.isDaytime = isDaytime;
            return this;
        }


        public TimeResponse build() {
            return new TimeResponse(datetime, timezone, city, country, formattedDatetime, isDaytime);
        }
    }
}
