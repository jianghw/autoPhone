package com.tzly.phone.call;

public class PhoneResponse {

    /**
     * responseCode : 2000
     * responseDesc :
     * data : {"acceptPhone":"13162091231"}
     */

    private String responseCode;
    private String responseDesc;
    private DataBean data;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDesc() {
        return responseDesc;
    }

    public void setResponseDesc(String responseDesc) {
        this.responseDesc = responseDesc;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * acceptPhone : 13162091231
         */

        private String acceptPhone;

        public String getAcceptPhone() {
            return acceptPhone;
        }

        public void setAcceptPhone(String acceptPhone) {
            this.acceptPhone = acceptPhone;
        }
    }
}
