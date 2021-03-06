package com.nerisa.thesis.constant;

/**
 * Created by nerisa on 2/12/18.
 */

public class Constant {
    public static final String MONUMENT = "monument";
    public static final String WARNING = "warning";
    public static final String SERVER_URL = "http://206.189.172.86:8080/thesis-datarepo_war";
    public static final String MONUMENT_URL = "/monument";
    public static final String MONUMENT_LIST_URL = "/monuments";
    public static final String WARNING_URL = "/warning";
    public static final String WARNING_LIST_URL = "/warnings";
    public static final String POST_URL = "/post";
    public static final String USER_URL = "/user";
    public static final String NOISE_URL = "/noise";
    public static final String TEMPERATURE_URL = "/temperature";
    public static final String WIKI_API_URL = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gsradius=%1$s&gscoord=%2$s|%3$s&gsprop=type&format=json";
    public static final String WIKI_REST_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/%1$s";
    public static final String WIKI_PAGE_URL = "http://en.wikipedia.org/?curid=%1$s";


    public static final int NOTIFICATION_ID = 100;
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String SHARED_PREF = "monusense";
    public static final String PUSH_NOTIFICATION = "push";
    public static final String DATA = "data";

    public static final String USER_EMAIL_KEY = "email";
    public static final String USER_TOKEN_KEY = "regId";
    public static final String USER_CUSTODIAN_KEY = "custodian";
    public static final String USER_ID_KEY = "user_id";
    public static final String MONUMENT_ID_KEY = "monument_id";
    public static final String LEVEL_KEY = "level";

    public static final String MONUMENT_INFO_PRESENT = "from wiki info";
    public final static String UNVERIFIED_WARNING = "unverified";

    public static final int VOLLEY_MAX_RETRIES = 0;
    public static final int VOLLEY_TIMEOUT_MS = 50000;

    public static final float NEARBY_DISTANCE = 100.0f;

}
